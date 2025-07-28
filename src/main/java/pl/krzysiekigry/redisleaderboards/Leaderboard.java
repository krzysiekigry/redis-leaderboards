package pl.krzysiekigry.redisleaderboards;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A Redis-based leaderboard implementation supporting various scoring and ranking operations.
 * <p>
 * This class provides a comprehensive leaderboard solution using Redis sorted sets as the underlying
 * data structure. It supports different sorting policies, update strategies, and provides both
 * synchronous and asynchronous operations for managing leaderboard entries.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Flexible scoring with generic numeric types</li>
 *   <li>Configurable sorting policies (high-to-low, low-to-high)</li>
 *   <li>Multiple update strategies (replace, aggregate, best)</li>
 *   <li>Batch operations for efficient bulk updates</li>
 *   <li>Ranking and position queries</li>
 *   <li>Range-based entry retrieval</li>
 *   <li>Export streaming for large datasets</li>
 * </ul>
 * 
 * @param <T> the numeric type used for scores (e.g., Integer, Double, Long)
 * 
 * @see LeaderboardOptions for configuration options
 * @see Entry for leaderboard entry structure
 * @see UpdatePolicy for update strategies
 * @see SortPolicy for sorting options
 */
public class Leaderboard<T extends Number> {

    private static final Logger log = LoggerFactory.getLogger(Leaderboard.class);

    private final RedisExtension redisExtension;
    private final String key;
    private final Class<T> clazz;
    private final LeaderboardOptions options;

    /**
     * Creates a new leaderboard instance with the specified configuration.
     * 
     * @param redisExtension the Redis connection and script management extension
     * @param key the Redis key to use for storing leaderboard data
     * @param clazz the class type for score values
     * @param options the configuration options for this leaderboard
     */
    public Leaderboard(RedisExtension redisExtension, String key, Class<T> clazz, LeaderboardOptions options) {
        this.redisExtension = redisExtension;
        this.key = key;
        this.clazz = clazz;
        this.options = options;
    }

    public CompletableFuture<Long> rank(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                Long rank = options.sortPolicy() == SortPolicy.HIGH_TO_LOW ?
                        jedis.zrevrank(key, id) :
                        jedis.zrank(key, id);
                return rank != null ? rank + 1 : null;
            }
        });
    }

    public Entry<T> find(String id) {
        try (Jedis jedis = redisExtension.getJedis()) {
            Double score = jedis.zscore(key, id);
            if (score == null) {
                return null;
            }

            Long rank = options.sortPolicy() == SortPolicy.HIGH_TO_LOW ?
                    jedis.zrevrank(key, id) :
                    jedis.zrank(key, id);
            if (rank == null) {
                return null;
            }

            return new Entry<>(id, getT(new Tuple(id, score)), rank + 1);
        }
    }

    public CompletableFuture<Entry<T>> at(long rank) {
        if (rank <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return list(rank, rank).thenApply(list -> list.isEmpty() ? null : list.get(0));
    }

    public CompletableFuture<T> updateOne(String id, T value, UpdatePolicy updatePolicy) {
        return update(Collections.singletonList(new EntryUpdateQuery<T>(id, value)), updatePolicy)
                .thenApply(results -> results.get(0));
    }

    public CompletableFuture<T> updateOne(String id, T value) {
        return updateOne(id, value, null);
    }

    public CompletableFuture<List<T>> update(List<EntryUpdateQuery<T>> entries) {
        return update(entries, null);
    }

    public CompletableFuture<List<T>> update(List<EntryUpdateQuery<T>> entries, UpdatePolicy updatePolicy) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(() -> updateInternal(entries, updatePolicy), 3));
    }

    private List<T> updateInternal(List<EntryUpdateQuery<T>> entries, UpdatePolicy updatePolicy) {
        try (Jedis jedis = redisExtension.getJedis()) {

            long currentCount = 0;
            if (options.limitTopN() > 0) {
                currentCount = jedis.zcard(key);
            }

            Pipeline pipeline = jedis.pipelined();

            updatePipe(entries, pipeline, updatePolicy);

            if (options.limitTopN() > 0 && currentCount + entries.size() > options.limitTopN()) {
                if (options.sortPolicy() == SortPolicy.HIGH_TO_LOW) {
                    pipeline.zremrangeByRank(key, 0, currentCount + entries.size() - options.limitTopN() - 1);
                } else {
                    pipeline.zremrangeByRank(key, options.limitTopN(), -1);
                }
            }

            List<Object> results = pipeline.syncAndReturnAll();

            return results.stream().limit(entries.size()).map(result -> {
                if (result instanceof Long) {
                    Integer intValue = ((Long) result).intValue();
                    return clazz == Integer.class ? (T) intValue : (T) Long.valueOf(intValue);
                } else {
                    return null;
                }
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to update leaderboard entries", e);
            throw new RuntimeException("Failed to update leaderboard entries", e);
        }
    }

    private <R> R executeWithRetry(java.util.function.Supplier<R> operation, int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                if (e.getCause() instanceof JedisConnectionException && attempt < maxRetries - 1) {
                    attempt++;
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000; // Exponential backoff: 1s, 2s, 4s
                    log.warn("Redis connection timeout on attempt {}/{}, retrying in {}ms", 
                            attempt, maxRetries, backoffMs, e.getCause());
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Should not reach here");
    }

    @FunctionalInterface
    private interface PipelineCommand<T extends Number> {
        void apply(EntryUpdateQuery<T> entry);
    }

    public void updatePipe(List<EntryUpdateQuery<T>> entries, Pipeline pipeline, UpdatePolicy customUpdatePolicy) {
        UpdatePolicy effectiveUpdatePolicy = (customUpdatePolicy != null) ? customUpdatePolicy : options.updatePolicy();

        PipelineCommand<T> fn = switch (effectiveUpdatePolicy) {
            case REPLACE -> (entry) -> pipeline.zadd(key, entry.value().doubleValue(), entry.id());
            case AGGREGATE -> (entry) -> pipeline.zincrby(key, entry.value().doubleValue(), entry.id());
            case BEST -> {
                String scriptSha = redisExtension.getScriptSha("zbest");
                yield (entry) -> pipeline.evalsha(scriptSha, 1, key, String.valueOf(entry.value()), entry.id(), options.sortPolicy() == SortPolicy.HIGH_TO_LOW ? "desc" : "asc");
            }
        };

        for (EntryUpdateQuery<T> entry : entries) {
            fn.apply(entry);
        }
    }

    public CompletableFuture<Void> remove(String... ids) {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                jedis.zrem(key, ids);
            }
        });
    }

    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                jedis.del(key);
            }
        });
    }

    public CompletableFuture<List<Entry<T>>> list(long lower, long upper) {
        final long finalLower = Math.max(lower, 1);
        final long finalUpper = Math.max(upper, 1);

        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                List<Tuple> results = options.sortPolicy() == SortPolicy.LOW_TO_HIGH ?
                        jedis.zrangeWithScores(key, finalLower - 1, finalUpper - 1) :
                        jedis.zrevrangeWithScores(key, finalLower - 1, finalUpper - 1);

                List<Entry<T>> entries = new ArrayList<>();
                long rank = finalLower;
                for (Tuple tuple : results) {
                    entries.add(new Entry<T>(tuple.getElement(), getT(tuple), rank++));
                }
                return entries;
            }
        });
    }

    public CompletableFuture<List<Entry<T>>> listByScore(double min, double max) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                String scriptSha = redisExtension.getScriptSha("zrangescore");
                Object result = jedis.evalsha(scriptSha, 1, key, String.valueOf(min), String.valueOf(max), options.sortPolicy().name());

                if (result instanceof List) {
                    List<Object> resultList = (List<Object>) result;
                    long baseRank = (long) resultList.get(0);
                    if (baseRank == -1) {
                        return Collections.emptyList();
                    }
                    List<Object> rawEntries = (List<Object>) resultList.get(1);

                    List<Entry<T>> entries = new ArrayList<>();
                    for (int i = 0; i < rawEntries.size(); i += 2) {
                        String memberId;
                        if (rawEntries.get(i) instanceof byte[]) {
                            memberId = new String((byte[]) rawEntries.get(i));
                        } else {
                            memberId = (String) rawEntries.get(i);
                        }

                        double score;
                        if (rawEntries.get(i + 1) instanceof byte[]) {
                            score = Double.parseDouble(new String((byte[]) rawEntries.get(i + 1)));
                        } else {
                            score = Double.parseDouble((String) rawEntries.get(i + 1));
                        }

                        entries.add(new Entry<>(memberId, getT(new Tuple(memberId, score)), baseRank + i / 2 + 1));
                    }
                    return entries;
                }
                return Collections.emptyList();
            }
        });
    }

    public CompletableFuture<List<Entry<T>>> top(int max) {
        return list(1, max);
    }

    public CompletableFuture<List<Entry<T>>> bottom(int max) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                long count = jedis.zcard(key);
                List<Tuple> results = options.sortPolicy() == SortPolicy.LOW_TO_HIGH ?
                        jedis.zrangeWithScores(key, -Math.max(1, max), -1) :
                        jedis.zrevrangeWithScores(key, -Math.max(1, max), -1);

                List<Entry<T>> entries = new ArrayList<>();
                long rank = count - results.size() + 1;
                for (Tuple tuple : results) {
                    entries.add(new Entry<T>(tuple.getElement(), getT(tuple), rank++));
                }
                Collections.reverse(entries);
                return entries;
            }
        });
    }

    public CompletableFuture<List<Entry<T>>> around(String id, int distance, boolean fillBorders) {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                String scriptSha = redisExtension.getScriptSha("zaround");
                Object result = jedis.evalsha(scriptSha, 1, key, id, String.valueOf(distance), String.valueOf(fillBorders), options.sortPolicy().name());

                if (result instanceof List) {
                    List<Object> resultList = (List<Object>) result;
                    long baseRank = (long) resultList.get(0);
                    if (baseRank == -1) {
                        return Collections.emptyList();
                    }
                    List<Object> rawEntries = (List<Object>) resultList.get(1);

                    List<Entry<T>> entries = new ArrayList<>();
                    for (int i = 0; i < rawEntries.size(); i += 2) {
                        String memberId;
                        if (rawEntries.get(i) instanceof byte[]) {
                            memberId = new String((byte[]) rawEntries.get(i));
                        } else {
                            memberId = (String) rawEntries.get(i);
                        }

                        double score;
                        if (rawEntries.get(i + 1) instanceof byte[]) {
                            score = Double.parseDouble(new String((byte[]) rawEntries.get(i + 1)));
                        } else {
                            score = Double.parseDouble((String) rawEntries.get(i + 1));
                        }

                        entries.add(new Entry<>(memberId, getT(new Tuple(memberId, score)), baseRank + i / 2 + 1));
                    }
                    return entries;
                }
                return Collections.emptyList();
            }
        });
    }

    public ExportStream<T> exportStream(int batchSize) {
        return new ExportStream<T>(this, batchSize);
    }

    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(() -> {
            try (Jedis jedis = redisExtension.getJedis()) {
                return jedis.zcard(key);
            }
        });
    }

    /**
     * Converts Redis tuple score to the appropriate numeric type.
     *
     * @param tuple Redis tuple containing score as double
     * @return converted score as type T
     * @throws IllegalArgumentException if a class type is not supported
     * @throws ClassCastException if value exceeds target type range
     *
     * @apiNote Integer range: ±2,147,483,647
     * @apiNote Long precision limited by double: ±9,007,199,254,740,992 (2^53)
     * @apiNote Values exceeding Integer.MAX_VALUE will cause overflow
     */
    private T getT(Tuple tuple) {
        Number score = switch (clazz.getSimpleName()) {
            case "Integer" -> (int) Math.round(tuple.getScore());
            case "Long" -> Math.round(tuple.getScore());
            case "Double" -> tuple.getScore();
            default -> throw new IllegalArgumentException("Unsupported class: " + clazz);
        };
        return clazz.cast(score);
    }
}

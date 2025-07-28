package pl.krzysiekigry.redisleaderboards;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.LocalDateTime;
import java.util.*;

/**
 * A time-based leaderboard system that automatically creates separate leaderboards for different time cycles.
 * <p>
 * This class manages multiple {@link Leaderboard} instances, each corresponding to a specific time period
 * (e.g., daily, weekly, monthly). It automatically generates appropriate Redis keys based on the current
 * time and the configured cycle, allowing for easy management of time-segmented leaderboards.
 * </p>
 * <p>
 * Key features include:
 * <ul>
 *   <li>Automatic time-based key generation using predefined or custom cycles</li>
 *   <li>Efficient leaderboard instance caching with LRU eviction</li>
 *   <li>Support for both {@link DefaultCycles} and custom {@link CycleFunction} implementations</li>
 *   <li>Discovery of existing time periods through key scanning</li>
 *   <li>Configurable time providers for testing and custom scenarios</li>
 * </ul>
 * 
 * @param <T> the numeric type used for scores (e.g., Integer, Double, Long)
 * 
 * @see PeriodicLeaderboardOptions for configuration options
 * @see DefaultCycles for predefined time cycles
 * @see CycleFunction for custom cycle implementations
 * @see Leaderboard for individual leaderboard operations
 */
public class PeriodicLeaderboard<T extends Number> {

    private static final Map<DefaultCycles, CycleFunction> CYCLE_FUNCTIONS = new EnumMap<>(DefaultCycles.class);

    static {
        CYCLE_FUNCTIONS.put(DefaultCycles.YEARLY, time -> String.format("y%d", time.getYear()));
        CYCLE_FUNCTIONS.put(DefaultCycles.WEEKLY, time -> String.format("w%04d", time.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())));
        CYCLE_FUNCTIONS.put(DefaultCycles.MONTHLY, time -> String.format("%s-m%02d",
                CYCLE_FUNCTIONS.get(DefaultCycles.YEARLY).apply(time), time.getMonthValue()));
        CYCLE_FUNCTIONS.put(DefaultCycles.DAILY, time -> String.format("%s-d%02d",
                CYCLE_FUNCTIONS.get(DefaultCycles.MONTHLY).apply(time), time.getDayOfMonth()));
        CYCLE_FUNCTIONS.put(DefaultCycles.HOURLY, time -> String.format("%s-h%02d",
                CYCLE_FUNCTIONS.get(DefaultCycles.DAILY).apply(time), time.getHour()));
        CYCLE_FUNCTIONS.put(DefaultCycles.MINUTE, time -> String.format("%s-m%02d",
                CYCLE_FUNCTIONS.get(DefaultCycles.HOURLY).apply(time), time.getMinute()));
    }

    private final RedisExtension redisExtension;
    private final String baseKey;
    private final Class<T> clazz;
    private final PeriodicLeaderboardOptions options;

    private final Map<String, Leaderboard<T>> leaderboards = new LinkedHashMap<String, Leaderboard<T>>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Leaderboard<T>> eldest) {
            return size() > 100;
        }
    };

    /**
     * Creates a new periodic leaderboard with the specified configuration.
     * 
     * @param redisExtension the Redis connection and script management extension
     * @param baseKey the base Redis key prefix for all time-based leaderboards
     * @param clazz the class type for score values
     * @param options the configuration options for this periodic leaderboard
     */
    public PeriodicLeaderboard(RedisExtension redisExtension, String baseKey, Class<T> clazz, PeriodicLeaderboardOptions options) {
        this.redisExtension = redisExtension;
        this.baseKey = baseKey;
        this.clazz = clazz;
        this.options = options;
    }

    public String getKey(LocalDateTime time) {
        if (options.cycle() instanceof DefaultCycles) {
            return CYCLE_FUNCTIONS.get((DefaultCycles) options.cycle()).apply(time);
        } else if (options.cycle() instanceof CycleFunction) {
            return ((CycleFunction) options.cycle()).apply(time);
        }
        throw new IllegalStateException("Invalid cycle type");
    }

    public Leaderboard<T> getLeaderboard(String key) {
        String finalKey = baseKey + ":" + key;

        Leaderboard<T> lb = leaderboards.get(finalKey);
        if (lb != null) {
            return lb;
        }

        if (leaderboards.size() > 100) {
            leaderboards.clear();
        }

        lb = new Leaderboard<>(redisExtension, finalKey, this.clazz, options.leaderboardOptions());
        leaderboards.put(finalKey, lb);
        return lb;
    }

    public Leaderboard<T> getLeaderboardAt(LocalDateTime time) {
        return getLeaderboard(time != null ? getKey(time) : getKeyNow());
    }

    public String getKeyNow() {
        return getKey(options.now().get());
    }

    public Leaderboard<T> getLeaderboardNow() {
        return getLeaderboard(getKeyNow());
    }

    public Set<String> getExistingKeys() {
        Set<String> keys = new HashSet<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams()
                .match(baseKey + ":*")
                .count(100);

        do {
            try (Jedis jedis = redisExtension.getJedis()) {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                cursor = scanResult.getCursor();

                for (String key : scanResult.getResult()) {
                    keys.add(key.substring(baseKey.length() + 1));
                }
            }
        } while (!cursor.equals("0"));

        return keys;
    }
}

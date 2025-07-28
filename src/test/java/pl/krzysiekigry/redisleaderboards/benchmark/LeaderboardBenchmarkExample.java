package pl.krzysiekigry.redisleaderboards.benchmark;

import pl.krzysiekigry.redisleaderboards.*;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Benchmark example demonstrating performance characteristics of Redis Leaderboards.
 * 
 * This example measures the performance of various leaderboard operations under different
 * scenarios and provides insights into throughput and latency characteristics.
 * 
 * Usage: Run this as a regular Java application (main method).
 * Requirements: Redis server running on localhost:6379
 */
public class LeaderboardBenchmarkExample {

    private static final String BENCHMARK_KEY = "benchmark-leaderboard";
    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    
    private RedisExtension redisExtension;
    private JedisPool jedisPool;
    private Random random = new Random(42); // Fixed seed for reproducible results

    public static void main(String[] args) {
        LeaderboardBenchmarkExample benchmark = new LeaderboardBenchmarkExample();
        try {
            benchmark.setUp();
            benchmark.runAllBenchmarks();
        } catch (Exception e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            benchmark.tearDown();
        }
    }

    private void setUp() {
        System.out.println("=== Redis Leaderboards Benchmark ===");
        System.out.println("Setting up Redis connection...");

        JedisPoolConfig poolConfig = buildPoolConfig();
        jedisPool = new JedisPool(poolConfig, "localhost", 6379, 10000, null);
        redisExtension = new RedisExtension(jedisPool);
        redisExtension.prepare();

        System.out.println("Setup complete.\n");
    }

    private void tearDown() {
        if (jedisPool != null) {
            System.out.println("\nCleaning up...");
            jedisPool.close();
        }
    }

    private void runAllBenchmarks() throws ExecutionException, InterruptedException {
        // Test different scenarios
        benchmarkSingleUpdates();
        benchmarkBatchUpdates();
        benchmarkQueries();
        benchmarkUpdatePolicies();
        benchmarkScaleTest();
    }

    private void benchmarkSingleUpdates() throws ExecutionException, InterruptedException {
        System.out.println("=== Single Update Operations Benchmark ===");
        
        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            leaderboard.updateOne("warmup-" + i, random.nextInt(1000)).get();
        }
        leaderboard.clear().get();
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            leaderboard.updateOne("player-" + i, random.nextInt(1000)).get();
        }
        long endTime = System.nanoTime();
        
        double avgLatency = (endTime - startTime) / 1_000_000.0 / BENCHMARK_ITERATIONS;
        double throughput = BENCHMARK_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);
        
        System.out.printf("Single updates: %.2f ms avg latency, %.0f ops/sec\n", avgLatency, throughput);
        leaderboard.clear().get();
    }

    private void benchmarkBatchUpdates() throws ExecutionException, InterruptedException {
        System.out.println("\n=== Batch Update Operations Benchmark ===");
        
        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);
        int[] batchSizes = {10, 50, 100, 500};
        
        for (int batchSize : batchSizes) {
            // Warmup
            for (int i = 0; i < 10; i++) {
                List<EntryUpdateQuery<Integer>> warmupBatch = generateBatch(batchSize, "warmup-" + i + "-");
                leaderboard.update(warmupBatch).get();
            }
            leaderboard.clear().get();
            
            // Benchmark
            int iterations = Math.max(10, BENCHMARK_ITERATIONS / batchSize);
            long startTime = System.nanoTime();
            
            for (int i = 0; i < iterations; i++) {
                List<EntryUpdateQuery<Integer>> batch = generateBatch(batchSize, "batch-" + i + "-");
                leaderboard.update(batch).get();
            }
            
            long endTime = System.nanoTime();
            double avgLatency = (endTime - startTime) / 1_000_000.0 / iterations;
            double throughput = (iterations * batchSize) / ((endTime - startTime) / 1_000_000_000.0);
            
            System.out.printf("Batch size %d: %.2f ms avg latency, %.0f ops/sec\n", 
                            batchSize, avgLatency, throughput);
            leaderboard.clear().get();
        }
    }

    private void benchmarkQueries() throws ExecutionException, InterruptedException {
        System.out.println("\n=== Query Operations Benchmark ===");
        
        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);
        
        // Populate with test data
        int dataSize = 10000;
        List<EntryUpdateQuery<Integer>> testData = generateBatch(dataSize, "player-");
        leaderboard.update(testData).get();
        
        // Benchmark different query operations
        benchmarkOperation("rank()", () -> {
            String playerId = "player-" + random.nextInt(dataSize);
            return leaderboard.rank(playerId).get();
        });
        
        benchmarkOperation("find()", () -> {
            String playerId = "player-" + random.nextInt(dataSize);
            return leaderboard.find(playerId);
        });
        
        benchmarkOperation("at()", () -> {
            long rank = random.nextInt(dataSize) + 1;
            return leaderboard.at(rank).get();
        });
        
        benchmarkOperation("top(10)", () -> {
            return leaderboard.top(10).get();
        });
        
        benchmarkOperation("around()", () -> {
            String playerId = "player-" + random.nextInt(dataSize);
            return leaderboard.around(playerId, 5, true).get();
        });
        
        leaderboard.clear().get();
    }

    private void benchmarkUpdatePolicies() throws ExecutionException, InterruptedException {
        System.out.println("\n=== Update Policies Benchmark ===");
        
        UpdatePolicy[] policies = {UpdatePolicy.REPLACE, UpdatePolicy.BEST, UpdatePolicy.AGGREGATE};
        
        for (UpdatePolicy policy : policies) {
            Leaderboard<Integer> leaderboard = createLeaderboard(policy);
            
            // Pre-populate with some data
            List<EntryUpdateQuery<Integer>> initialData = generateBatch(1000, "player-");
            leaderboard.update(initialData).get();
            
            // Benchmark updates on existing data
            long startTime = System.nanoTime();
            for (int i = 0; i < 500; i++) {
                String playerId = "player-" + random.nextInt(1000);
                leaderboard.updateOne(playerId, random.nextInt(1000)).get();
            }
            long endTime = System.nanoTime();
            
            double avgLatency = (endTime - startTime) / 1_000_000.0 / 500;
            System.out.printf("Policy %s: %.2f ms avg latency\n", policy, avgLatency);
            
            leaderboard.clear().get();
        }
    }

    private void benchmarkScaleTest() throws ExecutionException, InterruptedException {
        System.out.println("\n=== Scale Test Benchmark ===");
        
        int[] dataSizes = {1000, 10000, 100000};
        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);
        
        for (int dataSize : dataSizes) {
            System.out.printf("Testing with %d entries...\n", dataSize);
            
            // Populate data in batches
            long populateStart = System.nanoTime();
            int batchSize = 1000;
            for (int i = 0; i < dataSize; i += batchSize) {
                int currentBatchSize = Math.min(batchSize, dataSize - i);
                List<EntryUpdateQuery<Integer>> batch = generateBatch(currentBatchSize, "scale-" + i + "-");
                leaderboard.update(batch).get();
            }
            long populateEnd = System.nanoTime();
            
            // Test query performance
            long queryStart = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                leaderboard.top(10).get();
                leaderboard.rank("scale-" + random.nextInt(dataSize) + "-0").get();
            }
            long queryEnd = System.nanoTime();
            
            double populateTime = (populateEnd - populateStart) / 1_000_000.0;
            double queryTime = (queryEnd - queryStart) / 1_000_000.0 / 100;
            
            System.out.printf("  Populate: %.0f ms total, Query: %.2f ms avg\n", 
                            populateTime, queryTime);
            
            leaderboard.clear().get();
        }
    }

    private void benchmarkOperation(String operationName, BenchmarkOperation operation) 
            throws ExecutionException, InterruptedException {
        
        // Warmup
        for (int i = 0; i < 50; i++) {
            operation.execute();
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            operation.execute();
        }
        long endTime = System.nanoTime();
        
        double avgLatency = (endTime - startTime) / 1_000_000.0 / 200;
        System.out.printf("  %s: %.2f ms avg latency\n", operationName, avgLatency);
    }

    private Leaderboard<Integer> createLeaderboard(UpdatePolicy updatePolicy) {
        return new Leaderboard<>(
            redisExtension,
            BENCHMARK_KEY,
            Integer.class,
            new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, updatePolicy, 1000000)
        );
    }

    private List<EntryUpdateQuery<Integer>> generateBatch(int size, String prefix) {
        List<EntryUpdateQuery<Integer>> batch = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            batch.add(new EntryUpdateQuery<>(prefix + i, random.nextInt(10000)));
        }
        return batch;
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    @FunctionalInterface
    private interface BenchmarkOperation {
        Object execute() throws ExecutionException, InterruptedException;
    }
}
package pl.krzysiekigry.redisleaderboards.benchmark;

import pl.krzysiekigry.redisleaderboards.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extended benchmark for Redis Leaderboards with massive scale and realistic workload testing.
 * <p>
 * This benchmark includes:
 * - Massive scale tests (up to 10M entries)
 * - Mixed realistic workloads (70% reads, 20% updates, 10% batch operations)
 * - Concurrent load testing
 * - Memory usage monitoring
 * - Latency percentile tracking
 * <p>
 * Usage: Run this as a regular Java application (main method).
 * Requirements: Redis server with sufficient memory (recommend 4GB+)
 */
public class ExtendedLeaderboardBenchmark {

    private static final String BENCHMARK_KEY = "extended-benchmark-leaderboard";

    // Massive scale test parameters
    private static final int[] MASSIVE_SCALE_SIZES = {1_000_000, 5_000_000, 10_000_000};

    // Mixed workload parameters
    private static final int MIXED_WORKLOAD_DURATION_SECONDS = 300; // 5 minutes
    private static final int CONCURRENT_THREADS = 20;
    private static final double READ_RATIO = 0.70;   // 70% reads
    private static final double UPDATE_RATIO = 0.20; // 20% updates
    private static final double BATCH_RATIO = 0.10;  // 10% batch operations

    private RedisExtension redisExtension;
    private JedisPool jedisPool;
    private Random random = new Random(42);
    private volatile boolean stopBenchmark = false;

    public static void main(String[] args) {
        ExtendedLeaderboardBenchmark benchmark = new ExtendedLeaderboardBenchmark();
        try {
            benchmark.setUp();
            benchmark.runExtendedBenchmarks();
        } catch (Exception e) {
            System.err.println("Extended benchmark failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            benchmark.tearDown();
        }
    }

    private void setUp() {
        System.out.println("=== Extended Redis Leaderboards Benchmark ===");
        System.out.println("Setting up Redis connection...");

        JedisPoolConfig poolConfig = buildOptimizedPoolConfig();
        jedisPool = new JedisPool(poolConfig, "localhost", 6379, 30000, null);
        redisExtension = new RedisExtension(jedisPool);
        redisExtension.prepare();

        printRedisInfo();
        System.out.println("Setup complete.\n");
    }

    private void tearDown() {
        if (jedisPool != null) {
            System.out.println("\nCleaning up...");
            // Clean up test data
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(BENCHMARK_KEY);
            }
            jedisPool.close();
        }
    }

    private void runExtendedBenchmarks() throws Exception {
        System.out.println("Starting extended benchmarks...\n");

        // 1. Massive Scale Test
        runMassiveScaleTest();

        // 2. Mixed Realistic Workload Test
        runMixedWorkloadTest();

        System.out.println("\n=== Extended Benchmark Complete ===");
    }

    private void runMassiveScaleTest() throws Exception {
        System.out.println("=== MASSIVE SCALE TEST ===");
        System.out.println("Testing leaderboard performance with up to 10M entries");
        System.out.println("This may take several minutes...\n");

        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);

        for (int dataSize : MASSIVE_SCALE_SIZES) {
            System.out.printf("--- Testing with %,d entries ---\n", dataSize);

            // Memory before population
            RedisMemoryInfo memoryBefore = getRedisMemoryInfo();
            System.out.printf("Memory before: %s\n", memoryBefore);

            // Populate data in optimized batches
            long populateStart = System.currentTimeMillis();
            populateMassiveData(leaderboard, dataSize);
            long populateEnd = System.currentTimeMillis();

            // Memory after population
            RedisMemoryInfo memoryAfter = getRedisMemoryInfo();
            System.out.printf("Memory after: %s\n", memoryAfter);
            System.out.printf("Memory used for %,d entries: %s\n",
                    dataSize, formatBytes(memoryAfter.usedMemory - memoryBefore.usedMemory));

            double populateTime = (populateEnd - populateStart) / 1000.0;
            System.out.printf("Population time: %.1f seconds (%.0f entries/sec)\n",
                    populateTime, dataSize / populateTime);

            // Test various query operations with latency percentiles
            testMassiveScaleQueries(leaderboard, dataSize);

            // Clear for next test
            System.out.println("Clearing data...");
            long clearStart = System.currentTimeMillis();
            leaderboard.clear().get();
            long clearEnd = System.currentTimeMillis();
            System.out.printf("Clear time: %.1f seconds\n", (clearEnd - clearStart) / 1000.0);

            System.out.println();
        }
    }

    private void runMixedWorkloadTest() throws Exception {
        System.out.println("=== MIXED REALISTIC WORKLOAD TEST ===");
        System.out.printf("Running mixed workload for %d seconds with %d concurrent threads\n",
                MIXED_WORKLOAD_DURATION_SECONDS, CONCURRENT_THREADS);
        System.out.printf("Workload distribution: %.0f%% reads, %.0f%% updates, %.0f%% batch ops\n",
                READ_RATIO * 100, UPDATE_RATIO * 100, BATCH_RATIO * 100);
        System.out.println();

        // Pre-populate with realistic data (1M entries)
        Leaderboard<Integer> leaderboard = createLeaderboard(UpdatePolicy.REPLACE);
        int initialDataSize = 1_000_000;
        System.out.printf("Pre-populating with %,d entries...\n", initialDataSize);

        long prepopulateStart = System.currentTimeMillis();
        populateMassiveData(leaderboard, initialDataSize);
        long prepopulateEnd = System.currentTimeMillis();
        System.out.printf("Pre-population completed in %.1f seconds\n",
                (prepopulateEnd - prepopulateStart) / 1000.0);

        // Statistics tracking
        MixedWorkloadStats stats = new MixedWorkloadStats();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);

        // Start worker threads
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    runMixedWorkloadWorker(leaderboard, initialDataSize, threadId, stats);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start the benchmark
        System.out.println("Starting mixed workload benchmark...");
        long benchmarkStart = System.currentTimeMillis();
        startLatch.countDown(); // Release all threads

        // Monitor progress
        monitorMixedWorkloadProgress(stats, benchmarkStart);

        // Wait for completion
        boolean finished = endLatch.await(MIXED_WORKLOAD_DURATION_SECONDS + 30, TimeUnit.SECONDS);
        stopBenchmark = true;

        if (!finished) {
            System.out.println("Warning: Some threads didn't finish in time");
            executor.shutdownNow();
        } else {
            executor.shutdown();
        }

        long benchmarkEnd = System.currentTimeMillis();
        double actualDuration = (benchmarkEnd - benchmarkStart) / 1000.0;

        // Print results
        printMixedWorkloadResults(stats, actualDuration);

        leaderboard.clear().get();
    }

    private void populateMassiveData(Leaderboard<Integer> leaderboard, int totalSize) throws Exception {
        int optimalBatchSize = calculateOptimalBatchSize(totalSize);
        System.out.printf("Using batch size: %,d\n", optimalBatchSize);

        int completed = 0;
        while (completed < totalSize) {
            int currentBatchSize = Math.min(optimalBatchSize, totalSize - completed);
            List<EntryUpdateQuery<Integer>> batch = new ArrayList<>(currentBatchSize);

            for (int i = 0; i < currentBatchSize; i++) {
                String playerId = String.format("player-%08d", completed + i);
                int score = random.nextInt(1_000_000);
                batch.add(new EntryUpdateQuery<>(playerId, score));
            }

            leaderboard.update(batch).get();
            completed += currentBatchSize;

            if (completed % 100_000 == 0) {
                System.out.printf("Populated %,d / %,d entries (%.1f%%)\n",
                        completed, totalSize, (100.0 * completed / totalSize));
            }
        }
    }

    private void testMassiveScaleQueries(Leaderboard<Integer> leaderboard, int dataSize) throws Exception {
        System.out.println("Testing query performance...");

        String[] operations = {"rank()", "find()", "at()", "top(10)", "around()"};
        int testIterations = Math.min(1000, Math.max(100, 100_000 / (dataSize / 1000))); // Scale down iterations for larger datasets

        for (String operation : operations) {
            List<Long> latencies = new ArrayList<>();

            for (int i = 0; i < testIterations; i++) {
                long start = System.nanoTime();

                switch (operation) {
                    case "rank()":
                        String playerId = String.format("player-%08d", random.nextInt(dataSize));
                        leaderboard.rank(playerId).get();
                        break;
                    case "find()":
                        playerId = String.format("player-%08d", random.nextInt(dataSize));
                        leaderboard.find(playerId);
                        break;
                    case "at()":
                        long rank = random.nextInt(dataSize) + 1;
                        leaderboard.at(rank).get();
                        break;
                    case "top(10)":
                        leaderboard.top(10).get();
                        break;
                    case "around()":
                        playerId = String.format("player-%08d", random.nextInt(dataSize));
                        leaderboard.around(playerId, 5, true).get();
                        break;
                }

                long end = System.nanoTime();
                latencies.add(end - start);
            }

            LatencyStats stats = calculateLatencyStats(latencies);
            System.out.printf("  %s: avg=%.2fms, p50=%.2fms, p95=%.2fms, p99=%.2fms\n",
                    operation, stats.avg, stats.p50, stats.p95, stats.p99);
        }
    }

    private void runMixedWorkloadWorker(Leaderboard<Integer> leaderboard, int dataSize,
                                        int threadId, MixedWorkloadStats stats) {
        Random threadRandom = new Random(42 + threadId);

        while (!stopBenchmark) {
            try {
                double operationType = threadRandom.nextDouble();
                long start = System.nanoTime();

                if (operationType < READ_RATIO) {
                    // Read operation (70%)
                    performRandomReadOperation(leaderboard, dataSize, threadRandom);
                    stats.recordRead(System.nanoTime() - start);

                } else if (operationType < READ_RATIO + UPDATE_RATIO) {
                    // Update operation (20%)
                    String playerId = String.format("player-%08d", threadRandom.nextInt(dataSize));
                    int newScore = threadRandom.nextInt(1_000_000);
                    leaderboard.updateOne(playerId, newScore).get();
                    stats.recordUpdate(System.nanoTime() - start);

                } else {
                    // Batch operation (10%)
                    int batchSize = 10 + threadRandom.nextInt(40); // 10-50 updates
                    List<EntryUpdateQuery<Integer>> batch = new ArrayList<>(batchSize);

                    for (int i = 0; i < batchSize; i++) {
                        String playerId = String.format("batch-%d-player-%08d",
                                threadId, threadRandom.nextInt(dataSize));
                        int score = threadRandom.nextInt(1_000_000);
                        batch.add(new EntryUpdateQuery<>(playerId, score));
                    }

                    leaderboard.update(batch).get();
                    stats.recordBatch(System.nanoTime() - start, batchSize);
                }

                // Small random delay to simulate realistic usage
                if (threadRandom.nextInt(100) < 5) { // 5% chance
                    Thread.sleep(threadRandom.nextInt(10));
                }

            } catch (Exception e) {
                stats.recordError();
                if (!stopBenchmark) {
                    System.err.printf("Thread %d error: %s\n", threadId, e.getMessage());
                }
            }
        }
    }

    private void performRandomReadOperation(Leaderboard<Integer> leaderboard, int dataSize, Random random)
            throws Exception {
        int operation = random.nextInt(5);
        String playerId = String.format("player-%08d", random.nextInt(dataSize));

        switch (operation) {
            case 0: leaderboard.rank(playerId).get(); break;
            case 1: leaderboard.find(playerId); break;
            case 2: leaderboard.at(random.nextInt(dataSize) + 1).get(); break;
            case 3: leaderboard.top(10).get(); break;
            case 4: leaderboard.around(playerId, 5, true).get(); break;
        }
    }

    private void monitorMixedWorkloadProgress(MixedWorkloadStats stats, long startTime) {
        Thread monitor = new Thread(() -> {
            try {
                while (!stopBenchmark) {
                    Thread.sleep(10000); // Print every 10 seconds
                    if (!stopBenchmark) {
                        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
                        System.out.printf("Progress: %.0fs - Reads: %,d, Updates: %,d, Batches: %,d, Errors: %,d\n",
                                elapsed, stats.readCount.get(), stats.updateCount.get(),
                                stats.batchCount.get(), stats.errorCount.get());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        // Stop after specified duration
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopBenchmark = true;
            }
        }, MIXED_WORKLOAD_DURATION_SECONDS * 1000L);
    }

    private void printMixedWorkloadResults(MixedWorkloadStats stats, double duration) {
        System.out.println("\n=== Mixed Workload Results ===");
        System.out.printf("Duration: %.1f seconds\n", duration);
        System.out.printf("Total operations: %,d\n", stats.getTotalOperations());
        System.out.printf("Overall throughput: %.0f ops/sec\n", stats.getTotalOperations() / duration);
        System.out.printf("Errors: %,d (%.2f%%)\n", stats.errorCount.get(),
                100.0 * stats.errorCount.get() / stats.getTotalOperations());

        System.out.println("\nOperation breakdown:");
        System.out.printf("  Reads: %,d (%.1f%%, %.0f ops/sec, avg latency: %.2fms)\n",
                stats.readCount.get(),
                100.0 * stats.readCount.get() / stats.getTotalOperations(),
                stats.readCount.get() / duration,
                stats.getAverageReadLatency());

        System.out.printf("  Updates: %,d (%.1f%%, %.0f ops/sec, avg latency: %.2fms)\n",
                stats.updateCount.get(),
                100.0 * stats.updateCount.get() / stats.getTotalOperations(),
                stats.updateCount.get() / duration,
                stats.getAverageUpdateLatency());

        System.out.printf("  Batches: %,d (%.1f%%, %.0f ops/sec, avg latency: %.2fms, avg size: %.1f)\n",
                stats.batchCount.get(),
                100.0 * stats.batchCount.get() / stats.getTotalOperations(),
                stats.batchCount.get() / duration,
                stats.getAverageBatchLatency(),
                stats.getAverageBatchSize());
    }

    // Utility methods

    private Leaderboard<Integer> createLeaderboard(UpdatePolicy updatePolicy) {
        return new Leaderboard<>(
                redisExtension,
                BENCHMARK_KEY,
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, updatePolicy, -1) // No size limit
        );
    }

    private int calculateOptimalBatchSize(int totalSize) {
        if (totalSize <= 100_000) return 1000;
        if (totalSize <= 1_000_000) return 5000;
        return 10_000; // For very large datasets
    }

    private RedisMemoryInfo getRedisMemoryInfo() {
        try (Jedis jedis = jedisPool.getResource()) {
            String info = jedis.info("memory");
            long usedMemory = 0;
            long maxMemory = 0;

            for (String line : info.split("\r\n")) {
                if (line.startsWith("used_memory:")) {
                    usedMemory = Long.parseLong(line.split(":")[1]);
                } else if (line.startsWith("maxmemory:")) {
                    maxMemory = Long.parseLong(line.split(":")[1]);
                }
            }

            return new RedisMemoryInfo(usedMemory, maxMemory);
        }
    }

    private void printRedisInfo() {
        try (Jedis jedis = jedisPool.getResource()) {
            String info = jedis.info("server");
            for (String line : info.split("\r\n")) {
                if (line.startsWith("redis_version:")) {
                    System.out.println("Redis version: " + line.split(":")[1]);
                    break;
                }
            }

            RedisMemoryInfo memInfo = getRedisMemoryInfo();
            System.out.printf("Redis memory: %s used", formatBytes(memInfo.usedMemory));
            if (memInfo.maxMemory > 0) {
                System.out.printf(" / %s max", formatBytes(memInfo.maxMemory));
            }
            System.out.println();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private LatencyStats calculateLatencyStats(List<Long> latenciesNanos) {
        latenciesNanos.sort(Long::compareTo);

        double avg = latenciesNanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double p50 = latenciesNanos.get(latenciesNanos.size() / 2) / 1_000_000.0;
        double p95 = latenciesNanos.get((int) (latenciesNanos.size() * 0.95)) / 1_000_000.0;
        double p99 = latenciesNanos.get((int) (latenciesNanos.size() * 0.99)) / 1_000_000.0;

        return new LatencyStats(avg, p50, p95, p99);
    }

    private static JedisPoolConfig buildOptimizedPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(200);  // Increased for concurrent load
        poolConfig.setMaxIdle(50);
        poolConfig.setMinIdle(20);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    // Helper classes

    private static class RedisMemoryInfo {
        final long usedMemory;
        final long maxMemory;

        RedisMemoryInfo(long usedMemory, long maxMemory) {
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
        }

        @Override
        public String toString() {
            String result = formatBytes(usedMemory) + " used";
            if (maxMemory > 0) {
                result += " / " + formatBytes(maxMemory) + " max";
            }
            return result;
        }

        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private static class LatencyStats {
        final double avg, p50, p95, p99;

        LatencyStats(double avg, double p50, double p95, double p99) {
            this.avg = avg;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
        }
    }

    private static class MixedWorkloadStats {
        final AtomicLong readCount = new AtomicLong();
        final AtomicLong updateCount = new AtomicLong();
        final AtomicLong batchCount = new AtomicLong();
        final AtomicLong errorCount = new AtomicLong();

        final AtomicLong totalReadLatency = new AtomicLong();
        final AtomicLong totalUpdateLatency = new AtomicLong();
        final AtomicLong totalBatchLatency = new AtomicLong();
        final AtomicLong totalBatchSize = new AtomicLong();

        void recordRead(long latencyNanos) {
            readCount.incrementAndGet();
            totalReadLatency.addAndGet(latencyNanos);
        }

        void recordUpdate(long latencyNanos) {
            updateCount.incrementAndGet();
            totalUpdateLatency.addAndGet(latencyNanos);
        }

        void recordBatch(long latencyNanos, int batchSize) {
            batchCount.incrementAndGet();
            totalBatchLatency.addAndGet(latencyNanos);
            totalBatchSize.addAndGet(batchSize);
        }

        void recordError() {
            errorCount.incrementAndGet();
        }

        long getTotalOperations() {
            return readCount.get() + updateCount.get() + batchCount.get();
        }

        double getAverageReadLatency() {
            return readCount.get() > 0 ? totalReadLatency.get() / 1_000_000.0 / readCount.get() : 0;
        }

        double getAverageUpdateLatency() {
            return updateCount.get() > 0 ? totalUpdateLatency.get() / 1_000_000.0 / updateCount.get() : 0;
        }

        double getAverageBatchLatency() {
            return batchCount.get() > 0 ? totalBatchLatency.get() / 1_000_000.0 / batchCount.get() : 0;
        }

        double getAverageBatchSize() {
            return batchCount.get() > 0 ? (double) totalBatchSize.get() / batchCount.get() : 0;
        }
    }
}
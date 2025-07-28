package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PeriodicLeaderboardTest {

    private static RedisExtension redisExtension;
    private PeriodicLeaderboard<Integer> periodicLeaderboard;

    @BeforeAll
    public static void setUpClass() {
        JedisPoolConfig poolConfig = buildPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 5, null);
        redisExtension = new RedisExtension(jedisPool);
        redisExtension.prepare();
    }

    @BeforeEach
    public void setUp() {
        periodicLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-periodic",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.MINUTE
                )
        );
    }

    private static JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        return poolConfig;
    }

    @Test
    public void testGetKeyForMinuteCycle() {
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = periodicLeaderboard.getKey(testTime);
        assertEquals("y2024-m12-d25-h14-m30", key);
    }

    @Test
    public void testGetKeyForHourlyCycle() {
        PeriodicLeaderboard<Integer> hourlyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-hourly",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.HOURLY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = hourlyLeaderboard.getKey(testTime);
        assertEquals("y2024-m12-d25-h14", key);
    }

    @Test
    public void testGetKeyForDailyCycle() {
        PeriodicLeaderboard<Integer> dailyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-daily",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.DAILY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = dailyLeaderboard.getKey(testTime);
        assertEquals("y2024-m12-d25", key);
    }

    @Test
    public void testGetKeyForMonthlyCycle() {
        PeriodicLeaderboard<Integer> monthlyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-monthly",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.MONTHLY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = monthlyLeaderboard.getKey(testTime);
        assertEquals("y2024-m12", key);
    }

    @Test
    public void testGetKeyForYearlyCycle() {
        PeriodicLeaderboard<Integer> yearlyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-yearly",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.YEARLY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = yearlyLeaderboard.getKey(testTime);
        assertEquals("y2024", key);
    }

    @Test
    public void testGetKeyForWeeklyCycle() {
        PeriodicLeaderboard<Integer> weeklyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-weekly",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.WEEKLY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45); // Monday
        String key = weeklyLeaderboard.getKey(testTime);
        assertTrue(key.startsWith("w"));
        assertEquals(5, key.length()); // Format: w####
    }

    @Test
    public void testCustomCycleFunction() {
        CycleFunction customCycle = time -> "custom-" + time.getYear() + "-" + time.getMonthValue();
        
        PeriodicLeaderboard<Integer> customLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-custom",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        customCycle
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String key = customLeaderboard.getKey(testTime);
        assertEquals("custom-2024-12", key);
    }

    @Test
    public void testGetLeaderboard() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> lb1 = periodicLeaderboard.getLeaderboard("test-key-1");
        Leaderboard<Integer> lb2 = periodicLeaderboard.getLeaderboard("test-key-1");
        
        // Should return the same cached instance
        assertSame(lb1, lb2);
        
        // Test that it works
        lb1.updateOne("player1", 100).get();
        Entry<Integer> entry = lb1.find("player1");
        assertNotNull(entry);
        assertEquals(100, entry.score());
    }

    @Test
    public void testGetLeaderboardAt() throws ExecutionException, InterruptedException {
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        Leaderboard<Integer> lb = periodicLeaderboard.getLeaderboardAt(testTime);
        
        lb.updateOne("player1", 200).get();
        Entry<Integer> entry = lb.find("player1");
        assertNotNull(entry);
        assertEquals(200, entry.score());
        
        // Getting the same time should return the same leaderboard
        Leaderboard<Integer> lb2 = periodicLeaderboard.getLeaderboardAt(testTime);
        Entry<Integer> entry2 = lb2.find("player1");
        assertNotNull(entry2);
        assertEquals(200, entry2.score());
    }

    @Test
    public void testGetLeaderboardAtNull() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> lb = periodicLeaderboard.getLeaderboardAt(null);
        assertNotNull(lb);
        
        // Should use current time
        lb.updateOne("player1", 150).get();
        Entry<Integer> entry = lb.find("player1");
        assertNotNull(entry);
        assertEquals(150, entry.score());
    }

    @Test
    public void testGetKeyNow() {
        String keyNow = periodicLeaderboard.getKeyNow();
        assertNotNull(keyNow);
        assertFalse(keyNow.isEmpty());
        
        // Should follow the minute cycle format
        assertTrue(keyNow.matches("y\\d{4}-m\\d{2}-d\\d{2}-h\\d{2}-m\\d{2}"));
    }

    @Test
    public void testGetLeaderboardNow() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> lbNow = periodicLeaderboard.getLeaderboardNow();
        assertNotNull(lbNow);
        
        lbNow.updateOne("current-player", 300).get();
        Entry<Integer> entry = lbNow.find("current-player");
        assertNotNull(entry);
        assertEquals(300, entry.score());
    }

    @Test
    public void testCustomNowFunction() throws ExecutionException, InterruptedException {
        LocalDateTime fixedTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        NowFunction fixedNow = () -> fixedTime;
        
        PeriodicLeaderboard<Integer> fixedTimeLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-fixed-time",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.MINUTE,
                        fixedNow
                )
        );

        String keyNow = fixedTimeLeaderboard.getKeyNow();
        assertEquals("y2024-m06-d15-h10-m30", keyNow);
        
        Leaderboard<Integer> lb = fixedTimeLeaderboard.getLeaderboardNow();
        lb.updateOne("fixed-player", 400).get();
        
        Entry<Integer> entry = lb.find("fixed-player");
        assertNotNull(entry);
        assertEquals(400, entry.score());
    }

    @Test
    public void testLeaderboardCaching() {
        // Create multiple leaderboards to test caching
        Leaderboard<Integer> lb1 = periodicLeaderboard.getLeaderboard("cache-test-1");
        Leaderboard<Integer> lb2 = periodicLeaderboard.getLeaderboard("cache-test-2");
        Leaderboard<Integer> lb1Again = periodicLeaderboard.getLeaderboard("cache-test-1");
        
        // Same key should return same instance
        assertSame(lb1, lb1Again);
        
        // Different keys should return different instances
        assertNotSame(lb1, lb2);
    }

    @Test
    public void testGetExistingKeys() throws ExecutionException, InterruptedException {
        // Create some leaderboards with data
        LocalDateTime time1 = LocalDateTime.of(2024, 12, 25, 14, 30, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 12, 25, 14, 31, 0);
        
        Leaderboard<Integer> lb1 = periodicLeaderboard.getLeaderboardAt(time1);
        Leaderboard<Integer> lb2 = periodicLeaderboard.getLeaderboardAt(time2);
        
        lb1.updateOne("player1", 100).get();
        lb2.updateOne("player2", 200).get();
        
        Set<String> existingKeys = periodicLeaderboard.getExistingKeys();
        assertNotNull(existingKeys);
        
        // Should contain the keys we created
        assertTrue(existingKeys.contains("y2024-m12-d25-h14-m30"));
        assertTrue(existingKeys.contains("y2024-m12-d25-h14-m31"));
    }

    @Test
    public void testDifferentDataTypes() throws ExecutionException, InterruptedException {
        PeriodicLeaderboard<Double> doubleLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-double-periodic",
                Double.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.HOURLY
                )
        );

        Leaderboard<Double> lb = doubleLeaderboard.getLeaderboardNow();
        lb.updateOne("double-player", 99.99).get();
        
        Entry<Double> entry = lb.find("double-player");
        assertNotNull(entry);
        assertEquals(99.99, entry.score(), 0.001);
    }

    @Test
    public void testInvalidCycleType() {
        assertThrows(IllegalStateException.class, () -> {
            PeriodicLeaderboard<Integer> invalidLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                    "test-invalid",
                    Integer.class,
                    new PeriodicLeaderboardOptions(
                            new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                            "invalid-cycle-type" // This should cause an error
                    )
            );
            
            LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
            invalidLeaderboard.getKey(testTime);
        });
    }

    @Test
    public void testMultiplePeriodicLeaderboards() throws ExecutionException, InterruptedException {
        PeriodicLeaderboard<Integer> daily = new PeriodicLeaderboard<>(redisExtension,
                "test-daily-multi",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.DAILY
                )
        );

        PeriodicLeaderboard<Integer> hourly = new PeriodicLeaderboard<>(redisExtension,
                "test-hourly-multi",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.HOURLY
                )
        );

        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        
        Leaderboard<Integer> dailyLb = daily.getLeaderboardAt(testTime);
        Leaderboard<Integer> hourlyLb = hourly.getLeaderboardAt(testTime);
        
        dailyLb.updateOne("player1", 100).get();
        hourlyLb.updateOne("player1", 200).get();
        
        // Should be independent
        Entry<Integer> dailyEntry = dailyLb.find("player1");
        Entry<Integer> hourlyEntry = hourlyLb.find("player1");
        
        assertEquals(100, dailyEntry.score());
        assertEquals(200, hourlyEntry.score());
    }

    @Test
    public void testLeaderboardOptionsInheritance() throws ExecutionException, InterruptedException {
        PeriodicLeaderboard<Integer> bestPolicyLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "test-best-policy",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.LOW_TO_HIGH, UpdatePolicy.BEST, 50),
                        DefaultCycles.MINUTE
                )
        );

        Leaderboard<Integer> lb = bestPolicyLeaderboard.getLeaderboardNow();
        
        // Test that the update policy is inherited
        lb.updateOne("player1", 100).get();
        lb.updateOne("player1", 200).get(); // Should update to 200 (better for LOW_TO_HIGH is lower, but BEST means higher value)
        lb.updateOne("player1", 50).get();  // Should not update (50 < 200)
        
        Entry<Integer> entry = lb.find("player1");
        assertEquals(200, entry.score());
    }
}
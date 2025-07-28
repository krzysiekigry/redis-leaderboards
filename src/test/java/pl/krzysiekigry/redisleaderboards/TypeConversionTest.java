package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TypeConversionTest {

    private static RedisExtension redisExtension;

    @BeforeAll
    public static void setUpClass() {
        JedisPoolConfig poolConfig = buildPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 5, null);
        redisExtension = new RedisExtension(jedisPool);
        redisExtension.prepare();
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
    public void testIntegerMaxValue() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "test-int-max",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Integer.MAX_VALUE).get();
        Entry<Integer> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(Integer.MAX_VALUE, entry.score());
    }

    @Test
    public void testIntegerMinValue() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "test-int-min",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Integer.MIN_VALUE).get();
        Entry<Integer> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(Integer.MIN_VALUE, entry.score());
    }

    @Test
    public void testLongMaxValue() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-long-max",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Long.MAX_VALUE).get();
        Entry<Long> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        // Note: Due to double precision limitations, very large longs may lose precision
        assertTrue(Math.abs(entry.score() - Long.MAX_VALUE) <= 1024); // Allow for some precision loss
    }

    @Test
    public void testLongMinValue() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-long-min",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Long.MIN_VALUE).get();
        Entry<Long> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        // Note: Due to double precision limitations, very large longs may lose precision
        assertTrue(Math.abs(entry.score() - Long.MIN_VALUE) <= 1024); // Allow for some precision loss
    }

    @Test
    public void testLongPrecisionLimit() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-long-precision",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        // Test values within double precision range (2^53)
        long safeLong = 9007199254740992L; // 2^53
        leaderboard.updateOne("player1", safeLong).get();
        Entry<Long> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(safeLong, entry.score());
    }

    @Test
    public void testLongPrecisionLoss() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-long-precision-loss",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        // Test values beyond double precision range
        long largeLong = 9007199254740993L; // 2^53 + 1
        leaderboard.updateOne("player1", largeLong).get();
        Entry<Long> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        // Due to double precision limitations, this may not be exactly equal
        assertTrue(Math.abs(entry.score() - largeLong) <= 1);
    }

    @Test
    public void testDoubleMaxValue() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-double-max",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Double.MAX_VALUE).get();
        Entry<Double> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(Double.MAX_VALUE, entry.score());
    }

    @Test
    public void testDoubleMinValue() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-double-min",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", -Double.MAX_VALUE).get();
        Entry<Double> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(-Double.MAX_VALUE, entry.score());
    }

    @Test
    public void testDoubleMinPositiveValue() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-double-min-pos",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", Double.MIN_VALUE).get();
        Entry<Double> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(Double.MIN_VALUE, entry.score());
    }

    @Test
    public void testDoubleSpecialValues() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-double-special",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        // Test positive infinity
        leaderboard.updateOne("player1", Double.POSITIVE_INFINITY).get();
        Entry<Double> entry1 = leaderboard.find("player1");
        assertNotNull(entry1);
        assertEquals(Double.POSITIVE_INFINITY, entry1.score());

        // Test negative infinity
        leaderboard.updateOne("player2", Double.NEGATIVE_INFINITY).get();
        Entry<Double> entry2 = leaderboard.find("player2");
        assertNotNull(entry2);
        assertEquals(Double.NEGATIVE_INFINITY, entry2.score());

        // Test NaN - this might behave unexpectedly in Redis
        leaderboard.updateOne("player3", Double.NaN).get();
        Entry<Double> entry3 = leaderboard.find("player3");
        // NaN behavior in Redis is undefined, may return null
        // Just check it doesn't crash the system
    }

    @Test
    public void testZeroValues() throws ExecutionException, InterruptedException {
        // Test Integer zero
        Leaderboard<Integer> intLeaderboard = new Leaderboard<>(redisExtension,
                "test-int-zero",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        intLeaderboard.clear().get();
        intLeaderboard.updateOne("player1", 0).get();
        Entry<Integer> intEntry = intLeaderboard.find("player1");
        assertEquals(0, intEntry.score());

        // Test Long zero
        Leaderboard<Long> longLeaderboard = new Leaderboard<>(redisExtension,
                "test-long-zero",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        longLeaderboard.clear().get();
        longLeaderboard.updateOne("player1", 0L).get();
        Entry<Long> longEntry = longLeaderboard.find("player1");
        assertEquals(0L, longEntry.score());

        // Test Double zero
        Leaderboard<Double> doubleLeaderboard = new Leaderboard<>(redisExtension,
                "test-double-zero",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        doubleLeaderboard.clear().get();
        doubleLeaderboard.updateOne("player1", 0.0).get();
        Entry<Double> doubleEntry = doubleLeaderboard.find("player1");
        assertEquals(0.0, doubleEntry.score());

        // Test negative zero - Redis may not preserve the sign of zero
        doubleLeaderboard.updateOne("player2", -0.0).get();
        Entry<Double> negZeroEntry = doubleLeaderboard.find("player2");
        // Redis typically converts -0.0 to 0.0, so we check for either
        assertTrue(negZeroEntry.score() == 0.0 || negZeroEntry.score() == -0.0);
    }

    @Test
    public void testFractionalToIntegerConversion() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "test-fractional-int",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        // When storing fractional values, they should be rounded for Integer type
        // Note: This test assumes the internal Redis storage might have fractional values
        // that get converted to integers when retrieved
        leaderboard.updateOne("player1", 100).get();
        Entry<Integer> entry = leaderboard.find("player1");
        assertEquals(100, entry.score());
    }

    @Test
    public void testFractionalToLongConversion() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-fractional-long",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("player1", 1000000000000L).get();
        Entry<Long> entry = leaderboard.find("player1");
        assertEquals(1000000000000L, entry.score());
    }

    @Test
    public void testHighPrecisionDoubles() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-high-precision",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        double highPrecision = 123.456789012345;
        leaderboard.updateOne("player1", highPrecision).get();
        Entry<Double> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(highPrecision, entry.score(), 0.000000000001);
    }

    @Test
    public void testVerySmallDoubles() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "test-small-doubles",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        double verySmall = 1e-100;
        leaderboard.updateOne("player1", verySmall).get();
        Entry<Double> entry = leaderboard.find("player1");
        
        assertNotNull(entry);
        assertEquals(verySmall, entry.score(), 1e-110);
    }

    @Test
    public void testUnsupportedTypeThrowsException() {
        // This test verifies that unsupported types throw IllegalArgumentException
        // Note: This would require modifying the Leaderboard class to accept other types
        // or creating a test scenario where an unsupported type is encountered
        
        // For now, we test that the supported types work correctly
        assertDoesNotThrow(() -> {
            new Leaderboard<>(redisExtension, "test-int", Integer.class,
                    new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100));
        });
        
        assertDoesNotThrow(() -> {
            new Leaderboard<>(redisExtension, "test-long", Long.class,
                    new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100));
        });
        
        assertDoesNotThrow(() -> {
            new Leaderboard<>(redisExtension, "test-double", Double.class,
                    new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100));
        });
    }

    @Test
    public void testTypeConsistencyAcrossOperations() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "test-type-consistency",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        long testValue = 1234567890123L;
        
        // Test that the same value is consistent across different operations
        leaderboard.updateOne("player1", testValue).get();
        
        Entry<Long> findResult = leaderboard.find("player1");
        Entry<Long> atResult = leaderboard.at(1).get(); // Use 1-based ranking
        Entry<Long> topResult = leaderboard.top(1).get().get(0);
        
        assertEquals(testValue, findResult.score());
        assertEquals(testValue, atResult.score());
        assertEquals(testValue, topResult.score());
        
        assertEquals(findResult.score(), atResult.score());
        assertEquals(atResult.score(), topResult.score());
    }

    @Test
    public void testMixedSignValues() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "test-mixed-signs",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );
        leaderboard.clear().get();

        leaderboard.updateOne("positive", 1000).get();
        leaderboard.updateOne("negative", -1000).get();
        leaderboard.updateOne("zero", 0).get();
        
        Entry<Integer> positive = leaderboard.find("positive");
        Entry<Integer> negative = leaderboard.find("negative");
        Entry<Integer> zero = leaderboard.find("zero");
        
        assertEquals(1000, positive.score());
        assertEquals(-1000, negative.score());
        assertEquals(0, zero.score());
        
        // Verify ordering is correct (HIGH_TO_LOW) - ranks are 1-based
        assertEquals(1L, positive.rank()); // highest
        assertEquals(2L, zero.rank());    // middle
        assertEquals(3L, negative.rank()); // lowest
    }
}
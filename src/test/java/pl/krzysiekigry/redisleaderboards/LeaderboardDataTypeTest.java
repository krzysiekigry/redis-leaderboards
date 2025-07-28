package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeaderboardDataTypeTest {

    private static RedisExtension redisExtension;

    @BeforeAll
    public static void setUp() {
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
    public void testLongDataType() throws ExecutionException, InterruptedException {
        Leaderboard<Long> leaderboard = new Leaderboard<>(redisExtension,
                "long-scores",
                Long.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );

        leaderboard.clear().get();
        leaderboard.updateOne("player1", 10000000000L).get();
        leaderboard.updateOne("player2", 20000000000L).get();

        List<Entry<Long>> topPlayers = leaderboard.top(2).get();
        assertEquals(2, topPlayers.size());
        assertEquals("player2", topPlayers.get(0).id());
        assertEquals(20000000000L, topPlayers.get(0).score());
        assertEquals("player1", topPlayers.get(1).id());
        assertEquals(10000000000L, topPlayers.get(1).score());
    }

    @Test
    public void testDoubleDataType() throws ExecutionException, InterruptedException {
        Leaderboard<Double> leaderboard = new Leaderboard<>(redisExtension,
                "double-scores",
                Double.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100)
        );

        leaderboard.clear().get();
        leaderboard.updateOne("player1", 99.99).get();
        leaderboard.updateOne("player2", 199.99).get();
        leaderboard.updateOne("player3", 299.99).get();

        List<Entry<Double>> topPlayers = leaderboard.top(3).get();
        assertEquals(3, topPlayers.size());
        assertEquals("player3", topPlayers.get(0).id());
        assertEquals(299.99, topPlayers.get(0).score());
        assertEquals("player2", topPlayers.get(1).id());
        assertEquals(199.99, topPlayers.get(1).score());
        assertEquals("player1", topPlayers.get(2).id());
        assertEquals(99.99, topPlayers.get(2).score());
    }

    @Test
    public void testIntegerDataType() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "integer-scores",
                Integer.class,
                new LeaderboardOptions(SortPolicy.LOW_TO_HIGH, UpdatePolicy.REPLACE, 100)
        );

        leaderboard.clear().get();
        leaderboard.updateOne("player1", 100).get();
        leaderboard.updateOne("player2", 50).get();
        leaderboard.updateOne("player3", 200).get();

        List<Entry<Integer>> topPlayers = leaderboard.top(3).get();
        assertEquals(3, topPlayers.size());
        assertEquals("player2", topPlayers.get(0).id());
        assertEquals(50, topPlayers.get(0).score());
        assertEquals("player1", topPlayers.get(1).id());
        assertEquals(100, topPlayers.get(1).score());
        assertEquals("player3", topPlayers.get(2).id());
        assertEquals(200, topPlayers.get(2).score());
    }
}

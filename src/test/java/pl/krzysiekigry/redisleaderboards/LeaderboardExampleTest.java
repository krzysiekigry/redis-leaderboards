package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LeaderboardExampleTest {

    private static RedisExtension redisExtension;

    @BeforeAll
    public static void setUp() {
        JedisPoolConfig poolConfig = buildPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 5, null);
        redisExtension = new RedisExtension(jedisPool);
        redisExtension.prepare();
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

    @Test
    public void testStandardLeaderboard() throws ExecutionException, InterruptedException {
        Leaderboard<Integer> leaderboard = new Leaderboard<>(redisExtension,
                "game-scores",
                Integer.class,
                new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.BEST, 100)
        );

        leaderboard.updateOne("player1", 50).get();
        leaderboard.update(List.of(
                new EntryUpdateQuery<>("player2", 100),
                new EntryUpdateQuery<>("player3", 150),
                new EntryUpdateQuery<>("player4", 200))
        ).get();

        List<Entry<Integer>> top5 = leaderboard.top(5).get();
        assertEquals(4, top5.size());
        assertEquals("player4", top5.get(0).id());

        Entry<Integer> playerAt1 = leaderboard.at(1).get();
        assertEquals("player4", playerAt1.id());
    }

    @Test
    public void testPeriodicLeaderboard() throws ExecutionException, InterruptedException {
        PeriodicLeaderboard<Integer> periodicLeaderboard = new PeriodicLeaderboard<>(redisExtension,
                "periodic-scores",
                Integer.class,
                new PeriodicLeaderboardOptions(
                        new LeaderboardOptions(SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100),
                        DefaultCycles.MINUTE
                )
        );

        Leaderboard<Integer> leaderboardNow = periodicLeaderboard.getLeaderboardNow();
        leaderboardNow.update(List.of(
                new EntryUpdateQuery<>("player1", 50),
                new EntryUpdateQuery<>("player2", 100),
                new EntryUpdateQuery<>("player3", 150),
                new EntryUpdateQuery<>("player4", 200))
        ).get();

        Entry<Integer> playerAt1 = leaderboardNow.at(1).get();
        assertEquals("player4", playerAt1.id());

        leaderboardNow.updateOne("player1", 500).get();

        Entry<Integer> playerAt1AfterUpdate = leaderboardNow.at(1).get();
        assertEquals("player1", playerAt1AfterUpdate.id());
    }
}

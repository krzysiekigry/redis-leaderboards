package pl.krzysiekigry.redisleaderboards;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis connection and Lua script management extension for leaderboard operations.
 * <p>
 * This class provides a centralized way to manage Redis connections through a connection pool
 * and handles the loading and caching of Lua scripts used by the leaderboard system.
 * It ensures that scripts are loaded only once and provides efficient access to both
 * Redis connections and script SHA identifiers.
 * </p>
 * <p>
 * The class automatically loads the following Lua scripts on first use:
 * <ul>
 *   <li>zaround - for retrieving entries around a specific position</li>
 *   <li>zbest - for finding the best score operations</li>
 *   <li>zfind - for finding specific entries</li>
 *   <li>zkeeptop - for maintaining top-N entries</li>
 *   <li>zrangescore - for range-based score queries</li>
 * </ul>
 * 
 * @see Leaderboard for usage in leaderboard operations
 * @see PeriodicLeaderboard for usage in periodic leaderboards
 */
public class RedisExtension {

    private final JedisPool jedisPool;
    private final Map<String, String> scriptShas = new HashMap<>();
    private final AtomicBoolean scriptsLoaded = new AtomicBoolean(false);

    /**
     * Creates a new RedisExtension with the specified Jedis connection pool.
     * 
     * @param jedisPool the Jedis connection pool to use for Redis operations
     */
    public RedisExtension(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /**
     * Prepares the Redis extension by loading all required Lua scripts.
     * <p>
     * This method is thread-safe and ensures that scripts are loaded only once.
     * It can be called multiple times safely - subsequent calls will be ignored
     * if scripts have already been loaded.
     * </p>
     */
    public void prepare() {
        if (scriptsLoaded.compareAndSet(false, true)) {
            try (Jedis jedis = getJedis()) {
                loadScript(jedis, "zaround");
                loadScript(jedis, "zbest");
                loadScript(jedis, "zfind");
                loadScript(jedis, "zkeeptop");
                loadScript(jedis, "zrangescore");
            }
        }
    }

    private void loadScript(Jedis jedis, String scriptName) {
        try {
            byte[] scriptBytes = Files.readAllBytes(Paths.get(getClass().getResource("/lua/" + scriptName + ".lua").toURI()));
            String script = new String(scriptBytes, StandardCharsets.UTF_8);
            scriptShas.put(scriptName, jedis.scriptLoad(script));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to load Lua script: " + scriptName, e);
        }
    }

    public String getScriptSha(String scriptName) {
        if (!scriptsLoaded.get()) {
            prepare();
        }
        return scriptShas.get(scriptName);
    }

    public Jedis getJedis() {
        return jedisPool.getResource();
    }
}

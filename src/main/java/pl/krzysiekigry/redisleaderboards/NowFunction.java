package pl.krzysiekigry.redisleaderboards;

import java.time.LocalDateTime;

/**
 * A functional interface for providing the current time.
 * <p>
 * This interface allows for customizable time providers, which is particularly
 * useful for testing scenarios where you need to control the current time,
 * or for applications that need to use alternative time sources.
 * </p>
 * <p>
 * The default implementation typically uses {@link LocalDateTime#now()},
 * but custom implementations can provide fixed times, offset times,
 * or times from external sources.
 * </p>
 * 
 * @see PeriodicLeaderboard for usage in time-based leaderboards
 * @see PeriodicLeaderboardOptions for configuration with custom time providers
 */
@FunctionalInterface
public interface NowFunction {
    
    /**
     * Returns the current time as a LocalDateTime.
     * <p>
     * Implementations should return a consistent representation of the
     * current time that will be used for cycle calculations and time-based
     * operations within the leaderboard system.
     * </p>
     * 
     * @return the current time
     */
    LocalDateTime get();
}

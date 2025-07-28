package pl.krzysiekigry.redisleaderboards;

import java.time.LocalDateTime;

/**
 * Configuration options for periodic leaderboards with time-based cycles.
 * <p>
 * This record encapsulates the configuration needed to create and manage
 * periodic leaderboards, including the underlying leaderboard options,
 * the time cycle definition, and the time provider function.
 * </p>
 * <p>
 * The cycle parameter can be either a {@link DefaultCycles} enum value
 * for predefined time periods, or a custom {@link CycleFunction} for
 * specialized time-based grouping logic.
 * </p>
 * 
 * @param leaderboardOptions the configuration options for individual leaderboards
 * @param cycle the time cycle definition (DefaultCycles enum or CycleFunction)
 * @param now the function to provide current time (defaults to LocalDateTime::now)
 * 
 * @see PeriodicLeaderboard for usage in periodic leaderboard creation
 * @see LeaderboardOptions for individual leaderboard configuration
 * @see DefaultCycles for predefined time cycles
 * @see CycleFunction for custom cycle implementations
 * @see NowFunction for custom time providers
 */
public record PeriodicLeaderboardOptions(LeaderboardOptions leaderboardOptions, Object cycle, NowFunction now) {

    /**
     * Creates periodic leaderboard options with the default time provider.
     * <p>
     * This convenience constructor uses {@link LocalDateTime#now()} as the time provider.
     * </p>
     * 
     * @param leaderboardOptions the configuration options for individual leaderboards
     * @param cycle the time cycle definition (DefaultCycles enum or CycleFunction)
     */
    public PeriodicLeaderboardOptions(LeaderboardOptions leaderboardOptions, Object cycle) {
        this(leaderboardOptions, cycle, LocalDateTime::now);
    }

    /**
     * Creates periodic leaderboard options with all parameters specified.
     *
     * @param leaderboardOptions the configuration options for individual leaderboards
     * @param cycle the time cycle definition (DefaultCycles enum or CycleFunction)
     * @param now the function to provide current time
     */
    public PeriodicLeaderboardOptions {
    }
}

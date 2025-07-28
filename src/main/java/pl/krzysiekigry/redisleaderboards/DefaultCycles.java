package pl.krzysiekigry.redisleaderboards;

/**
 * Predefined time-based cycle constants for periodic leaderboards.
 * <p>
 * This enum provides standard time periods that can be used to create
 * periodic leaderboards with automatic cycle generation. Each constant
 * represents a different time granularity for grouping leaderboard data.
 * </p>
 * <p>
 * These cycles are automatically converted to appropriate {@link CycleFunction}
 * implementations by the {@link PeriodicLeaderboard} class.
 * </p>
 * 
 * @see CycleFunction for custom cycle implementations
 * @see PeriodicLeaderboard for usage in periodic leaderboards
 */
public enum DefaultCycles {

    /** Cycle that resets every minute */
    MINUTE,
    
    /** Cycle that resets every hour */
    HOURLY,
    
    /** Cycle that resets every day */
    DAILY,
    
    /** Cycle that resets every week */
    WEEKLY,
    
    /** Cycle that resets every month */
    MONTHLY,
    
    /** Cycle that resets every year */
    YEARLY;
}

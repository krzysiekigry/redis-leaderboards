package pl.krzysiekigry.redisleaderboards;

import java.time.LocalDateTime;

/**
 * A functional interface for generating cycle identifiers based on time.
 * <p>
 * This interface is used to create custom time-based cycles for periodic leaderboards.
 * Implementations should convert a given {@link LocalDateTime} into a string identifier
 * that represents a specific time cycle (e.g., "2024-week-42", "2024-12-25", etc.).
 * </p>
 * 
 * @see DefaultCycles for predefined cycle implementations
 * @see PeriodicLeaderboard for usage in periodic leaderboards
 */
@FunctionalInterface
public interface CycleFunction {
    
    /**
     * Generates a cycle identifier string based on the provided time.
     * <p>
     * The returned string should uniquely identify a time cycle and be consistent
     * for all times within the same cycle period.
     * </p>
     * 
     * @param now the time to generate a cycle identifier for
     * @return a string identifier representing the time cycle
     */
    String apply(LocalDateTime now);
}

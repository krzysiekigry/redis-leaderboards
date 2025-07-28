package pl.krzysiekigry.redisleaderboards;

/**
 * Defines the strategy for updating leaderboard entry scores.
 * <p>
 * This enum specifies how new score values should be handled when updating
 * existing leaderboard entries. Different policies provide flexibility for
 * various scoring scenarios and business requirements.
 * </p>
 * 
 * @see LeaderboardOptions for usage in leaderboard configuration
 * @see Leaderboard#updateOne(String, Number, UpdatePolicy) for single entry updates
 * @see Leaderboard#update(java.util.List, UpdatePolicy) for batch updates
 * @see EntryUpdateQuery for update query structure
 */
public enum UpdatePolicy {

    /**
     * Replace the existing score with the new value.
     * <p>
     * This policy completely overwrites the current score with the new value,
     * regardless of which value is higher or lower. This is useful for scenarios
     * where the latest score is always the most accurate or relevant.
     * </p>
     */
    REPLACE,
    
    /**
     * Add the new value to the existing score.
     * <p>
     * This policy aggregates scores by adding the new value to the current score.
     * This is useful for accumulative scoring systems where multiple contributions
     * should be summed together (e.g., total points earned over time).
     * </p>
     */
    AGGREGATE,
    
    /**
     * Keep the best score between the existing and new values.
     * <p>
     * This policy compares the existing score with the new value and keeps
     * whichever is considered "better" according to the leaderboard's sort policy.
     * For HIGH_TO_LOW sorting, the higher value is kept; for LOW_TO_HIGH sorting,
     * the lower value is kept. This is useful for personal best tracking.
     * </p>
     */
    BEST
}

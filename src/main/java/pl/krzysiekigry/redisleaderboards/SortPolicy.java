package pl.krzysiekigry.redisleaderboards;

/**
 * Defines the sorting order for leaderboard entries.
 * <p>
 * This enum specifies how entries should be ordered in the leaderboard,
 * determining whether higher scores or lower scores appear at the top.
 * The sorting policy affects ranking calculations, entry retrieval order,
 * and all position-based operations.
 * </p>
 * 
 * @see LeaderboardOptions for usage in leaderboard configuration
 * @see Leaderboard for leaderboard operations affected by sort policy
 */
public enum SortPolicy {

    /** 
     * Sort entries from highest score to lowest score.
     * <p>
     * In this mode, entries with higher scores receive better (lower) ranks.
     * This is typical for scoring systems where higher values indicate better performance.
     * </p>
     */
    HIGH_TO_LOW("high-to-low"),
    
    /** 
     * Sort entries from lowest score to highest score.
     * <p>
     * In this mode, entries with lower scores receive better (lower) ranks.
     * This is typical for timing-based systems where lower values indicate better performance.
     * </p>
     */
    LOW_TO_HIGH("low-to-high");

    private final String value;

    SortPolicy(String value) {
        this.value = value;
    }

    /**
     * Returns the string representation of this sort policy.
     * 
     * @return the string value of this sort policy
     */
    public String getValue() {
        return value;
    }
}

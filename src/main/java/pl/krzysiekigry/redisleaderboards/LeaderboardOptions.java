package pl.krzysiekigry.redisleaderboards;

/**
 * Configuration options for leaderboard behavior and constraints.
 * <p>
 * This record encapsulates the key configuration parameters that control
 * how a leaderboard operates, including sorting behavior, update strategies,
 * and size limitations.
 * </p>
 * 
 * @param sortPolicy the policy determining the sort order of entries (high-to-low or low-to-high)
 * @param updatePolicy the default strategy for handling score updates (replace, aggregate, or best)
 * @param limitTopN the maximum number of entries to keep in the leaderboard (0 for unlimited)
 * 
 * @see SortPolicy for available sorting options
 * @see UpdatePolicy for available update strategies
 * @see Leaderboard for usage in leaderboard creation
 */
public record LeaderboardOptions(
        SortPolicy sortPolicy,
        UpdatePolicy updatePolicy,
        int limitTopN
) {
}

package pl.krzysiekigry.redisleaderboards;

/**
 * Represents an update query for modifying a leaderboard entry's score.
 * <p>
 * This record encapsulates the information needed to update a specific
 * leaderboard entry: the unique identifier of the entry to update and
 * the new numeric value to apply. The actual update behavior depends on
 * the {@link UpdatePolicy} used during the update operation.
 * </p>
 * 
 * @param <T> the numeric type of the value (e.g., Integer, Double, Long)
 * @param id the unique identifier of the leaderboard entry to update
 * @param value the numeric value to apply to the entry
 * 
 * @see Leaderboard#update(java.util.List) for batch update operations
 * @see UpdatePolicy for different update strategies
 */
public record EntryUpdateQuery<T extends Number>(String id, T value) {
}

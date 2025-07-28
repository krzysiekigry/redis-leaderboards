package pl.krzysiekigry.redisleaderboards;

/**
 * Represents a single entry in a leaderboard with an identifier, score, and rank.
 * <p>
 * This record encapsulates the essential information for a leaderboard entry:
 * the unique identifier of the participant, their numeric score, and their
 * current rank position in the leaderboard.
 * </p>
 * 
 * @param <T> the numeric type of the score (e.g., Integer, Double, Long)
 * @param id the unique identifier for this leaderboard entry
 * @param score the numeric score associated with this entry
 * @param rank the current rank position of this entry in the leaderboard (1-based)
 * 
 * @see Leaderboard for leaderboard operations that return Entry objects
 * @see ExportStream for streaming Entry objects in batches
 */
public record Entry<T extends Number>(String id, T score, long rank) {
}

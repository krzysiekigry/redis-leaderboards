package pl.krzysiekigry.redisleaderboards;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An iterator for streaming leaderboard entries in batches.
 * <p>
 * This class provides an efficient way to iterate through all entries in a leaderboard
 * by fetching them in configurable batch sizes. It implements the {@link Iterator} interface
 * to provide a standard iteration mechanism over lists of {@link Entry} objects.
 * </p>
 * <p>
 * The iterator maintains an internal state to track the current position and automatically
 * handles the end-of-data condition when all entries have been retrieved.
 * </p>
 * 
 * @param <T> the numeric type of the leaderboard scores
 * 
 * @see Leaderboard#exportStream(int) for creating ExportStream instances
 * @see Entry for the structure of individual leaderboard entries
 */
public class ExportStream<T extends Number> implements Iterator<List<Entry<T>>> {

    private final Leaderboard<T> leaderboard;
    private final int batchSize;

    private long currentIndex = 1;
    private boolean done = false;

    /**
     * Creates a new ExportStream for the specified leaderboard with the given batch size.
     * 
     * @param leaderboard the leaderboard to stream entries from
     * @param batchSize the number of entries to fetch in each batch
     */
    public ExportStream(Leaderboard<T> leaderboard, int batchSize) {
        this.leaderboard = leaderboard;
        this.batchSize = batchSize;
    }

    /**
     * Returns {@code true} if there are more batches of entries to retrieve.
     * 
     * @return {@code true} if more entries are available, {@code false} otherwise
     */
    @Override
    public boolean hasNext() {
        return !done;
    }

    /**
     * Retrieves the next batch of leaderboard entries.
     * <p>
     * Each call returns a list containing up to {@code batchSize} entries.
     * The last batch may contain fewer entries if the total number of entries
     * is not evenly divisible by the batch size.
     * </p>
     * 
     * @return a list of leaderboard entries in the next batch
     * @throws NoSuchElementException if no more entries are available
     * @throws RuntimeException if an error occurs while fetching entries
     */
    @Override
    public List<Entry<T>> next() {
        if (done) {
            throw new NoSuchElementException();
        }

        try {
            List<Entry<T>> entries = leaderboard.list(currentIndex, currentIndex + batchSize - 1).get();
            if (entries.size() < batchSize) {
                done = true;
                if (entries.isEmpty()) {
                    throw new NoSuchElementException();
                }
            }
            currentIndex += batchSize;
            return entries;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

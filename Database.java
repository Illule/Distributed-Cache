import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface representing the database (persistent storage layer).
 * Distributed cache will query this database on cache misses.
 */
public interface Database<K, V> {
    /**
     * Fetch a value from the database by key.
     */
    V get(K key);

    /**
     * Store a value in the database.
     */
    void put(K key, V value);

    /**
     * Remove a value from the database.
     */
    void delete(K key);
}

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory, thread-safe implementation of Database for testing.
 */
public class SimpleDatabase<K, V> implements Database<K, V> {
    private final Map<K, V> dbStorage = new ConcurrentHashMap<>();

    @Override
    public V get(K key) {
        return dbStorage.get(key);
    }

    @Override
    public void put(K key, V value) {
        dbStorage.put(key, value);
    }

    @Override
    public void delete(K key) {
        dbStorage.remove(key);
    }

    public void clear() {
        dbStorage.clear();
    }
}

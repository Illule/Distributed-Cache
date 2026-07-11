import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single cache node partition with a configured capacity and eviction policy.
 */
public class CacheNode<K, V> {
    private final String nodeId;
    private final int capacity;
    private final Map<K, V> storage;
    private final EvictionPolicy<K> evictionPolicy;

    public CacheNode(String nodeId, int capacity, EvictionPolicy<K> evictionPolicy) {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new IllegalArgumentException("Node ID cannot be null or empty.");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero.");
        }
        if (evictionPolicy == null) {
            throw new IllegalArgumentException("Eviction policy cannot be null.");
        }
        this.nodeId = nodeId;
        this.capacity = capacity;
        this.storage = new HashMap<>();
        this.evictionPolicy = evictionPolicy;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Look up a value by key. Updates key access in eviction policy.
     */
    public synchronized V get(K key) {
        if (!storage.containsKey(key)) {
            return null;
        }
        evictionPolicy.keyAccessed(key);
        return storage.get(key);
    }

    /**
     * Store a key-value pair. Handles eviction if capacity is reached.
     */
    public synchronized void put(K key, V value) {
        if (storage.containsKey(key)) {
            storage.put(key, value);
            evictionPolicy.keyAccessed(key);
            return;
        }

        // Capacity check and eviction
        if (storage.size() >= capacity) {
            K evictedKey = evictionPolicy.evictKey();
            if (evictedKey != null) {
                storage.remove(evictedKey);
                System.out.println("[Eviction] Node " + nodeId + " evicted key: " + evictedKey);
            }
        }

        storage.put(key, value);
        evictionPolicy.keyAdded(key);
    }

    /**
     * Manually remove a key from the cache node.
     */
    public synchronized void remove(K key) {
        if (storage.containsKey(key)) {
            storage.remove(key);
            evictionPolicy.keyRemoved(key);
        }
    }

    /**
     * Clear all cache data and eviction tracking on this node.
     */
    public synchronized void clear() {
        storage.clear();
        evictionPolicy.clear();
    }

    /**
     * Returns the size of the storage (number of cached keys).
     */
    public synchronized int size() {
        return storage.size();
    }

    /**
     * Read-only copy of the keys currently stored in the node (for testing/debug).
     */
    public synchronized Set<K> getKeys() {
        return Map.copyOf(storage).keySet();
    }
}

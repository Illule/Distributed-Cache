/**
 * Interface defining the contract for cache eviction policies.
 * Implementations should track access patterns and determine which key to evict when capacity is reached.
 */
public interface EvictionPolicy<K> {
    /**
     * Called when a key is read or updated.
     */
    void keyAccessed(K key);

    /**
     * Called when a key is newly added to the cache.
     */
    void keyAdded(K key);

    /**
     * Identifies the victim key to evict based on the policy, removes it from tracking, and returns it.
     * Returns null if the cache is empty.
     */
    K evictKey();

    /**
     * Called when a key is manually removed from the cache.
     */
    void keyRemoved(K key);

    /**
     * Clears all tracking state.
     */
    void clear();
}

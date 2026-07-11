import java.util.List;

/**
 * Interface defining the strategy to resolve which cache node stores a given key.
 */
public interface DistributionStrategy<K, V> {
    /**
     * Resolve the cache node for a given key from the list of available nodes.
     * 
     * @param key The key to route.
     * @param nodes The list of active CacheNodes.
     * @return The CacheNode that should handle this key.
     */
    CacheNode<K, V> selectNode(K key, List<CacheNode<K, V>> nodes);
}

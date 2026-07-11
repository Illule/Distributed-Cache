import java.util.List;

/**
 * Modulo-based distribution strategy: hash(key) % numberOfNodes.
 * Fast, simple, but nodes cannot be easily added/removed without remapping most keys.
 */
public class ModuloDistributionStrategy<K, V> implements DistributionStrategy<K, V> {

    @Override
    public CacheNode<K, V> selectNode(K key, List<CacheNode<K, V>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalStateException("No cache nodes available for distribution.");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        
        int index = Math.abs(key.hashCode()) % nodes.size();
        return nodes.get(index);
    }
}

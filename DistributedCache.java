import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrator class for the Distributed Cache.
 * Connects the clients to the partitions (CacheNodes), distribution routing strategies, and the Database.
 */
public class DistributedCache<K, V> {
    private final List<CacheNode<K, V>> nodes;
    private DistributionStrategy<K, V> distributionStrategy;
    private final Database<K, V> database;

    /**
     * @param nodes The cache nodes to distribute keys across.
     * @param distributionStrategy The distribution routing strategy to use.
     * @param database The persistent backend database.
     */
    public DistributedCache(List<CacheNode<K, V>> nodes,
                            DistributionStrategy<K, V> distributionStrategy,
                            Database<K, V> database) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Must provide at least one cache node.");
        }
        if (distributionStrategy == null) {
            throw new IllegalArgumentException("Distribution strategy cannot be null.");
        }
        if (database == null) {
            throw new IllegalArgumentException("Database cannot be null.");
        }
        this.nodes = new ArrayList<>(nodes);
        this.distributionStrategy = distributionStrategy;
        this.database = database;
    }

    /**
     * Change the distribution strategy dynamically.
     */
    public synchronized void setDistributionStrategy(DistributionStrategy<K, V> strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null.");
        }
        this.distributionStrategy = strategy;
        System.out.println("[DistributedCache] Switched distribution strategy to: " + strategy.getClass().getSimpleName());
    }

    /**
     * Fetch key from the cache. On miss, fetches from database and populates cache.
     */
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }

        CacheNode<K, V> node = distributionStrategy.selectNode(key, nodes);
        V value = node.get(key);

        if (value != null) {
            System.out.println("[Cache HIT] Key '" + key + "' found on Node " + node.getNodeId());
            return value;
        }

        System.out.println("[Cache MISS] Key '" + key + "' not found on Node " + node.getNodeId() + ". Fetching from Database...");
        value = database.get(key);
        
        if (value != null) {
            node.put(key, value);
            System.out.println("[Cache Populate] Key '" + key + "' written to Node " + node.getNodeId());
        }
        
        return value;
    }

    /**
     * Put key-value pair in both cache and database (Write-Through).
     */
    public void put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }

        // Write-through logic: write to database first
        database.put(key, value);
        
        // Resolve cache node and write to it
        CacheNode<K, V> node = distributionStrategy.selectNode(key, nodes);
        node.put(key, value);
        System.out.println("[Cache PUT] Key '" + key + "' stored in Database and Node " + node.getNodeId());
    }

    /**
     * Delete key from both cache and database.
     */
    public void delete(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }
        database.delete(key);
        CacheNode<K, V> node = distributionStrategy.selectNode(key, nodes);
        node.remove(key);
        System.out.println("[Cache DELETE] Key '" + key + "' deleted from Database and Node " + node.getNodeId());
    }

    /**
     * Clear all cache nodes.
     */
    public synchronized void clear() {
        for (CacheNode<K, V> node : nodes) {
            node.clear();
        }
        System.out.println("[Cache CLEAR] Cleared all cache nodes.");
    }

    /**
     * Get a read-only list of the cache nodes.
     */
    public List<CacheNode<K, V>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}

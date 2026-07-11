import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Consistent Hashing Distribution Strategy.
 * Maps both keys and cache nodes (via virtual nodes) to a 360-degree hash ring.
 * Minimizes the number of re-mapped keys when nodes are added or removed.
 */
public class ConsistentHashDistributionStrategy<K, V> implements DistributionStrategy<K, V> {
    private final int virtualNodesFactor;
    private final TreeMap<Integer, CacheNode<K, V>> hashRing = new TreeMap<>();
    private List<CacheNode<K, V>> lastKnownNodes = null;

    /**
     * @param virtualNodesFactor Number of virtual nodes to create per physical cache node.
     */
    public ConsistentHashDistributionStrategy(int virtualNodesFactor) {
        if (virtualNodesFactor <= 0) {
            throw new IllegalArgumentException("Virtual nodes factor must be positive.");
        }
        this.virtualNodesFactor = virtualNodesFactor;
    }

    private synchronized void rebuildRing(List<CacheNode<K, V>> nodes) {
        hashRing.clear();
        for (CacheNode<K, V> node : nodes) {
            for (int i = 0; i < virtualNodesFactor; i++) {
                String virtualNodeKey = node.getNodeId() + "#VN-" + i;
                int hash = computeHash(virtualNodeKey);
                hashRing.put(hash, node);
            }
        }
        this.lastKnownNodes = new ArrayList<>(nodes);
    }

    @Override
    public CacheNode<K, V> selectNode(K key, List<CacheNode<K, V>> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalStateException("No cache nodes available for distribution.");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null.");
        }

        // Rebuild ring if the list of nodes has changed
        synchronized (this) {
            if (lastKnownNodes == null || !lastKnownNodes.equals(nodes)) {
                rebuildRing(nodes);
            }
        }

        int keyHash = computeHash(key.toString());
        
        synchronized (this) {
            if (hashRing.isEmpty()) {
                throw new IllegalStateException("Hash ring is empty.");
            }
            
            // Find the closest node with hash >= keyHash
            SortedMap<Integer, CacheNode<K, V>> tailMap = hashRing.tailMap(keyHash);
            int nodeHash = tailMap.isEmpty() ? hashRing.firstKey() : tailMap.firstKey();
            return hashRing.get(nodeHash);
        }
    }

    /**
     * MD5 hash computation mapping string keys into a 32-bit integer space.
     */
    private int computeHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // Combine first 4 bytes to form an integer
            return ((digest[3] & 0xFF) << 24) |
                   ((digest[2] & 0xFF) << 16) |
                   ((digest[1] & 0xFF) << 8)  |
                   (digest[0] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to java's hashCode in case MD5 is unavailable
            return key.hashCode();
        }
    }
}

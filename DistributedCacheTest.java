import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Unit tests and verification suite for the Distributed Cache implementation.
 * Runs without external testing frameworks (like JUnit) using standard assertions.
 */
public class DistributedCacheTest {

    // Simple tracking database to verify database hits vs cache hits
    private static class TrackingDatabase<K, V> extends SimpleDatabase<K, V> {
        private int getCount = 0;
        private int putCount = 0;

        @Override
        public V get(K key) {
            getCount++;
            return super.get(key);
        }

        @Override
        public void put(K key, V value) {
            putCount++;
            super.put(key, value);
        }

        public int getGetCount() {
            return getCount;
        }

        public int getPutCount() {
            return putCount;
        }

        public void resetCounts() {
            getCount = 0;
            putCount = 0;
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      RUNNING DISTRIBUTED CACHE UNIT TESTS       ");
        System.out.println("==================================================");

        try {
            testCacheMissAndDatabaseFallback();
            testModuloDistribution();
            testConsistentHashingDistribution();
            testLRUEvictionPolicy();
            testDynamicStrategySwitching();
            testCapacityEdgeCases();

            System.out.println("\n==================================================");
            System.out.println("            ALL TEST CASES PASSED!               ");
            System.out.println("==================================================");
        } catch (Throwable t) {
            System.err.println("\n==================================================");
            System.err.println("                 TEST FAILED!                     ");
            System.err.println("==================================================");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " - Expected: [" + expected + "], Got: [" + actual + "]");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    /**
     * Test Case 1: Cache Miss handles data load from database and populates cache.
     */
    private static void testCacheMissAndDatabaseFallback() {
        System.out.println("\n[Test 1] Testing Cache Miss and Database Fallback...");

        TrackingDatabase<String, String> db = new TrackingDatabase<>();
        db.put("key1", "value1");
        db.put("key2", "value2");

        // Initialize 3 cache nodes with capacity 5 and LRU eviction
        List<CacheNode<String, String>> nodes = List.of(
            new CacheNode<>("Node_0", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_1", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_2", 5, new LRUEvictionPolicy<>())
        );

        DistributedCache<String, String> cache = new DistributedCache<>(
            nodes,
            new ModuloDistributionStrategy<>(),
            db
        );

        db.resetCounts();

        // 1. Fetch key1 (Cache Miss)
        String v1 = cache.get("key1");
        assertEquals("value1", v1, "First fetch should return correct value.");
        assertEquals(1, db.getGetCount(), "Database should be queried exactly once on miss.");

        // 2. Fetch key1 again (Cache Hit)
        String v1_again = cache.get("key1");
        assertEquals("value1", v1_again, "Subsequent fetch should return correct value.");
        assertEquals(1, db.getGetCount(), "Database should not be queried again (Cache Hit).");

        // 3. Put key3 (Write-Through)
        cache.put("key3", "value3");
        assertEquals(1, db.getPutCount(), "Database should be updated on PUT (Write-through).");
        
        // Fetch key3 (Cache Hit, no DB query)
        db.resetCounts();
        String v3 = cache.get("key3");
        assertEquals("value3", v3, "Fetched value from put should match.");
        assertEquals(0, db.getGetCount(), "Database should not be queried for key3.");
    }

    /**
     * Test Case 2: Modulo-based distribution routing.
     */
    private static void testModuloDistribution() {
        System.out.println("\n[Test 2] Testing Modulo-Based Distribution Strategy...");

        TrackingDatabase<String, String> db = new TrackingDatabase<>();
        List<CacheNode<String, String>> nodes = List.of(
            new CacheNode<>("Node_0", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_1", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_2", 5, new LRUEvictionPolicy<>())
        );

        ModuloDistributionStrategy<String, String> strategy = new ModuloDistributionStrategy<>();
        DistributedCache<String, String> cache = new DistributedCache<>(nodes, strategy, db);

        // Put keys and inspect where they are routed
        String[] keys = {"apple", "banana", "cherry", "date", "fig", "grape"};
        for (String key : keys) {
            cache.put(key, key + "_value");
            
            // Expected node index calculation
            int expectedIndex = Math.abs(key.hashCode()) % nodes.size();
            CacheNode<String, String> targetNode = nodes.get(expectedIndex);
            
            // Verify the value was actually stored on the expected node
            assertEquals(key + "_value", targetNode.get(key), 
                "Key '" + key + "' should reside in node " + targetNode.getNodeId());
        }
    }

    /**
     * Test Case 3: Consistent Hashing distribution routing.
     */
    private static void testConsistentHashingDistribution() {
        System.out.println("\n[Test 3] Testing Consistent Hashing Distribution Strategy...");

        TrackingDatabase<String, String> db = new TrackingDatabase<>();
        List<CacheNode<String, String>> nodes = List.of(
            new CacheNode<>("Node_0", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_1", 5, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_2", 5, new LRUEvictionPolicy<>())
        );

        // 3 virtual nodes per physical node
        ConsistentHashDistributionStrategy<String, String> strategy = new ConsistentHashDistributionStrategy<>(3);
        DistributedCache<String, String> cache = new DistributedCache<>(nodes, strategy, db);

        // Store keys
        String[] keys = {"java", "cpp", "python", "javascript", "golang", "rust", "ruby", "swift"};
        for (String key : keys) {
            cache.put(key, key + "_lang");
        }

        // Verify keys exist in at least one of the nodes
        for (String key : keys) {
            boolean found = false;
            for (CacheNode<String, String> node : nodes) {
                if (node.getKeys().contains(key)) {
                    found = true;
                    assertEquals(key + "_lang", node.get(key), "Value should match.");
                    break;
                }
            }
            assertTrue(found, "Key '" + key + "' should be found in some CacheNode.");
        }
    }

    /**
     * Test Case 4: LRU Eviction Policy.
     */
    private static void testLRUEvictionPolicy() {
        System.out.println("\n[Test 4] Testing LRU Cache Eviction Policy...");

        // We use capacity 3 for our node
        LRUEvictionPolicy<String> policy = new LRUEvictionPolicy<>();
        CacheNode<String, String> node = new CacheNode<>("Node_0", 3, policy);

        // Insert 3 items (capacity reached)
        node.put("A", "Apple");
        node.put("B", "Banana");
        node.put("C", "Cherry");

        assertEquals(3, node.size(), "Node size should be 3.");
        assertTrue(node.getKeys().containsAll(Set.of("A", "B", "C")), "Node should contain keys A, B, C.");

        // Access "A" (makes A the most recently used)
        node.get("A"); // Order now should make B the least recently used: B -> C -> A

        // Insert "D" (should trigger eviction of B)
        node.put("D", "Date");

        assertEquals(3, node.size(), "Node size should remain 3.");
        assertTrue(!node.getKeys().contains("B"), "Least recently used key 'B' should have been evicted.");
        assertTrue(node.getKeys().containsAll(Set.of("A", "C", "D")), "Cache should contain A, C, D.");

        // Access "C" (Order becomes: A -> D -> C)
        node.get("C");

        // Insert "E" (should trigger eviction of A)
        node.put("E", "Elderberry");

        assertTrue(!node.getKeys().contains("A"), "Least recently used key 'A' should have been evicted.");
        assertTrue(node.getKeys().containsAll(Set.of("C", "D", "E")), "Cache should contain C, D, E.");
    }

    /**
     * Test Case 5: Pluggable Strategy dynamic switching.
     */
    private static void testDynamicStrategySwitching() {
        System.out.println("\n[Test 5] Testing Pluggable Distribution Strategy Switching...");

        TrackingDatabase<String, String> db = new TrackingDatabase<>();
        List<CacheNode<String, String>> nodes = List.of(
            new CacheNode<>("Node_0", 10, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_1", 10, new LRUEvictionPolicy<>()),
            new CacheNode<>("Node_2", 10, new LRUEvictionPolicy<>())
        );

        DistributedCache<String, String> cache = new DistributedCache<>(
            nodes,
            new ModuloDistributionStrategy<>(),
            db
        );

        cache.put("user1", "Alice");
        cache.put("user2", "Bob");

        // Switch strategy dynamically to Consistent Hashing
        cache.setDistributionStrategy(new ConsistentHashDistributionStrategy<>(5));

        // Attempt to fetch. If keys were not found on the new mapped nodes,
        // it triggers a cache miss, fetches from DB, and populates the new mapped nodes.
        db.resetCounts();
        String u1 = cache.get("user1");
        assertEquals("Alice", u1, "Should correctly retrieve value after strategy switch.");
    }

    /**
     * Test Case 6: Edge Cases.
     */
    private static void testCapacityEdgeCases() {
        System.out.println("\n[Test 6] Testing Capacity Edge Cases...");

        // Edge case: Node with capacity 1
        LRUEvictionPolicy<String> policy = new LRUEvictionPolicy<>();
        CacheNode<String, String> node = new CacheNode<>("Single_Capacity", 1, policy);

        node.put("k1", "v1");
        assertEquals("v1", node.get("k1"), "Should store first item.");

        node.put("k2", "v2");
        assertEquals("v2", node.get("k2"), "Should store second item.");
        assertEquals(null, node.get("k1"), "First item should be evicted since capacity is 1.");
    }
}

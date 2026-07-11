import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of EvictionPolicy using Least Recently Used (LRU) algorithm.
 * Implements a custom doubly linked list and hash map for O(1) performance.
 */
public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

    private static class Node<K> {
        K key;
        Node<K> prev;
        Node<K> next;

        Node(K key) {
            this.key = key;
        }
    }

    private final Map<K, Node<K>> map = new HashMap<>();
    private final Node<K> head;
    private final Node<K> tail;

    public LRUEvictionPolicy() {
        // Dummy head and tail to simplify boundary operations
        this.head = new Node<>(null);
        this.tail = new Node<>(null);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    @Override
    public synchronized void keyAccessed(K key) {
        Node<K> node = map.get(key);
        if (node != null) {
            removeNode(node);
            addToTail(node);
        }
    }

    @Override
    public synchronized void keyAdded(K key) {
        if (map.containsKey(key)) {
            keyAccessed(key);
        } else {
            Node<K> node = new Node<>(key);
            map.put(key, node);
            addToTail(node);
        }
    }

    @Override
    public synchronized K evictKey() {
        if (head.next == tail) {
            return null; // Cache eviction policy is empty
        }
        Node<K> lruNode = head.next;
        removeNode(lruNode);
        map.remove(lruNode.key);
        return lruNode.key;
    }

    @Override
    public synchronized void keyRemoved(K key) {
        Node<K> node = map.remove(key);
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public synchronized void clear() {
        map.clear();
        head.next = tail;
        tail.prev = head;
    }

    // Helper: remove node from doubly linked list
    private void removeNode(Node<K> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Helper: add node to tail (most recently used end)
    private void addToTail(Node<K> node) {
        Node<K> last = tail.prev;
        last.next = node;
        node.prev = last;
        node.next = tail;
        tail.prev = node;
    }
}

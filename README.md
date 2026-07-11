# Distributed Cache System

A clean, thread-safe, and highly extensible Low-Level Design (LLD) implementation of a **Distributed Cache System** in Java. 

This system supports pluggable data distribution strategies across multiple cache nodes, pluggable cache eviction policies (with a custom $O(1)$ LRU implementation), and a write-through database fallback mechanism.

---

## Core Architecture & Design Details

### 1. How Data is Distributed Across Nodes

Data is mapped to a specific `CacheNode` using a **`DistributionStrategy`**. The system supports multiple pluggable strategies:

- **Modulo-Based Distribution (`ModuloDistributionStrategy`)**:
  - Calculates the hash code of the key and maps it to a node using `Math.abs(hash(key)) % numberOfNodes`.
  - Simple and very fast ($O(1)$ routing).
  - *Drawback*: Adding or removing cache nodes changes the modulo factor, which invalidates/remaps almost all keys, triggering a massive wave of cache misses.
- **Consistent Hashing (`ConsistentHashDistributionStrategy`)**:
  - Maps both the keys and the cache nodes (via multiple **Virtual Nodes** to ensure uniform load distribution) to a $360^\circ$ circular hash ring.
  - When looking up or inserting a key, the system hashes the key and moves clockwise along the ring to find the first node hash that is $\ge$ the key's hash.
  - *Advantage*: Adding or removing a physical node only shifts a fraction of the keys (approximately $1/N$ where $N$ is the number of nodes), keeping the rest of the cache warm.

---

### 2. How Cache Misses are Handled

The `DistributedCache` class acts as the client-facing orchestrator. The cache miss flow operates as follows:

```mermaid
sequenceDiagram
    autonumber
    Client->>DistributedCache: get(key)
    DistributedCache->>DistributionStrategy: selectNode(key, nodes)
    DistributionStrategy-->>DistributedCache: return CacheNode
    DistributedCache->>CacheNode: get(key)
    alt Cache Hit
        CacheNode-->>DistributedCache: return value
        DistributedCache-->>Client: return value
    else Cache Miss
        CacheNode-->>DistributedCache: return null
        DistributedCache->>Database: get(key)
        Database-->>DistributedCache: return value
        alt Value exists in DB
            DistributedCache->>CacheNode: put(key, value)
            Note over CacheNode: Cache node stores key and updates eviction policy
        end
        DistributedCache-->>Client: return value
    }
```

1. The client requests a key via `DistributedCache.get(key)`.
2. The orchestrator uses the configured `DistributionStrategy` to determine which physical `CacheNode` owns the key.
3. The orchestrator queries the selected `CacheNode`.
4. **Cache Hit**: If the key is present, the node updates its eviction tracker (e.g. marking it as recently accessed) and returns the value.
5. **Cache Miss**: If the key is absent:
   - The orchestrator fetches the data from the persistent `Database`.
   - If found, the orchestrator populates the resolved `CacheNode` via `CacheNode.put(key, value)`.
   - The value is returned to the client.

---

### 3. How Eviction Works (LRU Policy)

Each `CacheNode` has a configured maximum capacity. When a node reaches its capacity and a new key needs to be inserted, it evicts a key according to its **`EvictionPolicy`**.

Our default implementation is the **Least Recently Used (LRU) policy (`LRUEvictionPolicy`)**:
- Uses a **Custom Doubly Linked List** combined with a **HashMap** to achieve $O(1)$ operations for lookup, access updates, and eviction.
- **Node Ordering**:
  - The head of the list represents the *Least Recently Used (LRU)* elements.
  - The tail of the list represents the *Most Recently Used (MRU)* elements.
- **Accessing a Key (`keyAccessed`)**:
  - The corresponding node is detached from its current position in the doubly linked list and moved to the tail.
- **Adding a Key (`keyAdded`)**:
  - A new node is appended to the tail.
- **Evicting a Key (`evictKey`)**:
  - The node immediately following the dummy `head` (`head.next`) is removed from both the linked list and the tracking map. The key is returned so the `CacheNode` can delete the entry from its main storage.

---

### 4. How the Design Supports Future Extensibility

The design follows SOLID principles to make additions simple and clean:

- **New Eviction Policies**:
  - To add policies like **LFU** (Least Frequently Used) or **MRU** (Most Recently Used), create a new class implementing the `EvictionPolicy` interface. 
  - Each `CacheNode` accepts an `EvictionPolicy` instance at construction, meaning different nodes could even run different eviction policies if needed.
- **New Distribution Strategies**:
  - To add custom strategies (e.g., location-aware routing, weighted hashing), create a new class implementing the `DistributionStrategy` interface.
  - The orchestrator `DistributedCache` allows changing the strategy dynamically at runtime using `setDistributionStrategy(...)`.
- **Database Adaptation**:
  - The `Database` interface decouples the cache from any specific database technology (SQL, NoSQL, etc.). Implement the `Database` interface to connect to any actual database backend.

---

## File Structure

- [Database.java](file:///c:/Users/arman/Desktop/DistributedCache/Database.java): Interface for persistent database backend.
- [SimpleDatabase.java](file:///c:/Users/arman/Desktop/DistributedCache/SimpleDatabase.java): In-memory database implementation.
- [EvictionPolicy.java](file:///c:/Users/arman/Desktop/DistributedCache/EvictionPolicy.java): Interface for cache eviction algorithms.
- [LRUEvictionPolicy.java](file:///c:/Users/arman/Desktop/DistributedCache/LRUEvictionPolicy.java): O(1) LRU eviction policy using a custom doubly linked list.
- [DistributionStrategy.java](file:///c:/Users/arman/Desktop/DistributedCache/DistributionStrategy.java): Interface for key routing/distribution.
- [ModuloDistributionStrategy.java](file:///c:/Users/arman/Desktop/DistributedCache/ModuloDistributionStrategy.java): Modulo-based key distribution strategy.
- [ConsistentHashDistributionStrategy.java](file:///c:/Users/arman/Desktop/DistributedCache/ConsistentHashDistributionStrategy.java): Consistent Hashing key distribution strategy with virtual nodes.
- [CacheNode.java](file:///c:/Users/arman/Desktop/DistributedCache/CacheNode.java): Class representing an individual storage node/partition.
- [DistributedCache.java](file:///c:/Users/arman/Desktop/DistributedCache/DistributedCache.java): Main client orchestrator coordinating nodes, strategy, and database.
- [Cache.java](file:///c:/Users/arman/Desktop/DistributedCache/Cache.java): Main convenient executable launcher.
- [DistributedCacheTest.java](file:///c:/Users/arman/Desktop/DistributedCache/DistributedCacheTest.java): Thorough test suite verifying routing, cache hits/misses, and eviction.
- [DistributedCache_UML.md](file:///c:/Users/arman/Desktop/DistributedCache/DistributedCache_UML.md): UML Class Diagram using Mermaid.js syntax.

---

## Getting Started

### Prerequisites

- Java SE Development Kit (JDK) version 11 or higher.

### Compilation

Compile all Java sources using standard `javac`:

```bash
javac *.java
```

### Running Tests and Verification

Run the test suite using:

```bash
java Cache
```

This will run all 6 test scenarios checking:
1. Cache misses & database fallbacks
2. Modulo-based distribution routing correctness
3. Consistent hashing distribution with virtual nodes
4. LRU eviction correctness (verifying correct eviction sequence)
5. Pluggable distribution strategies switching dynamically
6. Node capacity edge cases

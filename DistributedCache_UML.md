# Distributed Cache System UML Class Diagram

This document contains the UML Class Diagram for the Distributed Cache system, showcasing the pluggable eviction policies and distribution strategies.

```mermaid
classDiagram
    direction TB

    %% Interfaces
    class Database~K, V~ {
        <<interface>>
        +get(K key) V
        +put(K key, V value) void
        +delete(K key) void
    }

    class EvictionPolicy~K~ {
        <<interface>>
        +keyAccessed(K key) void
        +keyAdded(K key) void
        +evictKey() K
        +keyRemoved(K key) void
        +clear() void
    }

    class DistributionStrategy~K, V~ {
        <<interface>>
        +selectNode(K key, List~CacheNode~K,V~~ nodes) CacheNode~K,V~
    }

    %% Concrete Implementations
    class SimpleDatabase~K, V~ {
        -Map~K, V~ dbStorage
        +get(K key) V
        +put(K key, V value) void
        +delete(K key) void
        +clear() void
    }

    class LRUEvictionPolicy~K~ {
        -Map~K, Node~K~~ map
        -Node~K~ head
        -Node~K~ tail
        +keyAccessed(K key) void
        +keyAdded(K key) void
        +evictKey() K
        +keyRemoved(K key) void
        +clear() void
        -removeNode(Node~K~ node) void
        -addToTail(Node~K~ node) void
    }

    class ModuloDistributionStrategy~K, V~ {
        +selectNode(K key, List~CacheNode~K,V~~ nodes) CacheNode~K,V~
    }

    class ConsistentHashDistributionStrategy~K, V~ {
        -int virtualNodesFactor
        -TreeMap~Integer, CacheNode~K,V~~ hashRing
        -List~CacheNode~K,V~~ lastKnownNodes
        +selectNode(K key, List~CacheNode~K,V~~ nodes) CacheNode~K,V~
        -rebuildRing(List~CacheNode~K,V~~ nodes) void
        -computeHash(String key) int
    }

    %% Core Components
    class CacheNode~K, V~ {
        -String nodeId
        -int capacity
        -Map~K, V~ storage
        -EvictionPolicy~K~ evictionPolicy
        +CacheNode(String nodeId, int capacity, EvictionPolicy~K~ evictionPolicy)
        +getNodeId() String
        +getCapacity() int
        +get(K key) V
        +put(K key, V value) void
        +remove(K key) void
        +clear() void
        +size() int
        +getKeys() Set~K~
    }

    class DistributedCache~K, V~ {
        -List~CacheNode~K,V~~ nodes
        -DistributionStrategy~K, V~ distributionStrategy
        -Database~K, V~ database
        +DistributedCache(List~CacheNode~K,V~~ nodes, DistributionStrategy~K,V~ strategy, Database~K,V~ db)
        +setDistributionStrategy(DistributionStrategy~K,V~ strategy) void
        +get(K key) V
        +put(K key, V value) void
        +delete(K key) void
        +clear() void
        +getNodes() List~CacheNode~K,V~~
    }

    %% Realizations (Implementation Relationships)
    SimpleDatabase ..|> Database : implements
    LRUEvictionPolicy ..|> EvictionPolicy : implements
    ModuloDistributionStrategy ..|> DistributionStrategy : implements
    ConsistentHashDistributionStrategy ..|> DistributionStrategy : implements

    %% Associations (Usage and Ownership)
    CacheNode --> EvictionPolicy : uses
    DistributedCache --> DistributionStrategy : uses
    DistributedCache --> Database : uses
    DistributedCache "1" *--> "*" CacheNode : contains
```

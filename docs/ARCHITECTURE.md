# Platform Architecture & Engineering Design

## 1. High-Level System Architecture

```mermaid
flowchart TD
    subgraph Client ["Android Client (Offline-First)"]
        UI["UI Layer\n(Activities & ViewModels)"]
        Room["Room Database\n(Local SQLite)"]
        NetworkMonitor["Network Status Monitor\n(ConnectivityManager)"]
        WorkManager["WorkManager SyncWorker\n(Constraint: CONNECTED)"]
        CRDT_Client["CRDT Engine\n(Lamport Clock & Sequence Merge)"]

        UI <-->|Flow / StateFlow| Room
        UI -->|Generate Mutations| CRDT_Client
        CRDT_Client -->|Persist Ops (isSynced=0)| Room
        NetworkMonitor -->|Network Available Event| WorkManager
        WorkManager -->|Flush Outbox Queue| SyncApi["Sync REST API"]
    end

    subgraph Server ["Spring Boot Backend Server"]
        SyncApi --> SyncService["CRDT Sync Engine"]
        CourseApi["Course & Lesson Catalog API"]
        WebSocket["WebSocket / STOMP Broker"]
        ServerSQLite["Authoritative SQLite Database\n(courses, lessons, notes, crdt_operations)"]

        SyncService <--> ServerSQLite
        CourseApi <--> ServerSQLite
        SyncService -->|Broadcast Updates| WebSocket
    end

    SyncApi <-->|HTTP POST /api/sync/delta| SyncService
```

---

## 2. Architectural Pillars

### Pillar 1: Local SQLite as the Single Source of Truth (SSOT)
- In traditional online apps, the UI depends on network latency for data rendering.
- In our **Offline-First Platform**, the Android application **only** reads from and writes to the local Room SQLite database.
- Read operations return reactive `Flow<List<T>>` or `LiveData<T>`.
- Write operations append immutable mutations to the local outbox queue (`crdt_operations` table) and update the local canonical tables immediately.
- The user interface operates at 60/120 FPS with zero network spinner delay.

### Pillar 2: Resilient Background Synchronization via Jetpack WorkManager
- The device may reconnect while the application is in the background or device is locked.
- `WorkManager` guarantees execution under defined system constraints:
  ```kotlin
  val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()
  ```
- If the network drops mid-sync, WorkManager automatically applies exponential backoff retry.

### Pillar 3: Real-Time Network State Observation
- `NetworkStatusMonitor` utilizes Android's `ConnectivityManager.registerNetworkCallback`.
- Provides an immediate transition event when WiFi or Cellular connection becomes active.
- Automatically triggers `SyncScheduler.triggerImmediateSync()` so students don't need to manually press "Sync".

### Pillar 4: Pure Shared CRDT Core Library
- The mathematical convergence logic is encapsulated inside `crdt-core`.
- Both Android client and Spring Boot server use the exact same state machine.
- 100% reproducible convergence verified by automated property tests.

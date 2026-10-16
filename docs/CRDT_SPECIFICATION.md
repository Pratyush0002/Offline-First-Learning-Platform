# Conflict-Free Replicated Data Type (CRDT) Synchronization Specification

## 1. Problem Statement
In an **offline-first learning platform**, students in remote or low-connectivity regions face intermittent network availability.
When a student switches between devices (e.g. tablet at school and phone at home) while offline, both devices modify their local SQLite databases independently.

Standard centralized approaches fail:
- **Last-Write-Wins (LWW) at Document Level**: Causes complete data loss of whichever device synced first.
- **Git-style 3-Way Merge Conflicts**: Requires manual resolution, creating unacceptable friction for students.

## 2. Mathematical Foundation: State-based & Op-based CRDTs
A Conflict-Free Replicated Data Type is defined on a join semi-lattice \((S, \sqcup)\) equipped with a partial order \(\le\) such that:
1. **Commutativity**: \(\forall A, B \in S, \quad A \sqcup B = B \sqcup A\)
   The order in which operations or states arrive does not alter the converged outcome.
2. **Associativity**: \(\forall A, B, C \in S, \quad (A \sqcup B) \sqcup C = A \sqcup (B \sqcup C)\)
   Batching and grouping of updates over network partitions does not affect convergence.
3. **Idempotence**: \(\forall A \in S, \quad A \sqcup A = A\)
   Duplicate network packets or repeated retry attempts produce identical state without side effects.

---

## 3. Component CRDTs in the Platform

### 3.1 Logical Clocks & Version Vectors
- **Lamport Clock**: A monotonically increasing integer scalar logical clock:
  \[
  C_{local} \leftarrow \max(C_{local}, C_{received}) + 1
  \]
- **Version Vector**: A vector mapping client IDs to their highest observed Lamport clock:
  \[
  V: \text{ClientId} \to \mathbb{N}
  \]
  Enables nodes to determine causality:
  - \(V_1 < V_2\) if \(\forall c, V_1[c] \le V_2[c]\) and \(\exists c, V_1[c] < V_2[c]\) (Happened-Before).
  - \(V_1 \parallel V_2\) otherwise (Concurrent offline edits).

### 3.2 Sequence CRDT with Deterministic Fractional Indexing
Each note consists of an ordered sequence of content blocks (paragraphs, code snippets, checklist items).

- **Position Key**: A variable-length lexicographical string \(P \in \Sigma^*\).
- To insert a block between position \(P_{prev}\) and \(P_{next}\):
  \[
  P_{new} = \text{generateKeyBetween}(P_{prev}, P_{next})
  \]
- **Deterministic Tie-Breaker**:
  If two devices concurrently insert between the exact same blocks:
  \[
  \text{sortKey} = (P_{new}, \text{clientId}, \text{lamportClock})
  \]
  Because \(\text{clientId}\) and \(\text{lamportClock}\) are unique, the ordering is totally ordered and identical across all replicas worldwide!

### 3.3 LWW-Registers for Metadata & Titles
For note titles:
\[
Title_{converged} = \begin{cases} 
Title_A & \text{if } Clock_A > Clock_B \lor (Clock_A = Clock_B \land Client_A > Client_B) \\
Title_B & \text{otherwise}
\end{cases}
\]

### 3.4 Observed-Remove Set (OR-Set) for Categorization Tags
Tags are stored as a map of tag names to add and remove Lamport timestamps:
\[
\text{TagActive}(t) \iff \text{addedClock}(t) > \text{removedClock}(t)
\]
Concurrent additions and removals from separate devices resolve cleanly: an addition with a higher clock revives a removed tag, while a deletion with a higher clock removes it.

### 3.5 Monotonic CRDT for Lesson Progress
- `completed`: Grow-Only Boolean (\(C_{merged} = C_A \lor C_B\)).
- `progressPercentage`: Monotonic Maximum (\(P_{merged} = \max(P_A, P_B)\)).
- `quizScore`: Highest Score (\(S_{merged} = \max(S_A, S_B)\)).

---

## 4. The Delta-State Synchronization Handshake

1. **Client Disconnect**:
   The student continues writing notes. Every keystroke/block insertion creates an immutable `CrdtOperation`:
   \[
   Op = (\text{opId}, \text{noteId}, \text{type}, \text{clientId}, \text{lamportClock}, \text{timestamp}, \text{payload})
   \]
   Stored locally in Android Room SQLite (`crdt_operations` table where `isSynced = 0`).

2. **Network Restoration**:
   Android's `ConnectivityManager.NetworkCallback` detects an active internet connection and triggers WorkManager's `SyncWorker`.

3. **Handshake Protocol**:
   - **Client -> Server (`POST /api/sync/delta`)**:
     Sends its `clientId`, local `VersionVector`, and list of pending `operations`.
   - **Server Processing**:
     - Ingests incoming operations into server SQLite database.
     - Rebuilds converged `CrdtNote` snapshot.
     - Computes missing operations that the client has not yet seen:
       \[
       \text{Missing} = \{ op \in \text{ServerLog} \mid op.\text{lamportClock} > \text{clientVector}[op.\text{clientId}] \}
       \]
   - **Server -> Client Response**:
     Sends acknowledged `opId` list, `missingOperations`, and current `serverVector`.
   - **Client Convergence**:
     - Marks local operations as `isSynced = 1`.
     - Applies `missingOperations` to local Room database using `CrdtEngine`.
     - Local SQLite database and UI converge instantly.

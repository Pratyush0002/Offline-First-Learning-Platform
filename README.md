# Offline-First Learning Platform

> **A resilient offline-first educational platform enabling students with unreliable internet access to download lessons, take rich study notes offline, and synchronize progress seamlessly with zero merge conflicts using Conflict-Free Replicated Data Types (CRDTs).**

---

## 🎯 The Problem
Students in remote, rural, or underserved regions often have intermittent or zero internet access. Traditional online learning platforms require constant network connectivity, locking students out when connectivity fails. Furthermore, when students study across multiple devices (e.g. an Android phone on the bus and a tablet at home) while offline, standard cloud synchronization either causes silent data loss (Last-Write-Wins overwriting other device's notes) or painful Git-style merge conflict errors.

## 💡 The Solution
1. **100% Offline Capability**: Students download entire courses, modular lessons, markdown readings, and self-assessment quizzes to their local device SQLite database.
2. **Local Single Source of Truth**: The Android client writes directly to local Room SQLite; UI updates with zero network latency.
3. **Automatic Background Sync**: Android Jetpack `WorkManager` monitors connectivity via `NetworkCallback` and flushes queued mutations when the internet returns.
4. **Unique Feature: Conflict-Free Multi-Device Synchronization (CRDT)**:
   - Built on a mathematically proven state-based and operation-based CRDT engine.
   - Combines **Fractional Indexing** (Sequence CRDT) for block/paragraph ordering, **LWW-Registers** for titles, and **OR-Sets (Observed-Remove Sets)** for study tags.
   - When the same note is edited on multiple devices simultaneously while offline, both devices' additions, edits, and tags merge deterministically upon reconnection with **0% data loss**.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21 / 25, Spring Boot 3.3.4, Spring Data JPA, Spring WebSocket (STOMP) |
| **Server Database** | SQLite (via `org.xerial:sqlite-jdbc` and Hibernate 6 SQLite Community Dialect) |
| **Client (Android)** | Kotlin, Android Jetpack, Room Database (SQLite), WorkManager, Retrofit 2, OkHttp 3, Coroutines & StateFlow, Material Design 3 |
| **CRDT Core** | Pure Java/Kotlin Conflict-Free Engine (Lamport Clocks, Version Vectors, Fractional Indexing, Block CRDT, LWW-Registers) |
| **Testing** | JUnit 5, Spring Boot Test, MockMvc, Coroutine Test |

---

## 📂 Repository Structure

```
d:\Studenthelper\
├── pom.xml                               # Root Maven multi-module configuration
├── crdt-core\                            # Shared pure CRDT library
│   ├── pom.xml
│   └── src\
│       ├── main\java\com\studenthelper\crdt\
│       │   ├── clock\                    # LamportClock, VersionVector
│       │   ├── engine\                   # CrdtEngine (pure convergence algorithms)
│       │   ├── model\                    # FractionalIndex, CrdtBlock, CrdtNote, CrdtProgress
│       │   └── operation\                # CrdtOperation, OperationType
│       └── test\java\com\studenthelper\crdt\
│           └── CrdtConvergenceTest.java # Tests proving mathematical convergence
├── server\                               # Spring Boot Backend with SQLite
│   ├── pom.xml
│   ├── src\main\
│   │   ├── java\com\studenthelper\server\
│   │   │   ├── config\                   # WebSocketConfig, DatabaseConfig
│   │   │   ├── controller\               # CourseController, SyncController, NoteController, SimulatorController
│   │   │   ├── dto\                      # SyncRequest, SyncResponse, LessonPackageDto
│   │   │   ├── entity\                   # CourseEntity, LessonEntity, NoteEntity, CrdtOperationEntity
│   │   │   ├── repository\               # Spring Data JPA SQLite Repositories
│   │   │   └── service\                  # SyncService, CourseService, NoteService, DataSeedService
│   │   └── resources\
│   │       ├── application.properties    # SQLite JDBC configuration
│   │       └── static\
│   │           └── index.html            # Interactive Web UI & Multi-Device Conflict Simulator
│   └── src\test\                         # Integration test suite
├── android\                              # Android Studio Project
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── app\
│   │   ├── build.gradle.kts              # Room, WorkManager, Retrofit, Kotlin Coroutines
│   │   └── src\main\
│   │       ├── AndroidManifest.xml
│   │       ├── java\com\studenthelper\android\
│   │       │   ├── StudentHelperApp.kt   # App lifecycle, Room DB initialization, Network monitoring
│   │       │   ├── crdt\                 # Shared CRDT engine models in Kotlin
│   │       │   ├── data\local\           # Room AppDatabase, DAOs, and Entities
│   │       │   ├── data\remote\          # Retrofit ApiClient and Network DTOs
│   │       │   ├── data\repository\      # CourseRepository, NoteRepository, SyncRepository
│   │       │   ├── network\              # NetworkStatusMonitor (ConnectivityManager)
│   │       │   ├── sync\                 # WorkManager SyncWorker and SyncScheduler
│   │       │   └── ui\                   # MainActivity, LessonDetailActivity, NoteEditorActivity
│   │       └── res\                      # Layouts, themes, colors, and drawables
└── docs\                                 # Architectural & Technical documentation
    ├── ARCHITECTURE.md                   # Offline-First system design
    ├── CRDT_SPECIFICATION.md             # Formal mathematical specification of CRDT
    └── API_DOCUMENTATION.md              # REST & delta sync protocol endpoints
```

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 17, 21, or 25** (`java -version`)
- **Apache Maven 3.8+** (`mvn -v`)
- **Android Studio** (for building/running the Android client app)

### 2. Building & Running the Backend Server
From the root directory (`d:\Studenthelper`):

```powershell
# 1. Run full test suite (crdt-core tests + server SQLite integration tests)
mvn clean test

# 2. Start the Spring Boot backend server with SQLite
mvn --projects server spring-boot:run
```

Once running:
- **Server URL**: `http://localhost:8080`
- **Interactive Multi-Device Simulation Dashboard**: Open `http://localhost:8080` in any web browser.
- **SQLite Database File**: Automatically created at `d:\Studenthelper\server\learning_platform.db`.

---

## 🧪 Testing the Unique Feature: Multi-Device Conflict Simulator

Open `http://localhost:8080` in your web browser:
1. You will see **Device 1 (Student Phone)** and **Device 2 (Student Tablet)** side-by-side.
2. Click the **"Run Automated Conflict & Merge Simulation"** button, or:
   - Click **"Online"** toggle on Device 1 to simulate disconnecting from WiFi (turns red **Offline**).
   - Click **"Online"** toggle on Device 2 to simulate disconnecting from WiFi.
   - On Device 1 (Phone): Type a study note on BFS algorithm, add tag `#BFS`, and click **Save Local Edit** (saves into SQLite outbox).
   - On Device 2 (Tablet): Type a study note on DFS algorithm, update title, add tag `#DFS`, and click **Save Local Edit**.
   - Reconnect both devices to the network.
   - Click **Synchronize Now**.
3. **The Result**: Observe the converged note state in real-time:
   - Both paragraphs appear in deterministic order.
   - Both tags (`#BFS`, `#DFS`) are preserved.
   - The updated title is reconciled cleanly.
   - **Zero merge conflicts and zero lost data!**

---

## 📱 Running the Android Client App

1. Open **Android Studio**.
2. Select **Open an Existing Project** and navigate to `d:\Studenthelper\android`.
3. Allow Gradle to sync dependencies (Room, WorkManager, Retrofit, Material).
4. Run the app on an Android Emulator or connected physical device.
   - *Note*: If testing with Android Emulator, the app connects to the Spring Boot server via `http://10.0.2.2:8080/`. If running on a physical Android device on the same local network, update `baseUrl` in `ApiClient.kt` to your computer's local IP address (e.g., `http://192.168.1.X:8080/`).

### Android Offline Features to Test:
1. **Browse Courses**: View pre-seeded courses on Data Structures & Algorithms, Distributed Systems, and Android.
2. **Download for Offline**: Tap on any lesson and click **"Download for Offline"**. The full markdown content and quizzes are stored locally in Room SQLite.
3. **Toggle Airplane Mode**: Put your Android device into Airplane Mode:
   - Notice the top status bar changes to **"Offline Mode • Local Storage Active"** (Orange indicator).
   - Open downloaded lessons and read them completely offline.
   - Take the offline self-assessment quiz.
   - Click **"Study Notes"** and create or edit notes offline: the outbox indicator shows `⚡ 1 pending op` queued in Room.
4. **Turn Airplane Mode Off**:
   - As soon as internet is restored, `NetworkStatusMonitor` detects the connection.
   - Jetpack `WorkManager` triggers `SyncWorker` in the background.
   - The pending operations are pushed to Spring Boot, and the indicator updates to **"Connected • Online (All synced)"**.

---

## 📖 In-Depth Documentation
- [System Architecture](docs/ARCHITECTURE.md)
- [CRDT Mathematical Specification](docs/CRDT_SPECIFICATION.md)
- [REST API & Sync Protocol](docs/API_DOCUMENTATION.md)

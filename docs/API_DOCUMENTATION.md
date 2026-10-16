# REST API & Synchronization Protocol Documentation

Base URL: `http://localhost:8080/api` (Web / Server) or `http://10.0.2.2:8080/api` (Android Emulator)

---

## 1. Courses & Lessons

### 1.1 List All Courses
- **Endpoint**: `GET /api/courses`
- **Response**: `200 OK`
```json
[
  {
    "id": "course-dsa-101",
    "title": "Data Structures & Algorithms",
    "description": "Master core data structures and algorithms...",
    "subject": "Computer Science",
    "level": "Intermediate",
    "estimatedHours": 12
  }
]
```

### 1.2 Get Course with Lessons & Student Progress
- **Endpoint**: `GET /api/courses/{id}?studentId={studentId}`
- **Response**: `200 OK`
```json
{
  "course": { "id": "course-dsa-101", "title": "Data Structures & Algorithms" },
  "lessons": [
    {
      "id": "lesson-dsa-big-o",
      "courseId": "course-dsa-101",
      "title": "Big-O Complexity Analysis",
      "orderIndex": 1,
      "estimatedMinutes": 25
    }
  ],
  "completedLessonsCount": 1,
  "overallProgressPercent": 50
}
```

### 1.3 Download Offline Lesson Package
- **Endpoint**: `GET /api/lessons/{id}/download?studentId={studentId}`
- **Description**: Bundles the lesson markdown content, quiz questions, attachments, and current progress for 100% offline consumption on Android.
- **Response**: `200 OK`
```json
{
  "course": { ... },
  "lesson": {
    "id": "lesson-dsa-big-o",
    "contentMarkdown": "# Big-O Notation...",
    "quizQuestionsJson": "[{\"question\": \"What is worst case of Binary Search?\", ...}]"
  },
  "progress": { "completed": false, "progressPercentage": 0 },
  "siblingLessons": [ ... ],
  "downloadedAt": 1729074000000
}
```

---

## 2. Conflict-Free Synchronization (CRDT)

### 2.1 Bidirectional Delta Sync
- **Endpoint**: `POST /api/sync/delta`
- **Description**: Primary synchronization endpoint called by Android WorkManager and Web Simulator. Pushes client's unsynced operations, updates server state, and returns missing operations for the client.
- **Request Body**:
```json
{
  "clientId": "student-android-device",
  "deviceName": "Google Pixel 7",
  "clientVector": {
    "clocks": {
      "student-android-device": 14,
      "server": 20
    }
  },
  "operations": [
    {
      "opId": "c9284fae-128f-43ea-98bf-49f39097e883",
      "noteId": "note-welcome-guide",
      "type": "INSERT_BLOCK",
      "clientId": "student-android-device",
      "lamportClock": 15,
      "timestamp": 1729074100000,
      "targetId": "b-bfs-1",
      "payload": "a0V",
      "secondaryPayload": "BFS visits vertices level by level:::PARAGRAPH"
    }
  ]
}
```
- **Response**: `200 OK`
```json
{
  "status": "SUCCESS",
  "acknowledgedOpIds": ["c9284fae-128f-43ea-98bf-49f39097e883"],
  "missingOperations": [],
  "serverVector": {
    "clocks": {
      "student-android-device": 15,
      "server": 20
    }
  },
  "updatedNotes": [ ... ],
  "serverTimestamp": 1729074105000
}
```

### 2.2 Get Server Version Vector
- **Endpoint**: `GET /api/sync/vector`
- **Response**: `200 OK`
```json
{
  "clocks": {
    "server": 32,
    "device-phone": 12,
    "device-tablet": 18
  }
}
```

---

## 3. Study Notes

### 3.1 List Active Notes
- **Endpoint**: `GET /api/notes`

### 3.2 Get Note Rendered as Markdown
- **Endpoint**: `GET /api/notes/{id}/markdown`
- **Response**: `200 OK` (Content-Type: `text/markdown`)

---

## 4. Multi-Device Conflict Simulator

### 4.1 Simulate Concurrent Offline Edits & Merge
- **Endpoint**: `POST /api/simulator/simulate-conflict`
- **Request Body**:
```json
{
  "phoneText": "Device 1 offline edit",
  "phoneTag": "MobileNote",
  "tabletText": "Device 2 offline edit",
  "tabletTitle": "Updated Title on Tablet"
}
```
- **Response**: `200 OK` returns converged note with both edits merged without data loss.

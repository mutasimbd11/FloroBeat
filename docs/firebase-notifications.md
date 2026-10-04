# FloroBeat Firebase Cloud Messaging (FCM) Integration Guide

This document provides complete instructions for configuring, testing, and managing Firebase Cloud Messaging (FCM) push notifications in FloroBeat.

---

## 1. Firebase Project Creation

1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing project).
3. Name your project (e.g. `FloroBeat` or `FloroBeat-Music`).
4. Google Analytics can be enabled or disabled according to your telemetry and privacy policy (FloroBeat operates under a privacy-first model).
5. Click **Create project**.

---

## 2. Android App Registration

1. In the Project Overview page of Firebase Console, click the **Android** icon (`</>`) to add an Android application.
2. Register the application using the IDs described below.

---

## 3. Application & Package IDs

FloroBeat defines two build flavors (`dev` and `prod`). Register either or both in Firebase Console:

| Build Flavor | Application ID / Package Name | Description |
| :--- | :--- | :--- |
| **Production (`prod`)** | `com.florosoft.florobeat` | Official production release |
| **Development (`dev`)** | `com.dev.florobeat` | Debug / side-by-side development build |

- App nickname: `FloroBeat`
- Debug signing certificate SHA-1: Optional, but recommended if you test dynamic app links.

---

## 4. Downloading `google-services.json`

1. After entering the application ID, click **Register app**.
2. Click **Download google-services.json**.
3. Do not alter or rename the file.

---

## 5. Where to Place `google-services.json`

Place the downloaded file into the `app` module directory:

```
FloroBeat/
├── app/
│   ├── google-services.json   <-- PLACE FILE HERE
│   ├── build.gradle.kts
│   └── src/
```

> **Note**: `app/google-services.json` contains public client configuration. FloroBeat's build script automatically applies `com.google.gms.google-services` when this file is present. Never commit private service-account keys or server secrets to Git.

---

## 6. Enabling Firebase Cloud Messaging

1. In the Firebase Console sidebar, go to **Build** → **Cloud Messaging**.
2. If Cloud Messaging API (HTTP v1) is not enabled, enable it in the Google Cloud Console for this project.

---

## 7. Testing from Firebase Console

1. In Firebase Console, go to **Engage** → **Messaging** (or **Campaigns**).
2. Click **New campaign** → **Notifications**.
3. Enter notification details:
   - **Notification title**: `New FloroBeat Release`
   - **Notification text**: `FloroBeat 2.0 is now available!`
4. In **Additional options (optional)**:
   - Add custom data:
     - `type`: `RELEASE`
     - `route`: `/settings/updates`
     - `notification_id`: `test_release_001`
5. Target: Select your app (`com.florosoft.florobeat` or `com.dev.florobeat`) or test device FCM registration token.
6. Click **Review** → **Publish** or send a test message.

---

## 8. Android 13+ Notification Permission (`POST_NOTIFICATIONS`)

- Declared in `AndroidManifest.xml`: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
- Handled at runtime in accordance with Android guidelines:
  - **No cold-launch dialog spam**: The app will never present a permission popup immediately upon first launch.
  - **User-controlled**: Managed cleanly inside **Settings → Miscellaneous → Push notifications**.
  - Toggling notifications on Android 13+ prompts for permission via `ActivityResultContracts.RequestPermission()`.
  - Denials are respected without recurring nag dialogs.
  - On Android 12 and below, notifications behave normally without requiring runtime permission prompts.

---

## 9. Notification Channels

FloroBeat maintains strict separation between media playback notifications and system update notifications:

| Channel ID | Channel Name | Importance | Description |
| :--- | :--- | :--- | :--- |
| `florobeat_updates` | `FloroBeat Updates` | `IMPORTANCE_DEFAULT` | Releases, announcements, and maintenance alerts |
| `downloads` | `Downloads` | `IMPORTANCE_LOW` | Progress notifications when tracks are downloaded |
| `media3_session_*` | `Now Playing` | `IMPORTANCE_LOW` | Media3 foreground audio playback controls |

The `florobeat_updates` channel is initialized idempotently in `FloroBeatNotificationManager.createNotificationChannels()`.

---

## 10. Payload Format

FloroBeat expects structured JSON data. FCM messages can be sent as data-only or combined notification + data payloads.

### Recommended JSON Structure

```json
{
  "message": {
    "topic": "florobeat_releases",
    "notification": {
      "title": "New FloroBeat Release",
      "body": "FloroBeat 2.0 is now available on GitHub!"
    },
    "data": {
      "type": "RELEASE",
      "title": "New FloroBeat Release",
      "body": "FloroBeat 2.0 is now available on GitHub!",
      "route": "/settings/updates",
      "url": "https://github.com/mutasimbd11/FloroBeat/releases",
      "notification_id": "rel_2_0_0"
    }
  }
}
```

### Supported Notification Types

- `RELEASE`: App updates (opens update dialog / check).
- `ANNOUNCEMENT`: General announcements (routes to Home).
- `MAINTENANCE`: Service / provider status notices.
- `FEATURE`: Highlights new capabilities (routes to feature/explore screen).
- `IMPORTANT`: Critical user notices.
- Unrecognized types fall back safely to `UNKNOWN` without crashing.

---

## 11. Deep-Link Routes

When a notification is tapped, `NotificationDeepLink` relays the target route to `MainActivity`:

| Route / Type | Destination Screen | Action |
| :--- | :--- | :--- |
| `/settings/updates` or type `RELEASE` | Settings → Updates | Opens update dialog and queries GitHub Releases |
| `/settings/listen-together` or `/jam` | Settings → Listen Together | Opens Listen Together party screen |
| `/settings/equalizer` | Settings → Equalizer | Opens parametric equalizer |
| `/settings/sources` | Settings → Sources | Opens audio and lyrics source selector |
| `/settings/account` | Settings → Account & Scrobbling | Opens Last.fm / Google account management |
| `/settings` | Settings | Opens general settings sheet |
| `/explore` or type `FEATURE` | Explore Tab | Navigates to Explore tab |
| `/library` | Library Tab | Navigates to Library tab |
| `/search` | Search Tab | Navigates to Search tab |
| Unrecognized or missing route | Home Tab | Safe default fallback to Home feed |

---

## 12. Topic Architecture

Centralized in `NotificationTopicManager`:

- `florobeat_all`: Broadcast messages to all opted-in users.
- `florobeat_releases`: Update and release notices.
- `florobeat_announcements`: Community and feature announcements.

Topics are automatically subscribed when push notifications are active, and unsubscribed if the user turns push notifications off.

---

## 13. Secure Backend Architecture

```
+-------------------+       HTTPS (FCM v1)       +-----------------------+
| FloroBeat Backend | -------------------------> | Firebase Cloud Server |
| (Admin SDK / Key) |                            +-----------------------+
+-------------------+                                        |
                                                             | Push Message
                                                             v
                                                 +-----------------------+
                                                 | FloroBeat Android App |
                                                 +-----------------------+
```

- **Client Principle**: The FloroBeat Android client is strictly a receiver.
- **Server Principle**: The sender must always be a secured server environment (e.g. backend microservice, GitHub Action workflow, or serverless Cloud Function).

---

## 14. Sending Notifications via FCM HTTP v1 API

Send notifications using Google Application Default Credentials or the Firebase Admin SDK:

### Node.js Example (Firebase Admin SDK)

```javascript
const admin = require('firebase-admin');

// Service account JSON exists ONLY on the backend server
admin.initializeApp({
  credential: admin.credential.applicationDefault()
});

async function sendReleaseNotification(version) {
  const message = {
    topic: 'florobeat_releases',
    notification: {
      title: `FloroBeat ${version} Available`,
      body: 'Tap to view release notes and download the update.'
    },
    data: {
      type: 'RELEASE',
      title: `FloroBeat ${version} Available`,
      body: 'Tap to view release notes and download the update.',
      route: '/settings/updates',
      notification_id: `release_${version.replace(/\./g, '_')}`
    }
  };

  const response = await admin.messaging().send(message);
  console.log('Successfully sent message:', response);
}
```

---

## 15. Production Privacy & Security Considerations

1. **Never commit secrets**:
   - Never embed Firebase Service Account keys, private keys, or server keys inside the Android codebase or Git repository.
2. **Never log full tokens**:
   - `FcmTokenManager` masks tokens (e.g. `fK8j...9x2A`) in logs to protect user identity.
3. **No third-party leaks**:
   - Registration tokens are stored in private `SharedPreferences` and are never broadcast to arbitrary third-party services.
4. **Existing GitHub update mechanism remains authority**:
   - FCM serves strictly as an alerting channel. APK downloads, SHA-256 verification, and package installations remain entirely handled by FloroBeat's existing secure GitHub release pipeline (`AppUpdateChecker` and `ApkVerifier`).

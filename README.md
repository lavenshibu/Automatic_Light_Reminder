# 🚘 Smart Headlight Reminder App

An Android app that connects to a **Grove Light Sensor** via **MQTT**, reads ambient (surrounding/outdoor) light levels, and gives **voice reminders** using **Text-to-Speech** when it's too dark — helping drivers remember to turn on their headlights.

---

## 📱 Features

- 📡 Realtime MQTT communication with a light sensor (e.g., CC3200 + Grove Light Sensor)
- 🎤 Text-to-Speech voice prompt: “Turn on headlight”
- 🌙 Detects low light conditions and shows visual alerts
- 🎨 Modern UI built with **Jetpack Compose**
- 🔌 Powered by **Paho MQTT** client
- 🌓 Supports automatic dark/light system themes

---

## 🚀 Getting Started

### ✅ Requirements

- Android Studio **Hedgehog** or later
- Android Emulator or Device (API 28+)
- Internet connection

---

### 📦 Clone and Run

```bash
git clone https://github.com/lavenshibu/Automatic_Light_Reminder.git
cd Automatic_Light_Reminder
```

---

###  Open in Android Studio

1. Launch **Android Studio**
2. Click **"Open an existing project"**
3. Select the `Automatic_Light_Reminder` folder
4. Wait for Gradle sync and dependencies to finish

---

###  Run the App

1. Connect a physical Android device or start an emulator (API level 28+)
2. Click the **Run ▶️** button or press **Shift + F10**

---

##  Architecture / Folder Structure

```bash
Automatic_Light_Reminder/
├── app/                           # Main application module
│   └── src/main/java/...          # Contains MainActivity and composables
│   └── src/main/res/              # Resources (themes, icons)
│   └── AndroidManifest.xml        # Permissions and app declarations
├── build.gradle.kts               # App-level Gradle build file
├── settings.gradle.kts            # Project settings
├── README.md                      # This file
```

> This project follows a clean and modular Android/Kotlin structure using Jetpack Compose.

---

##  MQTT Broker Configuration

### Default values in `MainActivity.kt`:

```kotlin
private val brokerUrl = "tcp://broker.hivemq.com:1883"
private val subscriptionTopic = "sensors/light/cc3200"
```

- Connects to the **HiveMQ public broker**
- Subscribes to topic: `sensors/light/cc3200`
- Light sensor should publish **integer light values** like `1000`, `2000`, etc.
- App triggers **voice alert** when value is below `LOW_LIGHT_THRESHOLD` (default = 1500)

### To use your own broker:

- Replace `brokerUrl` with your own (e.g. `"tcp://192.168.1.100:1883"`)
- Change `subscriptionTopic` to match your device’s topic
- Optionally adjust `LOW_LIGHT_THRESHOLD` based on your environment



##  Built With

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Paho MQTT Client](https://www.eclipse.org/paho/)
- [Material3](https://m3.material.io/)
- [TextToSpeech API](https://developer.android.com/reference/android/speech/tts/TextToSpeech)

---

##  Permissions Used

- `INTERNET` – to connect to the MQTT broker
- `ACCESS_NETWORK_STATE` – to detect connection availability
- `WAKE_LOCK` – to keep the app active when receiving messages
- `RECEIVE_BOOT_COMPLETED` – to enable reconnection if needed after reboot

---




> Feel free to fork this repo, open issues, or contribute with pull requests!

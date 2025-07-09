#  Smart Headlight Reminder App

An Android app that connects to a **Grove Light Sensor** via **MQTT**, reads ambient light levels, and gives **voice reminders** using **Text-to-Speech** when it's too dark — helping drivers remember to turn on their headlights.

---

##  Features

-  Realtime MQTT communication with a light sensor (e.g., CC3200 + Grove Light Sensor)
-  Text-to-Speech voice prompt: “Turn on headlight”
-  Detects low light conditions and shows visual alerts
-  Modern UI built with **Jetpack Compose**
-  Powered by **Paho MQTT** client
-  Supports automatic dark/light system themes

---

###  Requirements

- Android Studio **Hedgehog** or later
- Android Emulator or Device (API 28+)
- Internet connection

---

###  Clone and Run

```bash
git clone https://github.com/lavenshibu/Automatic_Light_Reminder.git
cd Automatic_Light_Reminder

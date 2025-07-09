#include <SPI.h>
#include <WiFi.h>
#include <PubSubClient.h>

// WiFi credentials
char ssid[] = "username";         // <<-- VERIFY THIS IS CORRECT FOR YOUR WIFI NETWORK
char password[] = "password"; // <<-- VERIFY THIS IS CORRECT FOR YOUR WIFI NETWORK

// HiveMQ broker
char mqtt_server[] = "broker.hivemq.com";
// *** IMPORTANT: This topic MUST match your Android app's subscriptionTopic ***
const char* topic = "sensors/light/cc3200"; // Changed to match Android app

WiFiClient wifiClient;
PubSubClient client(mqtt_server, 1883, wifiClient);

void setup() {
  Serial.begin(115200);
  delay(1000);

  // Connect WiFi
  Serial.print("Connecting WiFi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  // Connect MQTT
  client.setServer(mqtt_server, 1883);
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");
    // Using a consistent Client ID for this device
    if (client.connect("CC3200Client")) {
      Serial.println(" connected");
    } else {
      Serial.print(" failed, rc=");
      Serial.print(client.state());
      Serial.println(" trying again in 1 second");
      delay(1000);
    }
  }
  Serial.println("Energia setup complete.");
}
void loop() {
  // Always call client.loop() to maintain connection and process MQTT events
  if (!client.connected()) {
    Serial.println("MQTT client disconnected. Attempting to reconnect...");
    if (client.connect("CC3200Client")) {
      Serial.println("MQTT reconnected.");
    } else {
      Serial.print("MQTT reconnection failed, rc=");
      Serial.println(client.state());
      delay(5000); // Wait 5 seconds before next retry
      return; // Skip the rest of the loop if not connected
    }
  }
  client.loop(); // Keeps the MQTT client alive and processes messages

  int light = analogRead(24); // Read light sensor value from pin 24 (adjust if your sensor is on a different pin)

  Serial.print("Light value: ");
  Serial.println(light);

  // Convert the integer light value to a String to send via MQTT
  String lightString = String(light);

  // Publish the light value to the specified MQTT topic
  if (client.connected()) {
    client.publish(topic, lightString.c_str()); // .c_str() converts String to const char*
    Serial.print("Published light value to ");
    Serial.print(topic);
    Serial.print(": ");
    Serial.println(lightString);
  } else {
    Serial.println("MQTT client not connected, cannot publish.");
  }
  
// Your original logic for determining headlight status (optional for MQTT)
  if (light < 1500) {
    Serial.println("❌ It's dark — turn on headlights!");
  } else {
    Serial.println("✅ Bright enough.");
  }
delay(1000); // Wait for 1 second before the next reading and publish
}

// Main package definition
package com.example.lhtkotapp // Must match your project package

// --- Android and Jetpack Compose Imports ---
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lhtkotapp.ui.theme.LhtkotAppTheme

// --- TTS and Locale Imports ---
import android.speech.tts.TextToSpeech
import java.util.Locale
import android.content.Context

// --- MQTT (Paho) Imports ---
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : ComponentActivity() {

    // MQTT configuration constants
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private val clientId = MqttClient.generateClientId()
    private val subscriptionTopic = "sensors/light/cc3200"
    private val qos = 1
    private val LOW_LIGHT_THRESHOLD = 1500

    private var mqttClient: MqttAsyncClient? = null // Holds the MQTT client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LhtkotAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MqttLightAppContent(
                        context = this,
                        brokerUrl = brokerUrl,
                        clientId = clientId,
                        subscriptionTopic = subscriptionTopic,
                        qos = qos,
                        lowLightThreshold = LOW_LIGHT_THRESHOLD,
                        onMqttClientReady = { client -> mqttClient = client }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient?.disconnect() // Disconnect MQTT on app close
            Log.d("MQTT", "MQTT Client disconnected on onDestroy.")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error disconnecting MQTT client: ${e.message}", e)
        }
    }
}

@Composable
fun MqttLightAppContent(
    context: Context,
    brokerUrl: String,
    clientId: String,
    subscriptionTopic: String,
    qos: Int,
    lowLightThreshold: Int,
    onMqttClientReady: (MqttAsyncClient) -> Unit
) {
    // UI state variables
    var connectionStatus by remember { mutableStateOf("Connecting to MQTT...") }
    var lightValue by remember { mutableStateOf("N/A") }
    var headlightStatus by remember { mutableStateOf("Headlights: OFF") }
    var headlightsOn by remember { mutableStateOf(false) }

    // TTS state and control flag
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var hasSpokenHeadlightsOn by remember { mutableStateOf(false) }

    // UI color animations based on light status
    val lightIndicatorColor by animateColorAsState(if (headlightsOn) Color(0xFFFFCC00) else Color.White, tween(500))
    val lightIndicatorBorderColor by animateColorAsState(if (headlightsOn) Color(0xFFFFA000) else Color(0xFFCCCCCC), tween(500))
    val headlightTextColor by animateColorAsState(if (headlightsOn) Color(0xFFFFC107) else Color(0xFF4CAF50), tween(500))
    val animationPlaceholderBgColor by animateColorAsState(if (headlightsOn) Color(0xFFFFF9C4) else Color(0xFFE0E0E0), tween(500))
    val animationPlaceholderTextColor by animateColorAsState(if (headlightsOn) Color(0xFFFFEB3B) else Color(0xFFE0E0E0), tween(500))

    // TTS lifecycle management
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
        tts = ttsInstance

        onDispose {
            tts?.stop()
            tts?.shutdown()
            Log.d("TTS", "TTS shut down")
        }
    }

    // MQTT client setup and lifecycle
    DisposableEffect(Unit) {
        var client: MqttAsyncClient? = null
        try {
            client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
            onMqttClientReady(client)

            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    connectionStatus = "Connected & Subscribed"
                    hasSpokenHeadlightsOn = false
                    client?.subscribe(subscriptionTopic, qos, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MQTT", "Subscribed to topic")
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            connectionStatus = "Subscription Failed!"
                        }
                    })
                }
                override fun connectionLost(cause: Throwable?) {
                    connectionStatus = "Connection Lost. Reconnecting..."
                    hasSpokenHeadlightsOn = false
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString()
                    payload?.toIntOrNull()?.let { value ->
                        val newHeadlightsOn = value < lowLightThreshold
                        if (newHeadlightsOn && !headlightsOn && tts?.isSpeaking == false) {
                            tts?.speak("Turn on headlight", TextToSpeech.QUEUE_FLUSH, null, "headlight_on_utterance")
                            hasSpokenHeadlightsOn = true
                        } else if (!newHeadlightsOn && headlightsOn) {
                            hasSpokenHeadlightsOn = false
                        }
                        lightValue = value.toString()
                        headlightsOn = newHeadlightsOn
                        headlightStatus = if (headlightsOn) "Headlights: ON" else "Headlights: OFF"
                    } ?: run {
                        lightValue = "Error"
                        headlightStatus = "Headlights: N/A"
                        headlightsOn = false
                        hasSpokenHeadlightsOn = false
                    }
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Delivery complete")
                }
            })

            client.connect(MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 60
                keepAliveInterval = 60
            }, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to broker")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    connectionStatus = "Connection Failed!"
                }
            })
        } catch (e: MqttException) {
            connectionStatus = "MQTT Error: ${e.message}"
        }

        onDispose {
            try {
                client?.disconnect()
            } catch (e: MqttException) {
                Log.e("MQTT", "Disconnection error: ${e.message}", e)
            }
        }
    }

    // --- UI Layout ---
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Smart Headlight Control", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Text(
            text = connectionStatus,
            fontSize = 18.sp,
            color = when (connectionStatus) {
                "Connected & Subscribed" -> Color(0xFF4CAF50)
                "Connection Lost. Reconnecting..." -> Color(0xFFFF9800)
                "Connection Failed!" -> Color(0xFFF44336)
                else -> Color(0xFF666666)
            },
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Box(
            modifier = Modifier.size(180.dp).background(lightIndicatorColor, CircleShape).border(2.dp, lightIndicatorBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = lightValue, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Text("Light Level", fontSize = 18.sp, color = Color(0xFF666666))
            }
        }
        Text(
            text = headlightStatus,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = headlightTextColor,
            modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)
        )
        Box(
            modifier = Modifier.width(200.dp).height(80.dp).background(animationPlaceholderBgColor, RoundedCornerShape(10.dp)).border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💡", fontSize = 48.sp, color = animationPlaceholderTextColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LhtkotAppTheme {
        MqttLightAppContent(
            context = androidx.compose.ui.platform.LocalContext.current,
            brokerUrl = "tcp://broker.hivemq.com:1883",
            clientId = "previewClient",
            subscriptionTopic = "sensors/light/cc3200",
            qos = 1,
            lowLightThreshold = 50,
            onMqttClientReady = {}
        )
    }
}

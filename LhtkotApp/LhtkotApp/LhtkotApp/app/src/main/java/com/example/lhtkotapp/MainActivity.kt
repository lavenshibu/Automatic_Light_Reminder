package com.example.lhtkotapp // IMPORTANT: This package name MUST match your new project's package

// Core Android imports
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect // Explicit import for DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lhtkotapp.ui.theme.LhtkotAppTheme // Your project's theme import - VERIFY THIS PATH

// Paho MQTT client imports
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : ComponentActivity() {

    // MQTT Configuration
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private val clientId = MqttClient.generateClientId()
    private val subscriptionTopic = "sensors/light/cc3200"
    private val qos = 1
    private val LOW_LIGHT_THRESHOLD = 50

    private var mqttClient: MqttAsyncClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LhtkotAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MqttLightAppContent(
                        brokerUrl = brokerUrl,
                        clientId = clientId,
                        subscriptionTopic = subscriptionTopic,
                        qos = qos,
                        lowLightThreshold = LOW_LIGHT_THRESHOLD,
                        onMqttClientReady = { client ->
                            mqttClient = client
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient?.disconnect()
            Log.d("MQTT", "MQTT Client disconnected on onDestroy.")
        } catch (e: MqttException) {
            Log.e("MQTT", "Error disconnecting MQTT client on onDestroy: ${e.message}", e)
        }
    }
}

@Composable
fun MqttLightAppContent(
    brokerUrl: String,
    clientId: String,
    subscriptionTopic: String,
    qos: Int,
    lowLightThreshold: Int,
    onMqttClientReady: (MqttAsyncClient) -> Unit
) {
    var connectionStatus by remember { mutableStateOf("Connecting to MQTT...") }
    var lightValue by remember { mutableStateOf("N/A") }
    var headlightStatus by remember { mutableStateOf("Headlights: OFF") }
    var headlightsOn by remember { mutableStateOf(false) }

    val lightIndicatorColor by animateColorAsState(
        targetValue = if (headlightsOn) Color(0xFFFFCC00) else Color(0xFFFFFFFF),
        animationSpec = tween(500), label = "lightIndicatorColor"
    )
    val lightIndicatorBorderColor by animateColorAsState(
        targetValue = if (headlightsOn) Color(0xFFFFA000) else Color(0xFFCCCCCC),
        animationSpec = tween(500), label = "lightIndicatorBorderColor"
    )
    val headlightTextColor by animateColorAsState(
        targetValue = if (headlightsOn) Color(0xFFFFC107) else Color(0xFF4CAF50),
        animationSpec = tween(500), label = "headlightTextColor"
    )
    val animationPlaceholderBgColor by animateColorAsState(
        targetValue = if (headlightsOn) Color(0xFFFFF9C4) else Color(0xFFE0E0E0),
        animationSpec = tween(500), label = "animationPlaceholderBgColor"
    )
    val animationPlaceholderTextColor by animateColorAsState(
        targetValue = if (headlightsOn) Color(0xFFFFEB3B) else Color(0xFFE0E0E0),
        animationSpec = tween(500), label = "animationPlaceholderTextColor"
    )

    // Use DisposableEffect for managing the MQTT client lifecycle
    DisposableEffect(Unit) { // 'Unit' as a key means this effect runs once
        var client: MqttAsyncClient? = null
        try {
            client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
            onMqttClientReady(client)

            client.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d("MQTT", "MQTT Connected to: $serverURI (Reconnect: $reconnect)")
                    connectionStatus = "Connected to MQTT"
                    client.subscribe(subscriptionTopic, qos, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d("MQTT", "Subscribed to topic: $subscriptionTopic")
                            connectionStatus = "Connected & Subscribed"
                        }
                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("MQTT", "Failed to subscribe: ${exception?.message}", exception)
                            connectionStatus = "Subscription Failed!"
                        }
                    })
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "MQTT Connection lost: ${cause?.message}")
                    connectionStatus = "Connection Lost. Reconnecting..."
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString()
                    Log.d("MQTT", "Message arrived: Topic=$topic, Payload=$payload")
                    if (payload != null) {
                        try {
                            val value = payload.toInt()
                            lightValue = value.toString()
                            headlightsOn = value < lowLightThreshold
                            headlightStatus = if (headlightsOn) "Headlights: ON" else "Headlights: OFF"
                        } catch (e: NumberFormatException) {
                            Log.e("MQTT", "Invalid light value format: $payload", e)
                            lightValue = "Error"
                            headlightStatus = "Headlights: N/A"
                            headlightsOn = false
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTT", "Delivery complete: ${token?.messageId}")
                }
            })

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 60
                keepAliveInterval = 60
            }

            Log.d("MQTT", "Attempting to connect to MQTT broker: $brokerUrl")
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "MQTT Connection successful!")
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "MQTT Connection failed: ${exception?.message}", exception)
                    connectionStatus = "Connection Failed!"
                }
            })

        } catch (e: MqttException) {
            Log.e("MQTT", "Error initializing MQTT client: ${e.message}", e)
            connectionStatus = "MQTT Error: ${e.message}"
        }

        // This is the onDispose block provided by DisposableEffect
        onDispose {
            try {
                client?.disconnect()
                Log.d("MQTT", "MQTT Client disconnected on DisposableEffect onDispose.")
            } catch (e: MqttException) {
                Log.e("MQTT", "Error disconnecting MQTT client on DisposableEffect onDispose: ${e.message}", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Smart Headlight Control",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = connectionStatus,
            fontSize = 18.sp,
            color = when (connectionStatus) {
                "Connected & Subscribed" -> Color(0xFF4CAF50)
                "Connection Lost. Reconnecting..." -> Color(0xFFFF9800)
                "Connection Failed!" -> Color(0xFFF44336)
                else -> if (connectionStatus.startsWith("MQTT Error:")) Color(0xFFF44336) else Color(0xFF666666)
            },
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(lightIndicatorColor, CircleShape)
                .border(2.dp, lightIndicatorBorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = lightValue,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = "Light Level",
                    fontSize = 18.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 8.dp)
                )
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
            modifier = Modifier
                .width(200.dp)
                .height(80.dp)
                .background(animationPlaceholderBgColor, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💡",
                fontSize = 48.sp,
                color = animationPlaceholderTextColor
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MQTT Broker: $brokerUrl",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "MQTT Topic: $subscriptionTopic",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LhtkotAppTheme {
        MqttLightAppContent(
            brokerUrl = "tcp://broker.hivemq.com:1883",
            clientId = "previewClient",
            subscriptionTopic = "sensors/light/cc3200",
            qos = 1,
            lowLightThreshold = 50,
            onMqttClientReady = {}
        )
    }
}

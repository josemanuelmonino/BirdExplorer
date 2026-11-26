package com.example.birdexplorer

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import org.meshtastic.mqtt.MqttClient
import org.meshtastic.mqtt.transport.tcp.TcpTransportFactory
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.meshtastic.mqtt.MqttEndpoint
import org.meshtastic.mqtt.QoS
import org.json.JSONObject
import org.meshtastic.mqtt.messagesMatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import java.util.UUID
import org.meshtastic.mqtt.PublishProperties

private const val DEFAULT_BROKER = "tcp://192.168.1.19:1883"
private const val PREFS = "birdexplorer_prefs"
private const val KEY_BROKER = "broker_url"
private const val PUB_TOPIC = "birdexplorer/sightings"
private const val SUB_TOPIC = "birdexplorer/sightings/#"

class MqttViewModel(application: Application) : AndroidViewModel(application) {

    private val client = MqttClient("birdexplorer-" + System.currentTimeMillis()) {
        transportFactory = TcpTransportFactory()
        autoReconnect = true
        keepAliveSeconds = 30
    }

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _brokerUrl = MutableStateFlow(
        prefs.getString(KEY_BROKER, DEFAULT_BROKER) ?: DEFAULT_BROKER
    )
    val brokerUrl: StateFlow<String> = _brokerUrl.asStateFlow()

    private val _uiState = MutableStateFlow(MqttUiState())
    val uiState: StateFlow<MqttUiState> = _uiState.asStateFlow()

    init {
        collectSightings()
        connectAndSubscribe()
    }

    private fun connectAndSubscribe() {
        viewModelScope.launch {
            try {
                client.connect(MqttEndpoint.parse(_brokerUrl.value))
                client.subscribe(SUB_TOPIC, QoS.AT_LEAST_ONCE)

                Log.d("BirdExplorerMQTT", "Connected and subscribed")

            } catch (e: Exception) {
                Log.e("BirdExplorerMQTT", "MQTT connection error", e)
            }
        }
    }

    fun changeBroker(input: String) {
        var url = input.trim()
        if (!url.startsWith("tcp://")) url = "tcp://$url"
        if (url.count { it == ':' } < 2) url += ":1883"   // default port

        prefs.edit().putString(KEY_BROKER, url).apply()
        _brokerUrl.value = url

        viewModelScope.launch {
            try { client.disconnect() } catch (_: Exception) {}
            connectAndSubscribe()
        }
    }


    fun publishSighting(
        species: String,
        location: String,
        notes: String,
        lat: Double?,
        lon: Double?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sighting = JSONObject().apply {
                    put("species", species)
                    put("location", location)
                    put("notes", notes)
                    put("timestamp", System.currentTimeMillis())
                    if (lat != null && lon != null) {
                        put("lat", lat)
                        put("lon", lon)
                    }
                }

                val topic = "$PUB_TOPIC/${UUID.randomUUID()}"

                client.publish(
                    topic = topic,
                    payload = sighting.toString(),
                    qos = QoS.AT_LEAST_ONCE,
                    retain = true,
                    properties = PublishProperties(
                        messageExpiryInterval = 1296000L //15 dias
                        // messageExpiryInterval = 604800L // 7 dias
                        // messageExpiryInterval = 60L //60s (pruebas)
                    )
                )
                val properties = PublishProperties()
                Log.d("BirdExplorerMQTT", "Sighting published: $sighting")
                onResult(true)

            } catch (e: Exception) {
                Log.e("BirdExplorerMQTT", "Error publishing sighting", e)
                onResult(false)
            }
        }
    }


    fun observeMessages() = client.messagesMatching(SUB_TOPIC)

    private fun collectSightings() {
        viewModelScope.launch {
            client.messagesMatching(SUB_TOPIC).collect { message ->
                try {
                    val json = JSONObject(message.payloadAsString())

                    val sighting = Sighting(
                        species = json.getString("species"),
                        location = json.getString("location"),
                        notes = json.getString("notes"),
                        timestamp = json.getLong("timestamp"),
                        lat = json.optDouble("lat").takeIf { !it.isNaN() },
                        lon = json.optDouble("lon").takeIf { !it.isNaN() }
                    )

                    _uiState.update { currentState ->
                        currentState.copy(
                            sightings = currentState.sightings + sighting
                        )
                    }

                } catch (e: Exception) {
                    Log.e("BirdExplorerMQTT", "Error processing sighting", e)
                }
            }
        }
    }



}
package com.example.birdexplorer

import android.app.Application
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

private const val BROKER = "tcp://192.168.1.83:1883" // IP de ejemplo
private const val PUB_TOPIC = "birdexplorer/sightings"
private const val SUB_TOPIC = "birdexplorer/sightings"

class MqttViewModel(application: Application) : AndroidViewModel(application) {

    private val client = MqttClient("birdexplorer-" + System.currentTimeMillis()) {
        transportFactory = TcpTransportFactory()
        autoReconnect = true
        keepAliveSeconds = 30
    }

    private val _uiState = MutableStateFlow(MqttUiState())
    val uiState: StateFlow<MqttUiState> = _uiState.asStateFlow()

    init {
        collectSightings()
        connectAndSubscribe()
    }

    private fun connectAndSubscribe() {
        viewModelScope.launch {
            try {
                client.connect(MqttEndpoint.parse(BROKER))
                client.subscribe(SUB_TOPIC, QoS.AT_LEAST_ONCE)

                Log.d("BirdExplorerMQTT", "Connected and subscribed")

            } catch (e: Exception) {
                Log.e("BirdExplorerMQTT", "MQTT connection error", e)
            }
        }
    }


    fun publishSighting(
        species: String,
        location: String,
        notes: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sighting = JSONObject().apply {
                    put("species", species)
                    put("location", location)
                    put("notes", notes)
                    put("timestamp", System.currentTimeMillis())
                }

                client.publish(
                    PUB_TOPIC,
                    sighting.toString(),
                    QoS.AT_LEAST_ONCE
                )

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
                        timestamp = json.getLong("timestamp")
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
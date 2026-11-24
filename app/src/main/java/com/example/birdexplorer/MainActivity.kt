package com.example.birdexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.birdexplorer.ui.theme.BirdExplorerTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    private val mqttViewModel: MqttViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue =
            "mdp/BirdExplorer" // if we just use userAgentValue = packageName, it returns a package error because it begins with
                                // com.example (com.example.birdExplorer)
        enableEdgeToEdge()

        setContent {
            BirdExplorerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BirdExplorerApp(
                        mqttViewModel = mqttViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

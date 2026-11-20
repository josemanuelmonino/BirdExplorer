package com.example.birdexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.birdexplorer.data.StepSensorManager
import com.example.birdexplorer.ui.theme.BirdExplorerTheme
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

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
                        modifier = Modifier.padding(innerPadding)
                    )

                }
            }
        }
    }
}

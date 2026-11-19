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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

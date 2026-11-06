package dev.hdytrfli.omata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.hdytrfli.omata.ui.CameraScreen
import dev.hdytrfli.omata.ui.theme.OmataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OmataTheme { CameraScreen() } }
    }
}

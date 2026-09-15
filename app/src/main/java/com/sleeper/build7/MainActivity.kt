package com.sleeper.build7

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sleeper.build7.data.SleeperRepository
import com.sleeper.build7.ui.MainContainer
import com.sleeper.build7.ui.theme.SleeperBuildTheme

class MainActivity : ComponentActivity() {

    private lateinit var sleeperRepository: SleeperRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sleeperRepository = SleeperRepository(applicationContext)

        setContent {
            val themeMode by sleeperRepository.themeMode.collectAsState()
            SleeperBuildTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContainer(sleeperRepo = sleeperRepository)
                }
            }
        }
    }
}

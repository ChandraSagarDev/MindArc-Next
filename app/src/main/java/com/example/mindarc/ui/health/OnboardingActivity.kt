package com.example.mindarc.ui.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mindarc.MainActivity
import com.example.mindarc.ui.theme.MindArcTheme

/**
 * Entry point that Health Connect can launch when the user connects MindArc
 * from within Health Connect. This should explain what MindArc does with
 * health data and then route into the normal app flow.
 */
class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MindArcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Connect MindArc to Health Connect",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "When you connect MindArc to Health Connect, the app can read your heart rate " +
                                    "during push-up and squat sessions. This lets us show live workout metrics " +
                                    "without storing or sharing your raw health data with others.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "You can change or revoke these permissions at any time from the Health Connect settings.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                // Continue into the normal MindArc flow.
                                val intent = Intent(this@OnboardingActivity, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                                finish()
                            },
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text("Continue to MindArc")
                        }
                    }
                }
            }
        }
    }
}


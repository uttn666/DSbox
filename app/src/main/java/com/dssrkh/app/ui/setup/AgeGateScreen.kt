package com.dssrkh.app.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dssrkh.app.DssrkhApp
import com.dssrkh.app.ui.components.DsboxBrand

@Composable
fun AgeGateScreen(onConfirm: () -> Unit) {
    val app = LocalContext.current.applicationContext as DssrkhApp
    var checked by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            DsboxBrand()
            Spacer(Modifier.height(28.dp))
            Text(
                "本应用提供由 AI 生成的角色扮演内容，可能涉及情感互动。请确认你已年满 18 周岁。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Spacer(Modifier.width(8.dp))
                Text("我已年满 18 周岁")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    app.settings.ageConfirmed = true
                    onConfirm()
                },
                enabled = checked,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("继续")
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

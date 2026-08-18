package dev.ssactinium.phone3dsgamepad.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.network.LinkStatus
import dev.ssactinium.phone3dsgamepad.network.SessionUiState
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.ui.theme.FieldStroke
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing
import dev.ssactinium.phone3dsgamepad.ui.theme.HousingInset
import dev.ssactinium.phone3dsgamepad.ui.theme.HousingRaised
import dev.ssactinium.phone3dsgamepad.ui.theme.HudMono
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute
import dev.ssactinium.phone3dsgamepad.ui.theme.LedDead
import dev.ssactinium.phone3dsgamepad.ui.theme.LedLive
import dev.ssactinium.phone3dsgamepad.ui.theme.LedWait

@Composable
fun ConnectScreen(
    host: String,
    port: String,
    link: SessionUiState,
    busy: Boolean,
    notice: String,
    profile: ControlProfile,
    onHost: (String) -> Unit,
    onPort: (String) -> Unit,
    onProfile: (ControlProfile) -> Unit,
    onConnect: () -> Unit,
    onTest: () -> Unit,
) {
    val led = when (link.status) {
        LinkStatus.Connected -> LedLive
        LinkStatus.Connecting, LinkStatus.Degraded -> LedWait
        LinkStatus.Error -> LedDead
        LinkStatus.Idle -> InkMute
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HINGE PAD", color = HingeGold, fontSize = 20.sp, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(8.dp).clip(CircleShape).background(led))
            Spacer(Modifier.width(6.dp))
            Text(link.detail.ifBlank { "Disconnected" }, style = HudMono.copy(color = Ink, fontSize = 11.sp), maxLines = 1)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactField(
                    label = "PC IP",
                    value = host,
                    placeholder = "192.168.1.10",
                    keyboard = KeyboardType.Decimal,
                    onValue = onHost,
                    modifier = Modifier.weight(1.4f),
                )
                CompactField(
                    label = "Port",
                    value = port,
                    placeholder = "26760",
                    keyboard = KeyboardType.Number,
                    onValue = onPort,
                    modifier = Modifier.weight(0.7f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Play as", color = InkMute, fontSize = 11.sp, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetChip(
                    "Xbox",
                    "Arkham / PC",
                    profile == ControlProfile.Xbox,
                    Modifier.weight(1f),
                ) { onProfile(ControlProfile.Xbox) }
                PresetChip(
                    "3DS",
                    "Azahar / Citra",
                    profile == ControlProfile.N3ds,
                    Modifier.weight(1f),
                ) { onProfile(ControlProfile.N3ds) }
            }
            if (notice.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(notice, color = if (link.status == LinkStatus.Error) LedDead else Ink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onTest,
                enabled = !busy,
                modifier = Modifier.weight(1f).height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HingeGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HingeGold),
            ) { Text("Test") }
            Button(
                onClick = onConnect,
                enabled = !busy,
                modifier = Modifier.weight(1.4f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HingeGold, contentColor = Housing),
            ) { Text("Connect") }
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    placeholder: String,
    keyboard: KeyboardType,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, color = InkMute, fontSize = 11.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            placeholder = { Text(placeholder, color = InkMute, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            textStyle = HudMono.copy(fontSize = 15.sp, color = Ink),
            colors = fieldColors(),
        )
    }
}

@Composable
private fun PresetChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) HingeGold else HousingRaised)
            .border(1.dp, if (selected) HingeGold else FieldStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(title, color = if (selected) Housing else Ink, fontSize = 16.sp)
        Text(subtitle, color = if (selected) Housing.copy(alpha = 0.75f) else InkMute, fontSize = 11.sp)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedBorderColor = HingeGold,
    unfocusedBorderColor = FieldStroke,
    focusedContainerColor = HousingInset,
    unfocusedContainerColor = HousingInset,
    cursorColor = HingeGold,
)

package dev.ssactinium.phone3dsgamepad.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import dev.ssactinium.phone3dsgamepad.ui.theme.Bezel
import dev.ssactinium.phone3dsgamepad.ui.theme.BrandTitle
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
    Row(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(Modifier.weight(1.05f)) {
            Text("HINGE PAD", style = BrandTitle.copy(color = HingeGold))
            Spacer(Modifier.height(6.dp))
            Text(
                "Phone becomes a handheld-style pad. PC sees an Xbox 360 controller.",
                color = InkMute,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Bezel)
                    .padding(16.dp),
            ) {
                Column {
                    Text("SAME WI-FI AS THE PC", color = HingeGold, fontSize = 11.sp, letterSpacing = 1.6.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Double-click Start-HingePad.bat on the PC.\n2. Enter the PC LAN IP and port 26760.\n3. Pick Xbox games or 3DS / Azahar.\n4. Test, then connect.",
                        color = Ink,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(HousingRaised)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(led))
                Spacer(Modifier.width(8.dp))
                Text(link.detail.ifBlank { "Disconnected" }, style = HudMono.copy(color = Ink))
            }
            Spacer(Modifier.height(16.dp))
            FieldLabel("PC IP address")
            OutlinedTextField(
                value = host,
                onValueChange = onHost,
                singleLine = true,
                placeholder = { Text("192.168.1.10", color = InkMute) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(10.dp))
            FieldLabel("UDP port")
            OutlinedTextField(
                value = port,
                onValueChange = onPort,
                singleLine = true,
                placeholder = { Text("26760", color = InkMute) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )
            Spacer(Modifier.height(10.dp))
            FieldLabel("Control preset")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetChip("Xbox games", profile == ControlProfile.Xbox) { onProfile(ControlProfile.Xbox) }
                PresetChip("3DS / Azahar", profile == ControlProfile.N3ds) { onProfile(ControlProfile.N3ds) }
            }
            if (notice.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(notice, color = if (link.status == LinkStatus.Error) LedDead else Ink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onTest,
                    enabled = !busy,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HingeGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HingeGold),
                ) { Text("Test") }
                Button(
                    onClick = onConnect,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = HingeGold, contentColor = Housing),
                ) { Text("Connect") }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) HingeGold else HousingInset,
            contentColor = if (selected) Housing else Ink,
        ),
    ) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = InkMute, fontSize = 12.sp, letterSpacing = 0.8.sp)
    Spacer(Modifier.height(4.dp))
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

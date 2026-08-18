package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.REMAP_TARGETS
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing
import dev.ssactinium.phone3dsgamepad.ui.theme.HousingRaised
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute

@Composable
fun RemapSheet(
    profile: ControlProfile,
    remap: Map<String, String>,
    onPick: (String, String) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    var editing by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Housing.copy(alpha = 0.96f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Custom map", color = HingeGold, fontSize = 18.sp)
            TextButton(onClick = onClose) { Text("Close", color = Ink) }
        }
        Text(
            if (profile == ControlProfile.N3ds) {
                "Preset: 3DS / Azahar — face buttons follow Nintendo positions."
            } else {
                "Preset: Xbox games — A on the phone is Xbox A."
            },
            color = InkMute,
            fontSize = 13.sp,
        )
        Text(
            "Tap a row to send a different Xbox button. Clear resets to the preset.",
            color = InkMute,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        PadButton.entries.filter { it != PadButton.LSTICK && it != PadButton.RSTICK }.forEach { button ->
            val dest = remap[button.wire] ?: defaultDest(button, profile)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(HousingRaised)
                    .clickable { editing = button.wire }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Phone ${button.wire}", color = Ink, fontSize = 14.sp)
                Text("→  Xbox $dest", color = HingeGold, fontSize = 14.sp)
            }
            if (editing == button.wire) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    REMAP_TARGETS.forEach { target ->
                        Box(
                            Modifier
                                .background(HousingRaised)
                                .clickable {
                                    onPick(button.wire, target)
                                    editing = null
                                }
                                .padding(6.dp),
                        ) {
                            Text(target, color = if (target == dest) HingeGold else Ink, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onClear) { Text("Clear custom map", color = InkMute) }
    }
}

private fun defaultDest(button: PadButton, profile: ControlProfile): String {
    if (profile != ControlProfile.N3ds) return button.wire
    return when (button) {
        PadButton.A -> "B"
        PadButton.B -> "A"
        PadButton.X -> "Y"
        PadButton.Y -> "X"
        else -> button.wire
    }
}

package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceA
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceB
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceX
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceY
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing
import dev.ssactinium.phone3dsgamepad.ui.theme.HousingRaised
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute
import dev.ssactinium.phone3dsgamepad.ui.theme.Shoulder

@Composable
fun RemapSheet(
    profile: ControlProfile,
    remap: Map<String, String>,
    invertStick: Boolean,
    onPick: (String, String) -> Unit,
    onClear: () -> Unit,
    onToggleInvert: () -> Unit,
    onPreset: (ControlProfile) -> Unit,
    onClose: () -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Map buttons", color = HingeGold, fontSize = 20.sp)
            TextButton(onClick = onClose) { Text("Done", color = Ink) }
        }
        Text(
            if (selected == null) "1. Tap a phone button.  2. Tap the Xbox button it should send."
            else "Phone $selected selected — tap the Xbox button it should become.",
            color = Ink,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Preset("Xbox games", profile == ControlProfile.Xbox) { onPreset(ControlProfile.Xbox) }
            Preset("3DS / Azahar", profile == ControlProfile.N3ds) { onPreset(ControlProfile.N3ds) }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (invertStick) HingeGold else HousingRaised)
                .clickable(onClick = onToggleInvert)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                if (invertStick) "Circle Pad Y inverted (Azahar)" else "Circle Pad Y normal — tap to invert",
                color = if (invertStick) Housing else Ink,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PHONE", color = InkMute, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                FaceCluster(
                    selected = selected,
                    onTap = { selected = it },
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SENDS XBOX", color = InkMute, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                FaceCluster(
                    selected = selected?.let { remap[it] ?: defaultDest(it, profile) },
                    onTap = { target ->
                        val src = selected
                        if (src != null) {
                            onPick(src, target)
                            selected = null
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Shoulders", color = InkMute, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("L", "R", "ZL", "ZR", "START", "SELECT").forEach { name ->
                val on = selected == name
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) HingeGold else Shoulder)
                        .clickable { selected = name }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    val dest = remap[name] ?: name
                    Text("$name → $dest", color = if (on) Housing else Ink, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onClear) { Text("Reset custom map", color = InkMute) }
    }
}

@Composable
private fun FaceCluster(
    selected: String?,
    onTap: (String) -> Unit,
) {
    Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Face("X", FaceX, Alignment.TopCenter, selected == "X", onTap)
        Face("A", FaceA, Alignment.CenterEnd, selected == "A", onTap)
        Face("B", FaceB, Alignment.BottomCenter, selected == "B", onTap)
        Face("Y", FaceY, Alignment.CenterStart, selected == "Y", onTap)
    }
}

@Composable
private fun BoxScope.Face(
    label: String,
    color: Color,
    align: Alignment,
    on: Boolean,
    onTap: (String) -> Unit,
) {
    Box(
        Modifier
            .align(align)
            .size(58.dp)
            .clip(CircleShape)
            .background(if (on) color else color.copy(alpha = 0.7f))
            .border(if (on) 3.dp else 0.dp, HingeGold, CircleShape)
            .clickable { onTap(label) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun Preset(label: String, on: Boolean, click: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (on) HingeGold else HousingRaised)
            .clickable(onClick = click)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, color = if (on) Housing else Ink, fontSize = 14.sp)
    }
}

private fun defaultDest(button: String, profile: ControlProfile): String {
    if (profile != ControlProfile.N3ds) return button
    return when (button) {
        "A" -> "B"
        "B" -> "A"
        "X" -> "Y"
        "Y" -> "X"
        else -> button
    }
}

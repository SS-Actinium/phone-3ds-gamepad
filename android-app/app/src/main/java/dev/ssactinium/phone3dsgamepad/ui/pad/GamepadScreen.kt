package dev.ssactinium.phone3dsgamepad.ui.pad

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.network.LinkStatus
import dev.ssactinium.phone3dsgamepad.network.SessionUiState
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import dev.ssactinium.phone3dsgamepad.ui.theme.Bezel
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceA
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceB
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceX
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceY
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeShadow
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing
import dev.ssactinium.phone3dsgamepad.ui.theme.HudMono
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute
import dev.ssactinium.phone3dsgamepad.ui.theme.LedDead
import dev.ssactinium.phone3dsgamepad.ui.theme.LedLive
import dev.ssactinium.phone3dsgamepad.ui.theme.LedWait
import dev.ssactinium.phone3dsgamepad.ui.theme.Screw

@Composable
fun GamepadScreen(
    link: SessionUiState,
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
    onStick: (StickSample) -> Unit,
    onStickRelease: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Housing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HingeRail()
        Spacer(Modifier.height(6.dp))
        StatusRow(link, onDisconnect)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LeftCluster(onPress, onRelease, onStick, onStickRelease)
            CenterWell(onPress, onRelease)
            RightCluster(onPress, onRelease)
        }
    }
}

@Composable
private fun HingeRail() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(HingeShadow),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(HingeGold),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(5) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Screw),
                )
            }
        }
    }
}

@Composable
private fun StatusRow(link: SessionUiState, onDisconnect: () -> Unit) {
    val led = when (link.status) {
        LinkStatus.Connected -> LedLive
        LinkStatus.Connecting, LinkStatus.Degraded -> LedWait
        else -> LedDead
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(led),
            )
            Spacer(Modifier.width(8.dp))
            Text(link.detail, style = HudMono.copy(color = Ink, fontSize = 12.sp))
        }
        TextButton(onClick = onDisconnect) {
            Text("Disconnect", color = InkMute, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LeftCluster(
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
    onStick: (StickSample) -> Unit,
    onStickRelease: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row {
            ShoulderButton("ZL", { onPress(PadButton.ZL) }, { onRelease(PadButton.ZL) }, width = 64.dp, compact = true)
            Spacer(Modifier.width(8.dp))
            ShoulderButton("L", { onPress(PadButton.L) }, { onRelease(PadButton.L) }, width = 86.dp)
        }
        Spacer(Modifier.height(10.dp))
        CirclePad(
            onMove = onStick,
            onRelease = onStickRelease,
            size = 168.dp,
        )
        Spacer(Modifier.height(10.dp))
        DPad(onPressed = onPress, onReleased = onRelease, size = 118.dp)
    }
}

@Composable
private fun CenterWell(
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 28.dp),
    ) {
        Box(
            Modifier
                .width(92.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Bezel),
            contentAlignment = Alignment.Center,
        ) {
            Text("HINGE", color = HingeGold, fontSize = 11.sp, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row {
            TinyButton("SELECT", { onPress(PadButton.SELECT) }, { onRelease(PadButton.SELECT) })
            Spacer(Modifier.width(10.dp))
            TinyButton("START", { onPress(PadButton.START) }, { onRelease(PadButton.START) })
        }
        Spacer(Modifier.height(10.dp))
        TinyButton("HOME", { onPress(PadButton.HOME) }, { onRelease(PadButton.HOME) })
    }
}

@Composable
private fun RightCluster(
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        Row {
            ShoulderButton("R", { onPress(PadButton.R) }, { onRelease(PadButton.R) }, width = 86.dp)
            Spacer(Modifier.width(8.dp))
            ShoulderButton("ZR", { onPress(PadButton.ZR) }, { onRelease(PadButton.ZR) }, width = 64.dp, compact = true)
        }
        Spacer(Modifier.height(18.dp))
        // 3DS diamond: X north, A east, B south, Y west. Letters map 1:1 to Xbox.
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            GameButton(
                "X",
                FaceX,
                { onPress(PadButton.X) },
                { onRelease(PadButton.X) },
                Modifier.align(Alignment.TopCenter),
                72.dp,
            )
            GameButton(
                "A",
                FaceA,
                { onPress(PadButton.A) },
                { onRelease(PadButton.A) },
                Modifier.align(Alignment.CenterEnd),
                72.dp,
            )
            GameButton(
                "Y",
                FaceY,
                { onPress(PadButton.Y) },
                { onRelease(PadButton.Y) },
                Modifier.align(Alignment.CenterStart),
                72.dp,
            )
            GameButton(
                "B",
                FaceB,
                { onPress(PadButton.B) },
                { onRelease(PadButton.B) },
                Modifier.align(Alignment.BottomCenter),
                72.dp,
            )
        }
    }
}

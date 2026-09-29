package com.eirspay.mobile.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType

/**
 * Header for full-screen views: back button, mono eyebrow over a serif title, optional trailing
 * content (status badge, + button). Sits under the status bar on a white surface with a hairline.
 */
@Composable
fun ViewHeader(
    eyebrow: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = Eirs.colors
    Row(
        modifier.fillMaxWidth().background(Color.White).bottomHairline(c.line).statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackButton(onBack)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow.isNotEmpty()) Eyebrow(eyebrow, size = 10.sp, tracking = 0.12)
            title()
        }
        trailing?.invoke(this)
    }
}

/** Serif header title with an optional italic accent: "Register *invoice.*" */
@Composable
fun ViewTitle(plain: String, accent: String = "") {
    Text(serifAccent(plain, accent), style = EirsType.serif(26.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * Full-screen view: header, scrollable body on the tinted background, optional sticky bottom bar.
 * Body content is padded 16dp with 12dp gaps like the prototype.
 */
@Composable
fun ViewScaffold(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Eirs.colors.background,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    bodyPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().background(background)) {
        header()
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bodyPadding)
                .then(if (bottomBar == null) Modifier.navigationBarsPadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = body,
        )
        if (bottomBar != null) StickyBottomBar(content = bottomBar)
    }
}

@Composable
fun StickyBottomBar(modifier: Modifier = Modifier, shadow: Boolean = true, spacing: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = Eirs.colors
    Column(
        modifier.fillMaxWidth()
            .then(if (shadow) Modifier.shadow(12.dp, ambientColor = Color(0x660B2A33), spotColor = Color(0x660B2A33)) else Modifier)
            .background(Color.White).topHairline(c.line).navigationBarsPadding().imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

package com.agentforandroid.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = rememberMarkwon()
    val textColor = if (isUser) android.graphics.Color.WHITE
        else MaterialTheme.colorScheme.onSurface.toArgb()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface,
            tonalElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setPadding(32, 16, 32, 16)
                        setTextIsSelectable(true)
                        textSize = 15f
                        setTextColor(textColor)
                    }
                },
                update = { textView ->
                    markwon.setMarkdown(textView, content)
                },
                modifier = Modifier.wrapContentWidth()
            )
        }
    }
}

@Composable
fun rememberMarkwon(): Markwon {
    val context = LocalContext.current
    return remember {
        Markwon.builder(context).build()
    }
}

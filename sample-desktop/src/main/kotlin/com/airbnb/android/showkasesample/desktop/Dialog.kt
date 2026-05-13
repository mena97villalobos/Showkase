package com.airbnb.android.showkasesample.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.annotation.ShowkaseDialog

@Composable
fun ConfirmDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.width(280.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = TextStyle(fontSize = 14.sp, color = Color.DarkGray),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = dismissLabel,
                    style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                    modifier = Modifier.padding(end = 16.dp),
                )
                Text(
                    text = confirmLabel,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@ShowkaseDialog(name = "Confirm", group = "Dialogs", defaultStyle = true)
@Composable
fun ConfirmDialogPreview() {
    ConfirmDialogContent(
        title = "Delete item?",
        message = "This action cannot be undone.",
        confirmLabel = "Delete",
        dismissLabel = "Cancel",
    )
}

@ShowkaseDialog(
    name = "Confirm",
    group = "Dialogs",
    styleName = "Custom labels",
    buttonText = "Open Confirm Dialog",
    hideButtonText = "Close",
)
@Composable
fun ConfirmDialogCustomLabelsPreview() {
    ConfirmDialogContent(
        title = "Sign out?",
        message = "You will need to log in again to access your account.",
        confirmLabel = "Sign out",
        dismissLabel = "Stay",
    )
}

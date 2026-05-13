package com.airbnb.android.showkasesample.desktop

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.android.showkase.annotation.ShowkaseColor
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.airbnb.android.showkase.annotation.ShowkaseTypography

@ShowkaseComposable(name = "Primary Button", group = "Buttons")
@Composable
fun PrimaryButton() {
    Button(onClick = {}) { Text("Primary") }
}

@ShowkaseComposable(name = "Secondary Card", group = "Cards")
@Composable
fun SecondaryCard() {
    Card(modifier = Modifier.padding(8.dp)) {
        Text(text = "Hello from Desktop", modifier = Modifier.padding(16.dp))
    }
}

@ShowkaseComposable(name = "Disabled Button", group = "Buttons")
@Composable
fun DisabledButton() {
    Button(onClick = {}, enabled = false) { Text("Disabled") }
}

@ShowkaseColor(name = "Brand Red", group = "Brand")
val brandRed = Color(0xFFCC0000)

@ShowkaseColor(name = "Brand Blue", group = "Brand")
val brandBlue = Color(0xFF0044CC)

@ShowkaseTypography(name = "Title", group = "Headings")
val titleStyle = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.SansSerif,
)

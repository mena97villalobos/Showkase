package com.airbnb.android.showkasesample.ios

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

@ShowkaseComposable(name = "Greeting Card", group = "Cards")
@Composable
fun GreetingCard() {
    Card(modifier = Modifier.padding(8.dp)) {
        Text(text = "Hello from iOS", modifier = Modifier.padding(16.dp))
    }
}

@ShowkaseColor(name = "Brand Red", group = "Brand")
val brandRed = Color(0xFFCC0000)

@ShowkaseTypography(name = "Title", group = "Headings")
val titleStyle = TextStyle(
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.SansSerif,
)

package org.mtopol.moodtracker.ui.theme

import androidx.compose.ui.graphics.Color

// Fallback brand palette — used only when Material You dynamic color is
// unavailable (API < 31).
val Indigo80 = Color(0xFFB9C3FF)
val Indigo40 = Color(0xFF4A5BC0)
val Teal80 = Color(0xFF8FD8C8)
val Teal40 = Color(0xFF1F6F62)
val Slate80 = Color(0xFFC6C5D0)
val Slate40 = Color(0xFF5B5D72)

// Explicit, non-dynamic colors for the two chart series and the missed-day
// styling, so they stay distinct regardless of the dynamic color scheme.
val AnxietyLight = Color(0xFFC77700) // amber
val AnxietyDark = Color(0xFFFFB74D)
val DepressionLight = Color(0xFF1565C0) // blue
val DepressionDark = Color(0xFF64B5F6)
val MissedLight = Color(0xFF9E9E9E)
val MissedDark = Color(0xFF707070)
val CompleteLight = Color(0xFF2E7D32) // green
val CompleteDark = Color(0xFF81C784)

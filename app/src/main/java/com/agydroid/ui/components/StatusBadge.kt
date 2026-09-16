package com.agydroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agydroid.data.local.entities.ProjectStatus

@Composable
fun StatusBadge(status: ProjectStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        ProjectStatus.CREATED -> Color(0xFF21262D) to Color(0xFF8B949E)
        ProjectStatus.PLANNING, ProjectStatus.CODING -> Color(0xFF1F3A5A) to Color(0xFF58A6FF)
        ProjectStatus.READY -> Color(0xFF1B4332) to Color(0xFF3FB950)
        ProjectStatus.BUILDING, ProjectStatus.CI_BUILDING, ProjectStatus.UPLOADING -> Color(0xFF382E00) to Color(0xFFD29922)
        ProjectStatus.BUILD_FAILED, ProjectStatus.ERROR -> Color(0xFF490202) to Color(0xFFF85149)
        ProjectStatus.FIXING -> Color(0xFF2D1B4E) to Color(0xFFBC8CFF)
        ProjectStatus.COMPLETED -> Color(0xFF133824) to Color(0xFF56D364)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

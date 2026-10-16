package com.example.coordinategrabber.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.data.model.Location

@Composable
fun LocationRow(
    location: Location,
    index: Int,
    onDelete: (() -> Unit)? = null
) {
    val backgroundColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Lat: ${location.lat}, Lon: ${location.lon}",
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = location.date,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Location",
                    tint = contentColor
                )
            }
        }
    }
}

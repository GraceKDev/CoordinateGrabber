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
            .padding(vertical = 4.dp)
            .background(backgroundColor),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
        ) {
            Text("Lat: ${location.lat}", color = contentColor)
            Text("Lon: ${location.lon}", color = contentColor)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.date,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
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
}

package com.example.coordinategrabber.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.coordinategrabber.R
import com.example.coordinategrabber.data.model.Route

@Composable
fun RouteAccordion(
    modifier: Modifier = Modifier,
    routes: List<Route>,
    onDeleteRoute: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "arrowRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Routes (${routes.size})",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            IconButton(
                onClick = { expanded = !expanded }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_drop_down),
                    contentDescription = if (expanded) "Collapse saved routes" else "Expand saved routes",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = arrowRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (routes.isEmpty()) {
                    Text(
                        text = "No saved routes yet",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    routes.forEach { route ->
                        RouteItem(
                            route = route,
                            onDelete = { onDeleteRoute(route.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteItem(
    route: Route,
    onDelete: () -> Unit
) {
    var pointsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${route.name} • ${route.points.size} pts",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = route.date,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (route.points.isNotEmpty()) {
                    IconButton(
                        onClick = { pointsExpanded = !pointsExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_drop_down),
                            contentDescription = "Show route points",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer(rotationZ = if (pointsExpanded) 180f else 0f)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Route",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        AnimatedVisibility(visible = pointsExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, bottom = 8.dp)
            ) {
                route.points.forEachIndexed { idx, point ->
                    LocationRow(
                        location = point,
                        index = idx,
                        onDelete = null
                    )
                }
            }
        }
    }
}

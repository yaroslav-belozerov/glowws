package com.yaabelozerov.glowws.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.domain.model.PointDomainModel

@Composable
fun IdeaScreen(modifier: Modifier, points: List<PointDomainModel>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (point in points) {
            Point(point.content, point.isMain)
        }
    }
}

@Composable
fun Point(text: String, isMain: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
    ) {
        Text(
            modifier = Modifier.padding(8.dp), text = text
        )
    }
}

@Composable
@Preview
fun IdeaScreenPreview() {
    IdeaScreen(
        modifier = Modifier, points = listOf(
            PointDomainModel("test_point1", false),
            PointDomainModel("test_point2", true),
            PointDomainModel("test_point2", false),
        )
    )
}
package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterModalBottomSheet(mvm: MainScreenViewModel) {
    if (mvm.sortFilterOpen.collectAsState().value) {
        ModalBottomSheet(onDismissRequest = { mvm.toggleSortFilterModal() }) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val flags = mvm.state.collectAsState().value.filter.flags
                if (flags.isNotEmpty()) FilterColumn(
                    flags = flags,
                    setFilterFlag = { f, v -> mvm.setFilterFlag(f, v) },
                    resetFilter = { mvm.resetFilter() }
                )
                SortColumn(
                    sortModel = mvm.state.collectAsState().value.sort,
                    setSortType = { mvm.setSortType(it) },
                    reverseOrder = { mvm.reverseSortOrder() },
                    resetSort = { mvm.fetchSort() }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterColumn(
    flags: Map<FilterFlag, Boolean>,
    setFilterFlag: (FilterFlag, Boolean) -> Unit,
    resetFilter: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.m_filter),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { resetFilter() }) {
                Text(
                    text = stringResource(id = R.string.m_reset_sortfilter),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            flags.forEach {
                if (it.value) {
                    Button(onClick = {
                        setFilterFlag(
                            it.key,
                            !it.value
                        )
                    }) {
                        if (it.value) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "filter applied icon",
                                modifier = Modifier.padding(0.dp, 0.dp, 4.dp, 0.dp)
                            )
                        }
                        Text(text = stringResource(id = it.key.resId), fontSize = 16.sp)
                    }
                } else {
                    OutlinedButton(onClick = {
                        setFilterFlag(
                            it.key,
                            !it.value
                        )
                    }) {
                        if (it.value) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "filter applied icon",
                                modifier = Modifier.padding(0.dp, 0.dp, 4.dp, 0.dp)
                            )
                        }
                        Text(text = stringResource(id = it.key.resId), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortColumn(
    sortModel: SortModel,
    setSortType: (SortType) -> Unit,
    reverseOrder: () -> Unit,
    resetSort: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.m_sort),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { reverseOrder() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedIconButton(onClick = { reverseOrder() }, modifier = Modifier.height(24.dp)) {
                Icon(
                    imageVector = if (sortModel.order == SortOrder.ASCENDING) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp, 0.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { resetSort() }) {
                Text(
                    text = stringResource(id = R.string.m_reset_sortfilter),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortType.entries.forEach {
                if (sortModel.type == it) {
                    Button(onClick = {
                        resetSort()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "filter applied icon",
                            modifier = Modifier.padding(0.dp, 0.dp, 4.dp, 0.dp)
                        )
                        Text(text = stringResource(it.resId), fontSize = 16.sp)
                    }
                } else {
                    OutlinedButton(onClick = {
                        setSortType(it)
                    }) {
                        Text(text = stringResource(it.resId), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

package com.yaabelozerov.glowws

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsList
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.domain.model.BooleanSettingDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.model.SortModel
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.settings.BooleanSettingsEntry
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Inject

class UnitTest {
  @Test
  fun `Test SettingsMapper toDomainModel + matchSettingsSchema`() {
    val mapper = SettingsMapper()
    val out = mapper.toDomainModel(
      mapper.matchSettingsSchema(
        SettingsList(
          listOf(
            SettingsModel(
              key = SettingsKeys.MONET_THEME,
              value = "true"
            )
          )
        )
      )
    )
    val exp = mapOf<SettingsKeys, SettingDomainModel>(
      SettingsKeys.MONET_THEME to BooleanSettingDomainModel(SettingsKeys.MONET_THEME, true)
    )
    assertEquals(exp[SettingsKeys.MONET_THEME], out[SettingsKeys.MONET_THEME])
  }

  @Test
  fun `Test SettingsMapper getSorting`() {
    val mapper = SettingsMapper()
    val (outOrder, outType) = mapper.getSorting(SettingsList(listOf(SettingsModel(SettingsKeys.SORT_TYPE, SortType.PRIORITY.name), SettingsModel(SettingsKeys.SORT_ORDER, SortOrder.ASCENDING.name))))
    assertEquals(outType, SortType.PRIORITY)
    assertEquals(outOrder, SortOrder.ASCENDING)
  }

  @Test
  fun `Test toReadableKey`() {
    val cases = listOf(
      "MONET_THEME" to "Monet theme",
      "SORT_TYPE" to "Sort type",
      "SORT_ORDER" to "Sort order"
    )
    cases.forEach {
      assertEquals(it.first.toReadableKey(), it.second)
    }
  }
}

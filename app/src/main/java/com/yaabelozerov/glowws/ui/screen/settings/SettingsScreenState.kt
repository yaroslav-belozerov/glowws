package com.yaabelozerov.glowws.ui.screen.settings

import com.yaabelozerov.glowws.domain.model.SettingDomainModel

data class SettingsScreenState(
    val settings: Map<String, List<SettingDomainModel>> = emptyMap()
)

package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.datastore.model.SettingsModel

enum class SettingType(val type: String) {
    BOOLEAN("boolean"), STRING("string"), DOUBLE("double")
}

abstract class SettingDomainModel {
    abstract val key: SettingsKeys
    abstract val name: String
    abstract val value: Any
}

data class BooleanSettingDomainModel(
    override val key: SettingsKeys,
    override val name: String,
    override val value: Boolean
) : SettingDomainModel()

data class StringSettingDomainModel(
    override val key: SettingsKeys,
    override val name: String,
    override val value: String
) : SettingDomainModel()

data class DoubleSettingDomainModel(
    override val key: SettingsKeys,
    override val name: String,
    val min: Double,
    val max: Double,
    override val value: Double
) : SettingDomainModel()

data class ChoiceSettingDomainModel(
    override val key: SettingsKeys,
    override val name: String,
    val choices: List<String>,
    override val value: String,
): SettingDomainModel()
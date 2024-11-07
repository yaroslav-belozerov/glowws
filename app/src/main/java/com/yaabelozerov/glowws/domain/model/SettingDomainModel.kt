package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys

interface SettingDomainModel {
  val key: SettingsKeys
  val value: Any
}

data class BooleanSettingDomainModel(override val key: SettingsKeys, override val value: Boolean) :
    SettingDomainModel

data class StringSettingDomainModel(override val key: SettingsKeys, override val value: String) :
    SettingDomainModel

data class DoubleSettingDomainModel(
    override val key: SettingsKeys,
    val min: Double,
    val max: Double,
    override val value: Double
) : SettingDomainModel

data class ChoiceSettingDomainModel(
    override val key: SettingsKeys,
    val choices: List<String>,
    val localChoicesIds: List<Int?>,
    override val value: String,
) : SettingDomainModel

data class MultipleChoiceSettingDomainModel(
    override val key: SettingsKeys,
    val choices: List<String>,
    val localChoicesIds: List<Int?>,
    override val value: List<Boolean>,
) : SettingDomainModel

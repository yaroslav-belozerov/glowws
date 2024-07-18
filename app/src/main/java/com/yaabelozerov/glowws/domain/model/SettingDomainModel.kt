package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys

abstract class SettingDomainModel {
    abstract val key: SettingsKeys
    abstract val nameRes: Int
    abstract val value: Any
}

fun List<SettingDomainModel>.findBooleanKey(key: SettingsKeys) = findLast { it.key == key }?.value.toString() == "true"

data class BooleanSettingDomainModel(
    override val key: SettingsKeys,
    override val nameRes: Int,
    override val value: Boolean
) : SettingDomainModel()

data class StringSettingDomainModel(
    override val key: SettingsKeys,
    override val nameRes: Int,
    override val value: String
) : SettingDomainModel()

data class DoubleSettingDomainModel(
    override val key: SettingsKeys,
    override val nameRes: Int,
    val min: Double,
    val max: Double,
    override val value: Double
) : SettingDomainModel()

data class ChoiceSettingDomainModel(
    override val key: SettingsKeys,
    override val nameRes: Int,
    val choices: List<String>,
    val localChoicesIds: List<Int?>,
    override val value: String,
): SettingDomainModel()

data class MultipleChoiceSettingDomainModel(
    override val key: SettingsKeys,
    override val nameRes: Int,
    val choices: List<String>,
    val localChoicesIds: List<Int?>,
    override val value: List<Boolean>,
): SettingDomainModel()
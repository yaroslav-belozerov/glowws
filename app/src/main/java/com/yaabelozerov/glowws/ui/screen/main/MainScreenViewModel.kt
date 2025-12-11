package com.yaabelozerov.glowws.ui.screen.main

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.SignInReq
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.IdeaModel
import com.yaabelozerov.glowws.domain.model.IdeaModelFull
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenEvent
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.reflect.KClass


sealed interface LoginState {
    object Loading : LoginState
    object Success : LoginState
    object AccountCreated : LoginState
    data class Error(val message: String) : LoginState
    object Init : LoginState
}

@HiltViewModel
class MainScreenViewModel
@Inject constructor(
    @ApplicationContext private val app: Context,
    private val settingsMapper: SettingsMapper,
    private val settingsManager: SettingsManager,
    private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {
    private val _state = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    private val _filter = MutableStateFlow(MainScreenFilterState())
    val filter = _filter.asStateFlow()

    private val _select: MutableStateFlow<SelectionState<Long>> = MutableStateFlow(SelectionState())
    val select = _select.asStateFlow()

    private val _archiveSelect: MutableStateFlow<SelectionState<Long>> = MutableStateFlow(SelectionState())
    val archiveSelect = _archiveSelect.asStateFlow()

    private val _isSortFilterOpen = MutableStateFlow(false)
    val sortFilterOpen = _isSortFilterOpen.asStateFlow()

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen = _searchOpen.asStateFlow()

    val jwt = dataStoreManager.jwt()
    val instanceUrl = dataStoreManager.instanceUrl()
    val login = dataStoreManager.login()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Init)
    val loginState = _loginState.asStateFlow()

    fun login(instanceUrl: String, username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val url = if (instanceUrl.startsWith("localhost")) {
                "http://${instanceUrl}"
            } else if (instanceUrl.startsWith("http://") || instanceUrl.startsWith("https://")) {
                instanceUrl
            } else {
                "https://${instanceUrl}"
            }
            try {
                val response = Net.httpClient.post("${url}/sign-in") {
                    setBody(SignInReq(username, password))
                }
                if (response.status.value == 200) {
                    val token: String = response.body()
                    _loginState.update { LoginState.Success }
                    delay(500)
                    dataStoreManager.setInstanceUrl(url)
                    dataStoreManager.setLogin(username)
                    dataStoreManager.setJwt(token)
                    _loginState.update { LoginState.Init }
                } else {
                    _loginState.update { LoginState.Error(response.toString()) }
                }
            } catch (e: ClientRequestException) {
                when (e.response.status.value) {
                    404 -> {
                        val response = Net.httpClient.post("${url}/sign-up") {
                            setBody(SignInReq(username, password))
                        }
                        if (response.status.value == 200) {
                            val token: String = response.body()
                            _loginState.update { LoginState.AccountCreated }
                            delay(1000)
                            dataStoreManager.setInstanceUrl(url)
                            dataStoreManager.setLogin(username)
                            dataStoreManager.setJwt(token)
                            _loginState.update { LoginState.Init }
                        } else {
                            _loginState.update { LoginState.Error(response.toString()) }
                        }
                    }

                    else -> {
                        _loginState.update { LoginState.Error(e.response.toString()) }
                    }
                }
            } catch (e: Throwable) {
                _loginState.update { LoginState.Error(e.message ?: e.toString()) }
                e.printStackTrace()
            }
        }
    }

    fun appFirstVisit(callback: suspend () -> Unit = {}) {
        viewModelScope.launch {
            when (settingsManager.getAppVisits().also { Log.i("App visits", it.toString()) }) {
                0L -> callback()
                else -> {}
            }
            settingsManager.visitApp()
        }
    }

    fun resetFilter() {
        _filter.update { it.copy(filter = MainScreenFilterState().filter) }
    }

    fun fetchSort() = viewModelScope.launch {
        _filter.update { it.copy(sort = settingsMapper.getSorting(settingsManager.fetchSettings())) }
    }

    fun toggleSortFilterModal() = _isSortFilterOpen.update { !_isSortFilterOpen.value }

    fun fetchMainScreen() {
        viewModelScope.launch {
            jwt.collect { token ->
                _filter.collect { q ->
                    update(token, q)
                }
            }
        }
    }

    private suspend fun update(token: String, q: MainScreenFilterState) {
        Net.get<List<IdeaModelFull>>(
            instanceUrl, "ideas", token, q.buildParams()
        ).onSuccess { ideas -> _state.update { it.setIdeas(ideas, instanceUrl.first()) } }
            .onFailure(Throwable::printStackTrace)
    }

    fun updateFilterFlag(flagType: KClass<FilterFlag>, flag: FilterFlag) {
        _filter.update { state ->
            state.copy(filter = state.filter + (flagType to flag))
        }
    }

    fun setSortType(type: SortType) = _filter.update { it.copy(sort = it.sort.copy(type = type)) }

    private fun setSortOrder(order: SortOrder) = _filter.update { it.copy(sort = it.sort.copy(order = order)) }

    fun reverseSortOrder() = setSortOrder(_filter.value.sort.order.reversed())

    private fun toggleArchiveIdea(ideaId: Long, isArchive: Boolean) {
        viewModelScope.launch {
            Net.put<List<IdeaModelFull>>(
                baseUrlFlow = instanceUrl,
                path = "ideas/archive/$ideaId",
                token = jwt.first(),
                _filter.value.buildParams(isArchive)
            ).onSuccess {
                _state.update { state -> state.setIdeas(it, instanceUrl.first()) }
            }.onFailure(Throwable::printStackTrace)
        }
    }

    private fun setPriority(ideaId: Long, priority: Long) {
        viewModelScope.launch {
            Net.put<List<IdeaModelFull>>(
                baseUrlFlow = instanceUrl,
                path = "ideas/$ideaId/priority/$priority",
                token = jwt.first(),
                params = _filter.value.buildParams()
            ).onSuccess {
                _state.update { state -> state.setIdeas(it, instanceUrl.first()) }
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun addNewIdea(callback: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            Net.post<IdeaModel>(instanceUrl, "ideas", jwt.first()).onSuccess {
                callback?.invoke(it.id)
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun toggleArchiveSelected(isArchive: Boolean) {
        viewModelScope.launch {
            val entries = (if (isArchive) _archiveSelect else _select).value.entries
            Net.put<List<IdeaModelFull>, List<Long>>(
                baseUrlFlow = instanceUrl,
                path = "ideas/archive/all",
                token = jwt.first(),
                reqBody = entries,
                params = _filter.value.buildParams(isArchive)
            ).onSuccess {
                _state.update { state -> state.setIdeas(it, instanceUrl.first()) }
                deselectAll()
                deselectAllArchive()
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun deselectAll() = _select.update { SelectionState() }
    fun deselectAllArchive() = _archiveSelect.update { SelectionState() }

    fun selectAllArchive() =
        _archiveSelect.update { it.copy(inSelectionMode = true, entries = _state.value.archivedIdeas.map { it.id }) }

    fun removeSelected() {
        viewModelScope.launch {
            Net.delete<List<IdeaModelFull>, List<Long>>(
                baseUrlFlow = instanceUrl,
                path = "ideas/all",
                token = jwt.first(),
                reqBody = _archiveSelect.value.entries
            ).onSuccess {
                deselectAll()
                deselectAllArchive()
                _state.update { state -> state.setIdeas(it, instanceUrl.first()) }
            }.onFailure(Throwable::printStackTrace)
        }
    }

    fun updateSearchQuery(newQuery: String) = _filter.update { it.copy(searchQuery = newQuery) }

    fun setSearch(open: Boolean) = _searchOpen.update { open }

    fun onEvent(event: MainScreenEvent, navCtrl: NavController, ivm: IdeaScreenViewModel) {
        when (event) {
            is MainScreenEvent.NavigateToFeedback -> navCtrl.navigate(Nav.FeedbackRoute.route)
            is MainScreenEvent.OpenIdea -> {
                navCtrl.navigate(Nav.IdeaScreenRoute.withParam(event.id))
                ivm.refreshPoints(event.id)
            }

            is MainScreenEvent.ArchiveIdea -> {
                toggleArchiveIdea(event.id, false)
            }

            is MainScreenEvent.SelectIdea -> {
                _select.update { it.select(event.id) }
            }

            is MainScreenEvent.SetPriority -> {
                setPriority(event.id, event.priority)
            }
        }
    }

    fun onArchiveEvent(event: ArchiveScreenEvent, navCtrl: NavController, ivm: IdeaScreenViewModel) {
        when (event) {
            is ArchiveScreenEvent.Open -> {
                navCtrl.navigate(Nav.IdeaScreenRoute.withParam(event.id))
                ivm.refreshPoints(event.id)
            }

            is ArchiveScreenEvent.Remove -> viewModelScope.launch {
                Net.delete<List<IdeaModelFull>>(instanceUrl, "ideas/${event.id}", jwt.first()).onSuccess {
                    _state.update { state -> state.setIdeas(it, instanceUrl.first()) }
                }.onFailure(Throwable::printStackTrace)
            }

            is ArchiveScreenEvent.Select -> _archiveSelect.update { it.select(event.id) }
            is ArchiveScreenEvent.Unarchive -> toggleArchiveIdea(event.id, true)
        }
    }
}

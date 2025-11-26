package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.yaabelozerov.glowws.Net
import com.yaabelozerov.glowws.SignInReq
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.AppModule
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.domain.model.IdeaModel
import com.yaabelozerov.glowws.domain.model.archived
import com.yaabelozerov.glowws.domain.model.notArchived
import com.yaabelozerov.glowws.domain.model.toDomain
import com.yaabelozerov.glowws.domain.model.toDomainModel
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@HiltViewModel
class MainScreenViewModel
@Inject constructor(
  private val dao: IdeaDao,
  private val ideaMapper: IdeaMapper,
  private val settingsMapper: SettingsMapper,
  private val settingsManager: SettingsManager,
  private val dataStoreManager: AppModule.DataStoreManager
) : ViewModel() {
  private val _state = MutableStateFlow(MainScreenState())
  val state = _state.asStateFlow()

  private val _filterState = MutableStateFlow(MainScreenFilterState())
  val filterState = _filterState.asStateFlow()

  private val _selectionState: MutableStateFlow<SelectionState<Long>> = MutableStateFlow(SelectionState())
  val selection = _selectionState.asStateFlow()

  private val _isSortFilterOpen = MutableStateFlow(false)
  val sortFilterOpen = _isSortFilterOpen.asStateFlow()

  private val _searchOpen = MutableStateFlow(false)
  val searchOpen = _searchOpen.asStateFlow()

  val jwt = dataStoreManager.jwt()
  val instanceUrl = dataStoreManager.instanceUrl()
  private val _loginErr = MutableStateFlow("")
  val loginErr = _loginErr.asStateFlow()

  fun login(instanceUrl: String, username: String, password: String) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val response = Net.httpClient.post("${instanceUrl}/sign-in") {
          setBody(SignInReq(username, password))
        }
        if (response.status.value == 200) {
          val token: String = response.body()
          dataStoreManager.setJwt(token)
          dataStoreManager.setInstanceUrl(instanceUrl)
          _loginErr.update { "" }
        } else {
          _loginErr.update { response.toString() }
        }
      } catch (e: Throwable) {
        _loginErr.update { e.message ?: e.toString() }
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
    _filterState.update { it.copy(filter = MainScreenFilterState().filter) }
  }

  fun fetchSort() = viewModelScope.launch {
    _filterState.update { it.copy(sort = settingsMapper.getSorting(settingsManager.fetchSettings())) }
  }

  fun toggleSortFilterModal() = _isSortFilterOpen.update { !_isSortFilterOpen.value }

  fun fetchMainScreen() {
    viewModelScope.launch {
      jwt.collect { token ->
        _filterState.collect { q ->
          Net.get<List<IdeaModel>>(
            instanceUrl, "ideas" + (if (q.searchQuery.isBlank()) "" else "?search=${q.searchQuery}"), token
          ).onSuccess { ideas -> _state.update { it.setIdeas(ideas) } }.onFailure(Throwable::printStackTrace)
        }
      }
    }
  }

  fun updateFilterFlag(flagType: KClass<FilterFlag>, flag: FilterFlag) {
    _filterState.update { state ->
      state.copy(filter = state.filter + (flagType to flag))
    }
  }

  fun setSortType(type: SortType) = _filterState.update { it.copy(sort = it.sort.copy(type = type)) }

  private fun setSortOrder(order: SortOrder) = _filterState.update { it.copy(sort = it.sort.copy(order = order)) }

  fun reverseSortOrder() = setSortOrder(_filterState.value.sort.order.reversed())

  private fun archiveIdea(ideaId: Long) {
    viewModelScope.launch {
      Net.put<List<IdeaModel>>(instanceUrl, "ideas/archive/$ideaId", jwt.first()).onSuccess {
        _state.update { state -> state.setIdeas(it) }
      }.onFailure(Throwable::printStackTrace)
    }
  }

  private fun setPriority(ideaId: Long, priority: Long) {
    viewModelScope.launch {
      Net.put<List<IdeaModel>>(instanceUrl, "ideas/$ideaId/priority/$priority", jwt.first()).onSuccess {
        _state.update { state -> state.setIdeas(it) }
      }.onFailure(Throwable::printStackTrace)
    }
  }

  fun addNewIdea(callback: ((Long) -> Unit)? = null) {
    viewModelScope.launch {
      Net.post<IdeaModel>(instanceUrl, "ideas", jwt.first()).onSuccess {
        fetchMainScreen()
        callback?.invoke(it.id)
      }.onFailure(Throwable::printStackTrace)
    }
  }

  fun archiveSelected() {
    viewModelScope.launch {
      Net.put<List<IdeaModel>, List<Long>>(
        instanceUrl, "ideas/archive/all", jwt.first(), _selectionState.value.entries
      ).onSuccess {
        _state.update { state -> state.setIdeas(it) }
        _selectionState.update { SelectionState() }
      }.onFailure(Throwable::printStackTrace)
    }
  }

  fun deselectAll() = _selectionState.update { SelectionState() }

  private fun onSelect(ideaId: Long) = _selectionState.update { it.select(ideaId) }

  fun updateSearchQuery(newQuery: String) = _filterState.update { it.copy(searchQuery = newQuery) }

  fun setSearch(open: Boolean) = _searchOpen.update { open }

  fun onEvent(event: MainScreenEvent, navCtrl: NavController, ivm: IdeaScreenViewModel) {
    when (event) {
      is MainScreenEvent.NavigateToFeedback -> navCtrl.navigate(Nav.FeedbackRoute.route)
      is MainScreenEvent.OpenIdea -> {
        navCtrl.navigate(Nav.IdeaScreenRoute.withParam(event.id))
        ivm.refreshPoints(event.id)
      }

      is MainScreenEvent.ArchiveIdea -> {
        archiveIdea(event.id)
      }

      is MainScreenEvent.SelectIdea -> {
        onSelect(event.id)
      }

      is MainScreenEvent.SetPriority -> {
        setPriority(event.id, event.priority)
      }
    }
  }
}

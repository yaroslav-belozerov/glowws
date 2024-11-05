package com.yaabelozerov.glowws

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.remote.PreloadModelsService
import com.yaabelozerov.glowws.ui.common.BottomNavBar
import com.yaabelozerov.glowws.ui.common.FloatingActionButtons
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.screen.ai.AiScreenViewModel
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.App
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.SortFilterModalBottomSheet
import com.yaabelozerov.glowws.ui.screen.main.booleanOrNull
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.moshi.MoshiConverterFactory

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val mvm: MainScreenViewModel by viewModels()
    val ivm: IdeaScreenViewModel by viewModels()
    val svm: SettingsScreenViewModel by viewModels()
    val avm: ArchiveScreenViewModel by viewModels()
    val aivm: AiScreenViewModel by viewModels()

    aivm.setOnPickModel {
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { aivm.importLocalModel(uri) }
          }
          .launch(arrayOf("application/octet-stream"))
    }

    ivm.setOnPickMedia {
      registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { ivm.viewModelScope.launch { ivm.importImage(uri) } }
          }
          .launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    setContent {
      val navCtrl = rememberNavController()

      val snackbarHostState = remember { SnackbarHostState() }
      val scope = rememberCoroutineScope()
      val text = stringResource(R.string.app_welcome)
      LaunchedEffect(true) {
        mvm.appFirstVisit {
          snackbarHostState.showSnackbar(message = text, duration = SnackbarDuration.Short)
          try {
            Retrofit.Builder()
                .baseUrl(Const.Net.MODEL_PRELOAD_BASE_URL)
                .addConverterFactory(
                    MoshiConverterFactory.create(
                        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
                .build()
                .create(PreloadModelsService::class.java)
                .getModels()
                .awaitResponse()
                .also { Log.i("got", it.body().toString()) }
                .body()
                ?.let { aivm.importRemoteModels(it) }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
      val dynamicColor = svm.state.collectAsState().value[SettingsKeys.MONET_THEME].booleanOrNull()
      if (dynamicColor != null) {
        GlowwsTheme(dynamicColor = dynamicColor.toString().toBoolean()) {
          Scaffold(
              modifier = Modifier.fillMaxSize(),
              content = { innerPadding ->
                App(
                    innerPadding = innerPadding,
                    navCtrl = navCtrl,
                    startDestination = Nav.MainScreenRoute,
                    mvm = mvm,
                    ivm = ivm,
                    svm = svm,
                    avm = avm,
                    aivm = aivm,
                    snackbar = Pair(snackbarHostState, scope))

                SortFilterModalBottomSheet(mvm = mvm)
              },
              floatingActionButton = { FloatingActionButtons(navCtrl, mvm, ivm, avm) },
              bottomBar = { BottomNavBar(navCtrl) },
          )
        }
      }
    }
  }
}

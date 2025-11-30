package com.yaabelozerov.glowws

import android.content.ClipData
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.ui.common.BottomNavBar
import com.yaabelozerov.glowws.ui.common.FloatingActionButtons
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.screen.ai.AiScreenViewModel
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.App
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.SortFilterModalBottomSheet
import com.yaabelozerov.glowws.ui.screen.main.booleanOrNull
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.collections.getValue

@Serializable
data class SignInReq(
  val username: String, val password: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val mvm: MainScreenViewModel by viewModels()
    val ivm: IdeaScreenViewModel by viewModels()
    val svm: SettingsScreenViewModel by viewModels()
    val aivm: AiScreenViewModel by viewModels()

    val onPickModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let { aivm.importLocalModel(uri) }
    }
    aivm.setOnPickModel(onPickModel = { onPickModel.launch(arrayOf("application/octet-stream")) })

    val onPickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let { ivm.viewModelScope.launch { ivm.importImage(uri) } }
    }
    ivm.setOnPickMedia(onPickMedia = { onPickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })

    setContent {
      val navCtrl = rememberNavController()

      val snackbarHostState = remember { SnackbarHostState() }
      val scope = rememberCoroutineScope()
      val text = stringResource(R.string.app_welcome)
      LaunchedEffect(true) {
        mvm.appFirstVisit {
          snackbarHostState.showSnackbar(message = text, duration = SnackbarDuration.Short)
        }
      }
      val settings by svm.state.collectAsState()
      val dynamicColor = settings[SettingsKeys.MONET_THEME].booleanOrNull()
      val instanceUrl by mvm.instanceUrl.collectAsState(initial = "");
      val token by mvm.jwt.collectAsState(initial = "");
      if (dynamicColor != null) {
        GlowwsTheme(dynamicColor = dynamicColor.toString().toBoolean()) {
          if (instanceUrl.isNotBlank() && token.isNotBlank()) {
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
                  aivm = aivm,
                  snackbar = Pair(snackbarHostState, scope),
                  onError = { e ->
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
                  })

                SortFilterModalBottomSheet(mvm = mvm)
              },
              floatingActionButton = { FloatingActionButtons(navCtrl, mvm, ivm) },
              bottomBar = { BottomNavBar(navCtrl) },
            )
          } else {
            Box(
              modifier = Modifier.fillMaxSize()
            ) {
              Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .align(
                    Alignment.Center
                  )
                  .imePadding()
              ) {
                var url by remember { mutableStateOf("") }
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                Column {
                  OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    shape = MaterialTheme.shapes.medium,
                    label = { Text(text = stringResource(R.string.instance_url)) })
                  OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    shape = MaterialTheme.shapes.medium,
                    label = { Text(text = stringResource(R.string.username)) })
                  OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    shape = MaterialTheme.shapes.medium,
                    label = { Text(text = stringResource(R.string.password)) })
                  Button(
                    onClick = { mvm.login(url, username, password) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.End)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Text(stringResource(R.string.login))
                      Icon(Icons.AutoMirrored.Default.ArrowRight, null)
                    }
                  }
                }
                mvm.loginErr.collectAsState().value.takeIf { it.isNotBlank() }?.let {
                  val clip = LocalClipboard.current
                  Card(onClick = {
                    scope.launch {
                      clip.setClipEntry(ClipEntry(ClipData.newPlainText("Glowws Error", it)))
                    }
                  }, modifier = Modifier.padding(16.dp)) {
                    Text(it, modifier = Modifier.padding(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

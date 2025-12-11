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
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.yaabelozerov.glowws.ui.screen.main.LoginState
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.SortFilterModalBottomSheet
import com.yaabelozerov.glowws.ui.screen.main.booleanOrNull
import com.yaabelozerov.glowws.ui.screen.settings.SettingsScreenViewModel
import com.yaabelozerov.glowws.ui.theme.GlowwsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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
      val login by mvm.login.collectAsState(initial = "");
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
            Surface(
              modifier = Modifier.fillMaxSize()
            ) {
              val loginState by mvm.loginState.collectAsState()
              AnimatedContent(loginState, modifier = Modifier.fillMaxSize()) {
                Box(
                  modifier = Modifier.fillMaxSize()
                ) {
                  when (it) {
                    is LoginState.Error -> LoginForm(mvm, login, instanceUrl, it.message)
                    LoginState.AccountCreated -> {
                      Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(
                          Alignment.Center
                        ).imePadding()
                      ) {
                        Icon(
                          Icons.Default.Check, null, modifier = Modifier.padding(16.dp).size(48.dp)
                        )
                        Text(stringResource(R.string.account_successfully_created), style = LocalTextStyle.current.copy(fontStyle = FontStyle.Italic))
                      }
                    }
                    LoginState.Init -> LoginForm(mvm, login, instanceUrl)
                    LoginState.Loading -> {
                      CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(48.dp).align(Alignment.Center))
                    }
                    LoginState.Success -> {
                      Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(
                          Alignment.Center
                        ).imePadding()
                      ) {
                        Icon(
                          Icons.Default.Check, null, modifier = Modifier.padding(16.dp).size(48.dp)
                        )
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
  }
}


@Composable
private fun BoxScope.LoginForm(mvm: MainScreenViewModel, username: String, instanceUrl: String, err: String? = null) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.align(
      Alignment.Center
    ).imePadding()
  ) {
    val scope = rememberCoroutineScope()
    var mutatingUrl by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf(instanceUrl.ifBlank { "gwws.tarakoshka.tech" }) }
    var username by remember { mutableStateOf(username) }
    var password by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    Column(modifier = Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 16.dp)
      )
      if (mutatingUrl) {
        var tempUrl by remember { mutableStateOf(url) }
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().focusRequester(fr),
          value = tempUrl,
          onValueChange = { tempUrl = it },
          shape = MaterialTheme.shapes.medium,
          label = { Text(text = stringResource(R.string.instance_url)) },
          trailingIcon = {
            IconButton(onClick = {
              url = tempUrl
              mutatingUrl = false
            }) {
              Icon(Icons.Default.Save, null)
            }
          })
      } else {
        OutlinedCard(
          onClick = { mutatingUrl = true; fr.requestFocus() },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = stringResource(R.string.connect_on), modifier = Modifier, style = MaterialTheme.typography.labelMedium
              )
              Text(
                text = url.ifBlank { "URL..." }, modifier = Modifier
              )
            }
            Icon(
              Icons.Default.Edit,
              contentDescription = null,
              modifier = Modifier.padding(start = 4.dp, end = 16.dp)
            )
          }
        }
      }
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = username,
        onValueChange = { username = it },
        shape = MaterialTheme.shapes.medium,
        label = { Text(text = stringResource(R.string.username)) })
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = password,
        onValueChange = { password = it },
        shape = MaterialTheme.shapes.medium,
        label = { Text(text = stringResource(R.string.password)) })
      Button(
        onClick = { mvm.login(url, username, password) },
        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
        enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && !mutatingUrl
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(stringResource(R.string.login))
          Icon(Icons.AutoMirrored.Default.ArrowRight, null)
        }
      }
      err?.let { message ->
        val clip = LocalClipboard.current
        Card(onClick = {
          scope.launch {
            clip.setClipEntry(ClipEntry(ClipData.newPlainText("Glowws Error", message)))
          }
        }, modifier = Modifier.padding(16.dp)) {
          Text(message, modifier = Modifier.padding(16.dp))
        }
      }
    }
  }
}
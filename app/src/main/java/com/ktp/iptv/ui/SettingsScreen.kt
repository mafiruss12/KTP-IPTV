package com.ktp.iptv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onLoadDefault: () -> Unit,
    onLoadM3u: (String) -> Unit,
    onLoadXtream: (String, String, String) -> Unit,
    onProxyChange: (Boolean, String, String) -> Unit,
    onCheckUpdate: () -> Unit = {},
    onDownloadUpdate: () -> Unit = {}
) {
    var m3uUrl by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }
    var proxyHost by remember { mutableStateOf(state.proxyHost) }
    var proxyPort by remember { mutableStateOf(state.proxyPort) }
    var proxyEnabled by remember { mutableStateOf(state.proxyEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Source publique iptv-org", style = MaterialTheme.typography.titleMedium)
            Text(
                "Collection de chaînes publiques (https://github.com/iptv-org/iptv). " +
                        "Les flux sont fournis tels quels ; aucun contenu n’est hébergé par l’app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onLoadDefault, modifier = Modifier.fillMaxWidth()) {
                Text("Charger playlist mondiale (index.m3u)")
            }

            Spacer(Modifier.height(8.dp))
            Text("Playlist M3U personnalisée", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = m3uUrl,
                onValueChange = { m3uUrl = it },
                label = { Text("URL M3U / M3U8") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { if (m3uUrl.isNotBlank()) onLoadM3u(m3uUrl.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = m3uUrl.isNotBlank()
            ) {
                Text("Charger M3U")
            }

            Spacer(Modifier.height(8.dp))
            Text("Compte Xtream Codes", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = xtreamServer,
                onValueChange = { xtreamServer = it },
                label = { Text("Serveur (http://host:port)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = xtreamUser,
                onValueChange = { xtreamUser = it },
                label = { Text("Utilisateur") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = xtreamPass,
                onValueChange = { xtreamPass = it },
                label = { Text("Mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank()) {
                        onLoadXtream(xtreamServer.trim(), xtreamUser.trim(), xtreamPass)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = xtreamServer.isNotBlank() && xtreamUser.isNotBlank()
            ) {
                Text("Connexion Xtream")
            }

            Spacer(Modifier.height(16.dp))
            
            Text("Mise à jour de l'application", style = MaterialTheme.typography.titleMedium)
            Text(
                "Vérifie automatiquement les nouvelles versions publiées sur GitHub (releases).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.updateChecking && !state.updateDownloading
            ) {
                Text(if (state.updateChecking) "Vérification…" else "Vérifier les mises à jour")
            }
            state.updateInfo?.let { info ->
                Text(
                    "Nouvelle version disponible : ${info.versionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (info.changelog.isNotBlank()) {
                    Text(info.changelog, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = onDownloadUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.updateDownloading
                ) {
                    Text(
                        if (state.updateDownloading)
                            "Téléchargement ${state.updateProgress}%"
                        else
                            "Télécharger et installer"
                    )
                }
            }
            state.updateMessage?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Text("Proxy (HTTP/HTTPS)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Active un proxy pour contourner les blocages ou les problèmes cleartext/HTTPS. " +
                        "L’app autorise déjà le trafic HTTP clair via networkSecurityConfig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Utiliser un proxy", modifier = Modifier.weight(1f))
                Switch(
                    checked = proxyEnabled,
                    onCheckedChange = {
                        proxyEnabled = it
                        onProxyChange(it, proxyHost, proxyPort)
                    }
                )
            }
            OutlinedTextField(
                value = proxyHost,
                onValueChange = {
                    proxyHost = it
                    if (proxyEnabled) onProxyChange(true, it, proxyPort)
                },
                label = { Text("Hôte proxy") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = proxyEnabled
            )
            OutlinedTextField(
                value = proxyPort,
                onValueChange = {
                    proxyPort = it
                    if (proxyEnabled) onProxyChange(true, proxyHost, it)
                },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = proxyEnabled
            )
        }
    }
}

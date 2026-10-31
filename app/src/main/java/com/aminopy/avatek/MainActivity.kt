package com.aminopy.avatek

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.aminopy.avatek.state.AvatekViewModel
import com.aminopy.avatek.ui.AvatekApp

class MainActivity : ComponentActivity() {
    private val vm: AvatekViewModel by viewModels()

    private val lyricsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.importLyrics(uri)
    }
    private val backupExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) vm.writeBackup(uri)
    }
    private val backupImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) vm.readBackup(uri)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        vm.onPermissionResult(results[audioPermission()] == true || hasAudioPermission())
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvatekApp(
                vm,
                requestPermission = ::requestPermissions,
                requestLyricsImport = ::requestLyricsImport,
                requestBackupExport = ::requestBackupExport,
                requestBackupImport = ::requestBackupImport,
                requestNotificationPermission = ::requestNotificationPermission,
                openNotificationSettings = ::openNotificationSettings
            )
        }
        if (savedInstanceState == null && hasAudioPermission()) vm.scan()
    }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestLyricsImport() { lyricsLauncher.launch(arrayOf("text/*", "application/x-lrc")) }
    private fun requestBackupExport() { backupExportLauncher.launch("AVATEK-backup.json") }
    private fun requestBackupImport() { backupImportLauncher.launch(arrayOf("application/json", "text/*")) }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else openNotificationSettings()
        } else openNotificationSettings()
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }
    }

    private fun requestPermissions() {
        val wanted = buildList {
            if (!hasAudioPermission()) add(audioPermission())
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (wanted.isEmpty()) { vm.scan(); return }
        permissionLauncher.launch(wanted.toTypedArray())
    }
}

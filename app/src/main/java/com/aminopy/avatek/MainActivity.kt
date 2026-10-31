package com.aminopy.avatek

import android.Manifest
import android.content.pm.PackageManager
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        vm.onPermissionResult(results[audioPermission()] == true || hasAudioPermission())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AvatekApp(vm, requestPermission = ::requestPermissions, requestLyricsImport = ::requestLyricsImport) }
        if (savedInstanceState == null && hasAudioPermission()) vm.scan()
    }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestLyricsImport() { lyricsLauncher.launch(arrayOf("text/*", "application/x-lrc")) }

    private fun requestPermissions() {
        if (hasAudioPermission()) { vm.scan(); return }
        val wanted = buildList {
            add(audioPermission())
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(wanted.toTypedArray())
    }
}

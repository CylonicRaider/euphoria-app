package io.euphoria.xkcd.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import io.euphoria.xkcd.app.updates.Manifest
import io.euphoria.xkcd.app.updates.ManifestDownloader
import io.euphoria.xkcd.app.updates.Version
import java.net.URL

class AboutActivity : Activity() {
    private enum class UpdateCheckState {
        BLANK, BUSY, FAILED, NO_RESULT, UPDATE_AVAILABLE
    }

    private lateinit var updateCheckState: UpdateCheckState
    private var updateCheckResult: Manifest? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_about)
        updateCheckState = UpdateCheckState.BLANK
        val version: TextView = findViewById(R.id.about_version)
        version.text = resources.getString(
            R.string.version_line,
            BuildConfig.VERSION_NAME,
            resources.getString(R.string.app_version_label)
        )
    }

    private fun setUpdateCheckState(newState: UpdateCheckState) {
        updateCheckState = newState
        val newestVersion: Version? = if (updateCheckResult != null) {
            updateCheckResult!!.version
        } else {
            null
        }
        val button: Button =
            findViewById(R.id.about_updateCheck)
        val outputLine: ViewGroup = findViewById(R.id.about_updateCheckLine)
        val progress: ProgressBar = findViewById(R.id.about_updateCheckProgress)
        val output: TextView = findViewById(R.id.about_updateCheckResult)
        when (newState) {
            UpdateCheckState.BLANK -> {
                button.isEnabled = true
                outputLine.visibility = View.GONE
                progress.visibility = View.GONE
            }
            UpdateCheckState.BUSY -> {
                button.isEnabled = false
                outputLine.visibility = View.VISIBLE
                progress.visibility = View.VISIBLE
            }
            UpdateCheckState.FAILED, UpdateCheckState.NO_RESULT, UpdateCheckState.UPDATE_AVAILABLE -> {
                button.isEnabled = true
                outputLine.visibility = View.VISIBLE
                progress.visibility = View.GONE
            }
        }
        if (newState == UpdateCheckState.UPDATE_AVAILABLE) {
            button.text = resources.getString(R.string.update_check_download, newestVersion)
        } else {
            button.setText(R.string.update_check)
        }
        when (newState) {
            UpdateCheckState.BUSY -> output.setText(R.string.update_check_busy)
            UpdateCheckState.FAILED -> output.setText(R.string.update_check_failed)
            UpdateCheckState.NO_RESULT -> output.setText(R.string.update_check_no_result)
            UpdateCheckState.UPDATE_AVAILABLE -> output.text = resources.getString(
                R.string.update_check_update_available,
                newestVersion
            )
            UpdateCheckState.BLANK -> {/* this is a dummy state -> do nothing */}
        }
    }

    fun updateButtonClicked(view: View?) {
        when (updateCheckState) {
            UpdateCheckState.BUSY -> {
            }
            UpdateCheckState.UPDATE_AVAILABLE -> {
                val url: URL? = updateCheckResult!!.latestRelease?.fileURL
                val intent: Intent = Intent(Intent.ACTION_VIEW, URLs.toUri(url))
                if (packageManager.resolveActivity(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    ) == null
                ) {
                    Toast.makeText(this, R.string.update_check_open_failed, Toast.LENGTH_SHORT)
                        .show()
                    return
                }
                startActivity(intent)
            }
            else -> {
                setUpdateCheckState(UpdateCheckState.BUSY)
                ManifestDownloader.download(
                    URLs.toURL(URLs.updateManifest),
                    object : ManifestDownloader.Callback {
                        override fun downloadFinished(result: Manifest) {
                            runOnUiThread { updateCheckFinished(result) }
                        }

                        override fun downloadFailed(error: Throwable) {
                            runOnUiThread { updateCheckFailed(error) }
                        }
                    })
            }
        }
    }

    private fun updateCheckFinished(result: Manifest) {
        updateCheckResult = result
        if (result.version > Version.Companion.currentAppVersion
        ) {
            setUpdateCheckState(UpdateCheckState.UPDATE_AVAILABLE)
        } else {
            setUpdateCheckState(UpdateCheckState.NO_RESULT)
        }
    }

    private fun updateCheckFailed(error: Throwable) {
        setUpdateCheckState(UpdateCheckState.FAILED)
        Log.w(
            UPDATE_CHECK_TAG,
            "Could not download update manifest: $error",
            error
        )
    }

    companion object {
        private const val UPDATE_CHECK_TAG: String = "UpdateCheck"
    }
}

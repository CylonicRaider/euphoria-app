package io.euphoria.xkcd.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;

import io.euphoria.xkcd.app.updates.Manifest;
import io.euphoria.xkcd.app.updates.ManifestDownloader;
import io.euphoria.xkcd.app.updates.Version;

public class AboutActivity extends Activity {

    private enum UpdateCheckState { BLANK, BUSY, FAILED, NO_RESULT, UPDATE_AVAILABLE }

    private static final String UPDATE_CHECK_TAG = "UpdateCheck";

    private UpdateCheckState updateCheckState;
    private Manifest updateCheckResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_about);

        updateCheckState = UpdateCheckState.BLANK;

        TextView version = findViewById(R.id.about_version);
        version.setText(getResources().getString(R.string.version_line,
                BuildConfig.VERSION_NAME,
                getResources().getString(R.string.app_version_label)));
    }

    private void setUpdateCheckState(UpdateCheckState newState) {
        updateCheckState = newState;
        Version newestVersion;
        if (updateCheckResult != null) {
            newestVersion = updateCheckResult.getVersion();
        } else {
            newestVersion = null;
        }
        Button button = findViewById(R.id.about_updateCheck);
        ViewGroup outputLine = findViewById(R.id.about_updateCheckLine);
        ProgressBar progress = findViewById(R.id.about_updateCheckProgress);
        TextView output = findViewById(R.id.about_updateCheckResult);
        switch (newState) {
            case BLANK:
                button.setEnabled(true);
                outputLine.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                break;
            case BUSY:
                button.setEnabled(false);
                outputLine.setVisibility(View.VISIBLE);
                progress.setVisibility(View.VISIBLE);
                break;
            case FAILED: case NO_RESULT: case UPDATE_AVAILABLE:
                button.setEnabled(true);
                outputLine.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                break;
        }
        if (newState == UpdateCheckState.UPDATE_AVAILABLE) {
            button.setText(getResources().getString(R.string.update_check_download, newestVersion));
        } else {
            button.setText(R.string.update_check);
        }
        switch (newState) {
            case BUSY:
                output.setText(R.string.update_check_busy);
                break;
            case FAILED:
                output.setText(R.string.update_check_failed);
                break;
            case NO_RESULT:
                output.setText(R.string.update_check_no_result);
                break;
            case UPDATE_AVAILABLE:
                output.setText(getResources().getString(R.string.update_check_update_available, newestVersion));
                break;
        }
    }

    public void updateButtonClicked(View view) {
        switch (updateCheckState) {
            case BUSY:
                /* Do nothing. */
                break;
            case UPDATE_AVAILABLE:
                URL url = updateCheckResult.getLatestRelease().getFileURL();
                Intent intent = new Intent(Intent.ACTION_VIEW, URLs.toUri(url));
                if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
                    Toast.makeText(this, R.string.update_check_open_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(intent);
                break;
            default:
                setUpdateCheckState(UpdateCheckState.BUSY);
                ManifestDownloader.download(URLs.toURL(URLs.getUpdateManifest()), new ManifestDownloader.Callback() {
                    @Override
                    public void downloadFinished(final Manifest result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCheckFinished(result);
                            }
                        });
                    }

                    @Override
                    public void downloadFailed(final Throwable error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCheckFailed(error);
                            }
                        });
                    }
                });
                break;
        }
    }

    private void updateCheckFinished(Manifest result) {
        updateCheckResult = result;
        if (result.getVersion().compareTo(Version.getCurrentAppVersion()) > 0) {
            setUpdateCheckState(UpdateCheckState.UPDATE_AVAILABLE);
        } else {
            setUpdateCheckState(UpdateCheckState.NO_RESULT);
        }
    }

    private void updateCheckFailed(Throwable error) {
        setUpdateCheckState(UpdateCheckState.FAILED);
        Log.w(UPDATE_CHECK_TAG, "Could not download update manifest: " + error, error);
    }

}

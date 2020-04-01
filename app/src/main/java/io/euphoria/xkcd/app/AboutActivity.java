package io.euphoria.xkcd.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.about_version);
        version.setText(getResources().getString(R.string.version_line,
                BuildConfig.VERSION_NAME,
                getResources().getString(R.string.app_version_label)));
    }

}

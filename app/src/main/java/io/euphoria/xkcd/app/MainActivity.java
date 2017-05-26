package io.euphoria.xkcd.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;

public class MainActivity extends Activity {

    Pattern ROOM_NAME_RE = Pattern.compile("^(?:[A-Za-z0-9\\-._~])+$");

    boolean mDualPane;
    Button enterBtn;
    AutoCompleteTextView roomField;

    // TODO comment
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enterBtn = (Button) findViewById(R.id.eneter_btn);
        roomField = (AutoCompleteTextView) findViewById(R.id.room_field);

        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder corrected = new StringBuilder();
                boolean changed = false;
                for (int i = start; i < end; i++) {
                    if (ROOM_NAME_RE.matcher("" + source.charAt(i)).matches()) {
                        corrected.append(source.charAt(i));
                    } else {
                        changed = true;
                    }
                }
                return changed ? corrected : null;
            }
        };
        roomField.setFilters(new InputFilter[]{inputFilter});
        roomField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    showRoom(roomField.getText().toString());
                    return true;
                }
                return false;
            }
        });

        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRoom(roomField.getText().toString());
            }
        });
    }

    private void showRoom(String roomName) {
        if (roomName.length() > 0) {
            Intent roomIntent = new Intent(this, RoomActivity.class);
            Uri roomURI = Uri.parse("https://euphoria.io/room/"+roomName+"/");
            roomIntent.setData(roomURI);
            roomIntent.setAction(Intent.ACTION_VIEW);
            startActivity(roomIntent);
        } else {
            Toast.makeText(MainActivity.this, "Please enter a valid room name", Toast.LENGTH_SHORT).show();
        }
    }
}

package io.euphoria.xkcd.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.InputFilter;
import android.text.InputType;
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

public class MainActivity extends FragmentActivity {

    Pattern ROOM_NAME_RE = Pattern.compile("^(?:[A-Za-z0-9\\-._~])+$");

    boolean mDualPane;
    Button enterBtn;
    AutoCompleteTextView roomField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View roomUIFrame = findViewById(R.id.room_ui_frame);
        mDualPane = roomUIFrame != null && roomUIFrame.getVisibility() == View.VISIBLE;

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
        roomField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        roomField.setImeOptions(EditorInfo.IME_ACTION_GO);
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

        enterBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEvent.ACTION_DOWN == event.getAction() && roomField.getText().length() > 0) {
                    showRoom(roomField.getText().toString());
                } else if (roomField.getText().length() <= 0) {
                    Toast.makeText(MainActivity.this, "Please enter a valid room name", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    private void showRoom(String roomName) {
        Intent roomIntent = new Intent(this, RoomActivity.class);
        Uri roomURI = new Uri.Builder().scheme("https").authority("euphoria.io").encodedPath("/room/" + roomName + "/").build();
        roomIntent.setData(roomURI);
        roomIntent.setAction(Intent.ACTION_VIEW);
        startActivity(roomIntent);
    }
}

package io.euphoria.xkcd.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import java.util.regex.Pattern;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.setEnterKeyListener;

public class MainActivity extends Activity {

    Pattern ROOM_NAME_RE = Pattern.compile("^[A-Za-z0-9:]+$");

    boolean mDualPane;
    Button enterBtn;
    AutoCompleteTextView roomField;

    // TODO comment
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Showing the title myself
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        enterBtn = (Button) findViewById(R.id.join_room);
        roomField = (AutoCompleteTextView) findViewById(R.id.room_name);

        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder corrected = new StringBuilder();
                boolean changed = false;
                for (int i = start; i < end; i++) {
                    if (ROOM_NAME_RE.matcher(Character.toString(source.charAt(i))).matches()) {
                        corrected.append(source.charAt(i));
                    } else {
                        changed = true;
                    }
                }
                return changed ? corrected : null;
            }
        };
        roomField.setFilters(new InputFilter[] {inputFilter});
        setEnterKeyListener(roomField, EditorInfo.IME_ACTION_GO, new Runnable() {
            @Override
            public void run() {
                MainActivity.this.showRoom(roomField.getText().toString());
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
        if (ROOM_NAME_RE.matcher(roomName).matches()) {
            Intent roomIntent = new Intent(this, RoomActivity.class);
            Uri roomURI = Uri.parse("https://euphoria.io/room/" + roomName + "/");
            roomIntent.setData(roomURI);
            roomIntent.setAction(Intent.ACTION_VIEW);
            startActivity(roomIntent);
        } else {
            Toast.makeText(MainActivity.this, "Please enter a valid room name", Toast.LENGTH_SHORT).show();
        }
    }
}

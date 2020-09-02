package io.euphoria.xkcd.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import static io.euphoria.xkcd.app.URLs.isValidRoomName;
import static io.euphoria.xkcd.app.URLs.isValidRoomNameFragment;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setEnterKeyListener;

public class MainActivity extends AppCompatActivity {

    private Button enterBtn;
    private AutoCompleteTextView roomField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Showing the title myself
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        enterBtn = (Button) findViewById(R.id.join_room);
        roomField = (AutoCompleteTextView) findViewById(R.id.room_name);

        // Filter out definitely invalid characters
        InputFilter inputFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                StringBuilder corrected = new StringBuilder();
                boolean changed = false;
                for (int i = start; i < end; i++) {
                    if (isValidRoomNameFragment(Character.toString(source.charAt(i)))) {
                        corrected.append(source.charAt(i));
                    } else {
                        changed = true;
                    }
                }
                return changed ? corrected : null;
            }
        };
        roomField.setFilters(new InputFilter[] {inputFilter});

        // Go to the selected room when done
        setEnterKeyListener(roomField, EditorInfo.IME_ACTION_GO, new Runnable() {
            @Override
            public void run() {
                showRoom(roomField.getText().toString());
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
        if (!isValidRoomName(roomName)) {
            Toast.makeText(MainActivity.this, "Please enter a valid room name", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent roomIntent = new Intent(this, RoomActivity.class);
        Uri roomURI = Uri.parse("https://euphoria.io/room/" + roomName + "/");
        roomIntent.setData(roomURI);
        roomIntent.setAction(Intent.ACTION_VIEW);
        startActivity(roomIntent);
    }

    public void settingsButtonOnClick(View settingsBtn) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}

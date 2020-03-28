package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Locale;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.InputBarView;
import io.euphoria.xkcd.app.impl.ui.MessageForest;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter.InputBarDirection;
import io.euphoria.xkcd.app.impl.ui.MessageListView;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.UIMessage;

import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.getRoomName;
import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.isValidRoomUri;

public class RoomActivity extends FragmentActivity {

    private static class TestMessage implements Message {

        private final String id;
        private final String nick;
        private final String parent;
        private final String content;

        private final SessionView sender = new SessionView() {
            @Override
            public String getSessionID() {
                return "0123-4567-89ab-cdef";
            }

            @Override
            public String getAgentID() {
                return "bot:test";
            }

            @Override
            public String getName() {
                return nick;
            }

            @Override
            public boolean isStaff() {
                return false;
            }

            @Override
            public boolean isManager() {
                return false;
            }
        };

        public TestMessage(String parent, String id, String nick, String content) {
            this.id = id;
            this.parent = parent;
            this.nick = nick;
            this.content = content;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getParent() {
            return parent;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public SessionView getSender() {
            return sender;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public boolean isTruncated() {
            return false;
        }

    }

    // TODO find some appropriate place for this in config
    public static final boolean RIGHT_KEY_HACK = true;

    // Test handling of out-of-order messages.
    private final UIMessage[] testMessages = new UIMessage[] {
            makeUIMessage("a", "j", "Test message A/J"),
            makeUIMessage("j", "k", "Test message A/J/K"),
            makeUIMessage("k", "l", "Test message A/J/K/L"),
            makeUIMessage("a", "b", "Test message A/B"),
            makeUIMessage(null, "a", "Test message A"),
            makeUIMessage("e", "f", "Test message A/E/F"),
            makeUIMessage("b", "d", "Test message A/B/D"),
            makeUIMessage("a", "e", "Test message A/E"),
            makeUIMessage("b", "c", "Test message A/B/C"),
            makeUIMessage("a", "g", "/me message A/G"),
            makeUIMessage("g", "h", "/me message A/G/H This is a particularly long testing string that will hopefully be wider than the screen."),
            makeUIMessage("g", "i", "Test message A/G/I This is a particularly long testing string that will hopefully be wider than the screen."),
            makeUIMessage(null, "x", "Test message X"),
            makeUIMessage("x", "x01", "Test message X/01"),
            makeUIMessage("x", "x02", "Test message X/02"),
            makeUIMessage("x", "x03", "Test message X/03"),
            makeUIMessage("x", "x04", "Test message X/04"),
            makeUIMessage("x", "x05", "Test message X/05"),
            makeUIMessage("x", "x06", "Test message X/06"),
            makeUIMessage("x", "x07", "Test message X/07"),
            makeUIMessage("x", "x08", "Test message X/08"),
            makeUIMessage("x", "x09", "Test message X/09"),
            makeUIMessage("x", "x10", "Test message X/10"),
            makeUIMessage("x", "x11", "Test message X/11"),
            makeUIMessage("x", "x12", "Test message X/12"),
            makeUIMessage("x", "x13", "Test message X/13"),
            makeUIMessage("x", "x14", "Test message X/14"),
            makeUIMessage("x", "x15", "Test message X/15")
    };

    private static UIMessage makeUIMessage(String parent, String id, String nick, String content) {
        return new UIMessage(new TestMessage(parent, id, nick, content));
    }

    private static UIMessage makeUIMessage(String parent, String id, String content) {
        return makeUIMessage(parent, id, "test", content);
    }

    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_TEST_ID = "testID";
    private static final String KEY_INPUT_STATE = "inputState";

    private final String TAG = "RoomActivity";

    // Tag for finding RoomControllerFragment
    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();

    private RoomControllerFragment roomControllerFragment;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private MessageListView messageList;
    private MessageListAdapter messageAdapter;
    private InputBarView inputBar;

    private int testID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // Get RoomControllerFragment
        FragmentManager fm = getSupportFragmentManager();
        roomControllerFragment = (RoomControllerFragment) fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT);

        // create the fragment and data the first time
        if (roomControllerFragment == null) {
            // add the fragment
            roomControllerFragment = new RoomControllerFragment();
            fm.beginTransaction().add(roomControllerFragment, TAG_ROOM_CONTROLLER_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        // Acquire RoomController
        roomController = roomControllerFragment.getRoomController();

        // View setup
        messageList = findViewById(R.id.message_recycler_view);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputBar = (InputBarView) inflater.inflate(R.layout.input_bar, messageList, false);

        MessageForest data = null;
        if (savedInstanceState != null) {
            data = savedInstanceState.getParcelable(KEY_MESSAGES);
            testID = savedInstanceState.getInt(KEY_TEST_ID);
            SparseArray<Parcelable> inputState = savedInstanceState.getSparseParcelableArray(KEY_INPUT_STATE);
            if (inputState != null) inputBar.restoreHierarchyState(inputState);
        }
        if (data == null) {
            data = new MessageForest();
        }
        if (savedInstanceState == null) {
            // TODO remove test messages
            for (UIMessage msg : testMessages) {
                data.add(msg);
            }
        }
        messageAdapter = new MessageListAdapter(data, inputBar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent i = getIntent();
        if (!Intent.ACTION_VIEW.equals(i.getAction()) || !isValidRoomUri(i.getData())) {
            Intent chooserIntent = new Intent(this, MainActivity.class);
            startActivity(chooserIntent);
            return;
        }

        String roomName = getRoomName(i.getData());
        setTitle("&" + roomName);
        roomUI = (RoomUIImpl) roomController.getManager().getRoomUI(roomName);

        messageList.setAdapter(messageAdapter);
        messageAdapter.setInputBarListener(new MessageListAdapter.InputBarListener() {
            @Override
            public void onInputBarMoved(String oldParent, String newParent) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        inputBar.requestEntryFocus();
                    }
                });
            }
        });

        inputBar.requestEntryFocus();
        inputBar.getMessageEntry().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                InputBarDirection dir;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        dir = InputBarDirection.UP;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        dir = InputBarDirection.DOWN;
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        dir = InputBarDirection.LEFT;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        // Upstream Euphoria binds the right key to the ROOT action, and we mirror that as default.
                        // TODO ask community whether they like it
                        dir = RIGHT_KEY_HACK ? InputBarDirection.ROOT : InputBarDirection.RIGHT;
                        break;
                    case KeyEvent.KEYCODE_ESCAPE:
                        dir = InputBarDirection.ROOT;
                        break;
                    default:
                        return false;
                }
                return inputBar.mayNavigateInput(dir) && messageAdapter.navigateInputBar(dir);
            }
        });
        // TODO remove test submission
        inputBar.setSubmitListener(new InputBarView.SubmitListener() {
            @Override
            public boolean onSubmit(InputBarView view) {
                String id = String.format((Locale) null, "z%05d", testID++);
                String parent = view.getMessage().getParent();
                messageAdapter.add(makeUIMessage(parent, id, view.getNickText(), view.getMessageText()));
                if (parent == null)
                    messageAdapter.moveInputBar(id);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_MESSAGES, messageAdapter.getData());
        outState.putInt(KEY_TEST_ID, testID);
        // RecyclerView suppresses instance state saving for all its children; we override this decision for the
        // input bar without poking into RecyclerView internals.
        SparseArray<Parcelable> inputState = new SparseArray<>();
        inputBar.saveHierarchyState(inputState);
        outState.putSparseParcelableArray(KEY_INPUT_STATE, inputState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //roomController.shutdown(); //NYI
    }

}

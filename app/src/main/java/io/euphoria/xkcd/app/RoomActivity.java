package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Locale;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.InputBarView;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter.InputBarDirection;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;

import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.getRoomName;
import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.isValidRoomUri;

public class RoomActivity extends FragmentActivity {

    // TODO find some appropriate place for this in config
    public static final boolean RIGHT_KEY_HACK = true;

    private class TestMessage implements Message {

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

        public TestMessage(String parent, String id, String content) {
            this(parent, id, "test", content);
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

    private final String TAG = "RoomActivity";

    // Tag for finding RoomControllerFragment
    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();

    private RoomControllerFragment rcf;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private RecyclerView recyclerView;
    private InputBarView inputBar;
    private MessageListAdapter rmla;

    private int testID = 0;

    // Test handling of out-of-order messages.
    private Message[] testMessages = new Message[] {
            new TestMessage("a", "b", "Test message A/B"),
            new TestMessage(null, "a", "Test message A"),
            new TestMessage("e", "f", "Test message A/E/F"),
            new TestMessage("b", "d", "Test message A/B/D"),
            new TestMessage("a", "e", "Test message A/E"),
            new TestMessage("b", "c", "Test message A/B/C")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        // Get RoomControllerFragment
        FragmentManager fm = getSupportFragmentManager();
        rcf = (RoomControllerFragment) fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT);

        // create the fragment and data the first time
        if (rcf == null) {
            // add the fragment
            rcf = new RoomControllerFragment();
            fm.beginTransaction().add(rcf, TAG_ROOM_CONTROLLER_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        // Acquire RoomController
        roomController = rcf.getRoomController();

        // View setup
        recyclerView = (RecyclerView) findViewById(R.id.message_recycler_view);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            recyclerView.setItemAnimator(null);
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputBar = (InputBarView) inflater.inflate(R.layout.input_bar, null);
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

        rmla = new MessageListAdapter(inputBar);
        // TODO remove test messages
        for (Message msg : testMessages) {
            rmla.add(msg);
        }
        rmla.moveInputBar(null);
        recyclerView.setAdapter(rmla);
        rmla.setInputBarListener(new MessageListAdapter.InputBarListener() {
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
                return inputBar.mayNavigateInput(dir) && rmla.navigateInputBar(dir);
            }
        });
        // TODO remove test submission
        inputBar.setSubmitListener(new InputBarView.SubmitListener() {
            @Override
            public boolean onSubmit(InputBarView view) {
                String id = String.format((Locale) null, "z%05d", testID++);
                String parent = view.getTree().getParent();
                rmla.add(new TestMessage(parent, id, view.getNick(), view.getMessage()));
                if (parent == null)
                    rmla.moveInputBar(id);
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //roomController.shutdown(); //NYI
    }

}

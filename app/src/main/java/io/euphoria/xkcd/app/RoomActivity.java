package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.InputBarView;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;

public class RoomActivity extends FragmentActivity {

    private class TestMessage implements Message {

        private final String id;
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
                return "TestBot";
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

        public TestMessage(String parent, String id, String content) {
            this.id = id;
            this.parent = parent;
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

    // Tag for finding RoomControllerFragment
    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();
    private static final Pattern ROOM_PATH_RE = Pattern.compile("^/room/([A-Za-z0-9:]+)/?$");

    private RoomControllerFragment rcf;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private RecyclerView recyclerView;
    private InputBarView inputBar;
    private MessageListAdapter rmla;

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

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputBar = (InputBarView) inflater.inflate(R.layout.input_bar, null);
        inputBar.init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = getIntent();
        // Check passed path with ROOM_PATH_RE, if it doesn't match return to the MainActivity
        Matcher m = ROOM_PATH_RE.matcher(i.getData().getPath());
        if (Intent.ACTION_VIEW.equals(i.getAction()) && isEuphoriaURI(i.getData()) && m.matches()) {
            setTitle("&" + m.group(1));

            roomUI = (RoomUIImpl) roomController.getManager().getRoomUI(m.group(1));
            rmla = new MessageListAdapter(inputBar);
            // TODO remove test messages
            for (Message msg : testMessages) {
                rmla.add(msg);
            }
            rmla.moveInputBar(null);
            recyclerView.setAdapter(rmla);
        } else {
            Intent chooserIntent = new Intent(this, MainActivity.class);
            startActivity(chooserIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //roomController.shutdown(); //NYI
    }

    /**
     * Checks if an URI describes a room of euphoria
     *
     * @param uri URI to check
     * @return True if URI describes an euphoria room
     */
    private boolean isEuphoriaURI(Uri uri) {
        return uri.getHost().toLowerCase().equals("euphoria.io");
    }
}

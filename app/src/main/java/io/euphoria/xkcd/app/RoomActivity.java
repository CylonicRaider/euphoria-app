package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.impl.ui.InputBarView;
import io.euphoria.xkcd.app.impl.ui.MessageForest;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter.InputBarDirection;
import io.euphoria.xkcd.app.impl.ui.MessageListView;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.UserList;
import io.euphoria.xkcd.app.impl.ui.UserListAdapter;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIFactory;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;

import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.getRoomName;
import static io.euphoria.xkcd.app.impl.ui.RoomUIImpl.isValidRoomUri;

public class RoomActivity extends FragmentActivity {

    private class LocalRoomUIFactory implements RoomUIFactory {
        @Override
        public RoomUI createRoomUI(String roomName) {
            return new LocalRoomUIImpl(roomName);
        }
    }

    private class LocalRoomUIImpl extends RoomUIImpl {

        private ConnectionStatus connectionStatus;

        public LocalRoomUIImpl(String roomName) {
            super(roomName);
        }

        public ConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        @Override
        public void setConnectionStatus(ConnectionStatus status) {
            super.setConnectionStatus(status);
            connectionStatus = status;
        }

        @Override
        public void showMessages(List<Message> messages) {
            super.showMessages(messages);
            // An empty response signifies no more logs.
            if (messages.isEmpty()) return;
            for (Message msg : messages) {
                if (earliestID == null || msg.getID().compareTo(earliestID) < 0) {
                    earliestID = msg.getID();
                }
            }
            isPullingLogs = false;
            checkPullLogs();
        }
    }

    // TODO find some appropriate place for this in config
    public static final boolean RIGHT_KEY_HACK = true;
    private static final double LOG_PULL_THRESHOLD = 0.1;

    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_USERS = "users";
    private static final String KEY_INPUT_STATE = "inputState";

    private final String TAG = "RoomActivity";

    // Tag for finding RoomControllerFragment
    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();

    private RoomControllerFragment roomControllerFragment;
    private RoomController roomController;

    private LocalRoomUIImpl roomUI;
    private TextView statusDisplay;
    private MessageListView messageList;
    private MessageListAdapter messageAdapter;
    private RecyclerView userList;
    private UserListAdapter userListAdapter;
    private InputBarView inputBar;

    private boolean isPullingLogs;
    private String earliestID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quickly bounce away if we have a wrong room
        Intent i = getIntent();
        if (!Intent.ACTION_VIEW.equals(i.getAction()) || !isValidRoomUri(i.getData())) {
            Intent chooserIntent = new Intent(this, MainActivity.class);
            startActivity(chooserIntent);
            return;
        }

        // Create the UI
        setContentView(R.layout.activity_room);
        String roomName = getRoomName(i.getData());
        setTitle("&" + roomName);

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
        // Acquire RoomController and roomUI
        roomController = roomControllerFragment.getRoomController();
        roomController.getRoomUIManager().setRoomUIFactory(new LocalRoomUIFactory());
        roomUI = (LocalRoomUIImpl) roomController.getRoomUIManager().getRoomUI(roomName);

        // View setup
        statusDisplay = findViewById(R.id.conn_status_display);

        messageList = findViewById(R.id.message_list_view);
        userList = findViewById(R.id.user_list_view);
        userList.setLayoutManager(new LinearLayoutManager(userList.getContext()));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputBar = (InputBarView) inflater.inflate(R.layout.input_bar, messageList, false);

        // Data setup
        MessageForest messages = null;
        UserList users = null;
        if (savedInstanceState != null) {
            messages = savedInstanceState.getParcelable(KEY_MESSAGES);
            users = savedInstanceState.getParcelable(KEY_USERS);
            SparseArray<Parcelable> inputState = savedInstanceState.getSparseParcelableArray(KEY_INPUT_STATE);
            if (inputState != null) inputBar.restoreHierarchyState(inputState);
        }
        if (messages == null) {
            messages = new MessageForest();
        }
        if (users == null) {
            users = new UserList();
        }
        messageAdapter = new MessageListAdapter(messages, inputBar);
        userListAdapter = new UserListAdapter(users);
        messageList.setAdapter(messageAdapter);
        userList.setAdapter(userListAdapter);

        // Input bar setup
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

        // Scrolling setup
        messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy == 0) return;
                checkPullLogs();
            }
        });

        // Controller etc. setup
        roomUI.link(statusDisplay, messageAdapter, userListAdapter);
        roomController.openRoom(roomName);
        inputBar.setNickChangeListener(new InputBarView.NickChangeListener() {
            @Override
            public boolean onChangeNick(InputBarView view) {
                final String text = view.getNickText();
                if (text.isEmpty() || roomUI.getConnectionStatus() != ConnectionStatus.CONNECTED) return false;
                roomUI.submitEvent(new NewNickEvent() {
                    @Override
                    public String getNewNick() {
                        return text;
                    }

                    @Override
                    public RoomUI getRoomUI() {
                        return roomUI;
                    }
                });
                return true;
            }
        });
        inputBar.setSubmitListener(new InputBarView.SubmitListener() {
            @Override
            public boolean onSubmit(InputBarView view) {
                final String text = view.getMessageText();
                final String parent = view.getMessage().getParent();
                if (text.isEmpty() || roomUI.getConnectionStatus() != ConnectionStatus.CONNECTED) return false;
                roomUI.submitEvent(new MessageSendEvent() {
                    @Override
                    public String getText() {
                        return text;
                    }

                    @Override
                    public String getParent() {
                        return parent;
                    }

                    @Override
                    public RoomUI getRoomUI() {
                        return roomUI;
                    }
                });
                return true;
            }
        });
        // Suspend log pulling until the snapshot-event arrives.
        isPullingLogs = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_room, menu);
        return true;
    }

    public void toggleUserDrawer(MenuItem item) {
        DrawerLayout dl = findViewById(R.id.room_drawer_root);
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END);
        } else {
            dl.openDrawer(Gravity.END);
        }
    }

    public void showAbout(MenuItem item) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout dl = findViewById(R.id.room_drawer_root);
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_MESSAGES, messageAdapter.getData());
        outState.putParcelable(KEY_USERS, userListAdapter.getData());
        // RecyclerView suppresses instance state saving for all its children; we override this decision for the
        // input bar without poking into RecyclerView internals.
        SparseArray<Parcelable> inputState = new SparseArray<>();
        inputBar.saveHierarchyState(inputState);
        outState.putSparseParcelableArray(KEY_INPUT_STATE, inputState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomUI.unlink(statusDisplay, messageAdapter, userListAdapter);
        roomController.closeRoom(roomUI.getRoomName());
        roomController.getRoomUIManager().setRoomUIFactory(null);
        roomController.shutdown();
    }

    private void checkPullLogs() {
        LinearLayoutManager layout = (LinearLayoutManager) messageList.getLayoutManager();
        RecyclerView.Adapter adapter = messageList.getAdapter();
        if (layout == null || adapter == null) return;
        if (layout.findFirstVisibleItemPosition() > adapter.getItemCount() * LOG_PULL_THRESHOLD) return;
        pullMoreLogs();
    }

    private void pullMoreLogs() {
        if (isPullingLogs) return;
        isPullingLogs = true;
        final String top = earliestID;
        roomUI.submitEvent(new LogRequestEvent() {
            @Override
            public String getBefore() {
                return top;
            }

            @Override
            public RoomUI getRoomUI() {
                return roomUI;
            }
        });
    }

}

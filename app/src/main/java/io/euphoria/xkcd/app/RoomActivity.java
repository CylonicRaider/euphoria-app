package io.euphoria.xkcd.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.RoomUIManagerImpl;
import io.euphoria.xkcd.app.impl.ui.data.MessageForest;
import io.euphoria.xkcd.app.impl.ui.data.UserList;
import io.euphoria.xkcd.app.impl.ui.views.InputBarView;
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter;
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter.InputBarDirection;
import io.euphoria.xkcd.app.impl.ui.views.MessageListView;
import io.euphoria.xkcd.app.impl.ui.views.UserListAdapter;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIFactory;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;

import static io.euphoria.xkcd.app.URLs.getRoomName;
import static io.euphoria.xkcd.app.URLs.isValidRoomUri;

public class RoomActivity extends AppCompatActivity {

    private class LocalRoomUIFactory implements RoomUIFactory {
        @Override
        public RoomUI createRoomUI(String roomName) {
            return new LocalRoomUIImpl((RoomUIManagerImpl) roomController.getRoomUIManager(), roomName);
        }
    }

    private class LocalRoomUIImpl extends RoomUIImpl {

        private ConnectionStatus connectionStatus;

        public LocalRoomUIImpl(RoomUIManagerImpl parent, String roomName) {
            super(parent, roomName);
        }

        public ConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        @Override
        public void setConnectionStatus(ConnectionStatus status) {
            super.setConnectionStatus(status);
            if (connectionStatus != ConnectionStatus.CONNECTED && status == ConnectionStatus.CONNECTED) trimLogs();
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

    private RoomController roomController;

    private LocalRoomUIImpl roomUI;
    private ActionBarDrawerToggle roomToggle;
    private TextView statusDisplay;
    private MessageListView messageList;
    private MessageListAdapter messageAdapter;
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout roomDrawer = findViewById(R.id.room_drawer_root);
        roomToggle = new ActionBarDrawerToggle(this, roomDrawer, toolbar,
                R.string.menu_rooms_open, R.string.menu_rooms_close);
        roomDrawer.addDrawerListener(roomToggle);

        // Get RoomControllerFragment
        FragmentManager fm = getSupportFragmentManager();
        RoomControllerFragment rcf = (RoomControllerFragment) fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT);

        // create the fragment and data the first time
        if (rcf == null) {
            // add the fragment
            rcf = new RoomControllerFragment();
            fm.beginTransaction().add(rcf, TAG_ROOM_CONTROLLER_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        // Acquire RoomController and roomUI
        roomController = rcf.getRoomController();
        roomController.getRoomUIManager().setRoomUIFactory(new LocalRoomUIFactory());
        roomUI = (LocalRoomUIImpl) roomController.getRoomUIManager().getRoomUI(roomName);

        // View setup
        statusDisplay = findViewById(R.id.conn_status_display);

        messageList = findViewById(R.id.message_list_view);
        RecyclerView userList = findViewById(R.id.user_list_view);
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
        roomUI.link(statusDisplay, messageAdapter, userListAdapter, inputBar);
        roomController.openRoom(roomName);
        inputBar.setNickChangeListener(new InputBarView.NickChangeListener() {
            @Override
            public boolean onChangeNick(InputBarView view) {
                final String text = view.getNextNick();
                if (text.isEmpty() || roomUI.getConnectionStatus() != ConnectionStatus.CONNECTED) return false;
                if (text.equals(view.getConfirmedNick())) return true;
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
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        roomToggle.syncState();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions_room, menu);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        roomToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (roomToggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    public void toggleUserDrawer(MenuItem item) {
        DrawerLayout dl = findViewById(R.id.room_drawer_root);
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END);
        } else {
            dl.openDrawer(Gravity.END);
        }
    }

    public void showSettings(MenuItem item) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void showAbout(MenuItem item) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout dl = findViewById(R.id.room_drawer_root);
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END);
        } else if (dl.isDrawerOpen(Gravity.START)) {
            dl.closeDrawer(Gravity.START);
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
        roomUI.unlink(statusDisplay, messageAdapter, userListAdapter, inputBar);
        roomController.closeRoom(roomUI.getRoomName());
        roomController.getRoomUIManager().setRoomUIFactory(null);
        roomController.shutdown();
    }

    private void checkPullLogs() {
        LinearLayoutManager layout = (LinearLayoutManager) messageList.getLayoutManager();
        RecyclerView.Adapter<?> adapter = messageList.getAdapter();
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

    private void trimLogs() {
        MessageListAdapter adapter = (MessageListAdapter) messageList.getAdapter();
        adapter.clearExceptInputBar();
    }

}

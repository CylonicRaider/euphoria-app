package io.euphoria.xkcd.app.control;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.euphoria.xkcd.app.connection.Connection;
import io.euphoria.xkcd.app.connection.ConnectionListener;
import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.connection.ConnectionStatus;
import io.euphoria.xkcd.app.connection.event.CloseEvent;
import io.euphoria.xkcd.app.connection.event.IdentityEvent;
import io.euphoria.xkcd.app.connection.event.LogEvent;
import io.euphoria.xkcd.app.connection.event.MessageEvent;
import io.euphoria.xkcd.app.connection.event.NickChangeEvent;
import io.euphoria.xkcd.app.connection.event.OpenEvent;
import io.euphoria.xkcd.app.connection.event.PresenceChangeEvent;
import io.euphoria.xkcd.app.ui.RoomUI;
import io.euphoria.xkcd.app.ui.RoomUIManager;
import io.euphoria.xkcd.app.ui.UIListener;
import io.euphoria.xkcd.app.ui.event.LogRequestEvent;
import io.euphoria.xkcd.app.ui.event.MessageSendEvent;
import io.euphoria.xkcd.app.ui.event.NewNickEvent;
import io.euphoria.xkcd.app.ui.event.RoomSwitchEvent;
import io.euphoria.xkcd.app.ui.event.UICloseEvent;

/** Created by Xyzzy on 2017-03-19. */

public class RoomController {

    public static final int DEFAULT_LOG_REQUEST_AMOUNT = 50;

    private final Context context;
    private final Handler handler;
    private final RoomUIManager uiManager;
    private final ConnectionManager connManager;
    private final Set<String> openRooms;

    public RoomController(Context ctx, RoomUIManager uiManager, ConnectionManager connManager) {
        this.context = ctx;
        this.handler = new Handler(ctx.getMainLooper());
        this.uiManager = uiManager;
        this.connManager = connManager;
        this.openRooms = new HashSet<>();
    }

    public Context getContext() {
        return context;
    }

    public RoomUIManager getRoomUIManager() {
        return uiManager;
    }

    public ConnectionManager getConnectionManager() {
        return connManager;
    }

    public void openRoom(String roomName) {
        Connection conn = connManager.connect(roomName);
        RoomUI ui = uiManager.getRoomUI(roomName);
        if (openRooms.add(roomName)) link(conn, ui);
    }

    public void closeRoom(String roomName) {
        Connection conn = connManager.getConnection(roomName);
        if (conn != null) conn.close();
        RoomUI ui = uiManager.getRoomUI(roomName);
        if (ui != null) ui.close();
        openRooms.remove(roomName);
    }

    public void shutdown() {
        connManager.shutdown();
        List<String> rooms = new ArrayList<>(openRooms);
        for (String roomName : rooms) {
            closeRoom(roomName);
        }
    }

    public void invokeLater(Runnable cb) {
        handler.post(cb);
    }

    public void invokeLater(Runnable cb, long delay) {
        handler.postDelayed(cb, delay);
    }

    protected void link(final Connection conn, final RoomUI ui) {
        ui.setConnectionStatus(ConnectionStatus.CONNECTING);
        conn.addEventListener(new ConnectionListener() {
            @Override
            public void onOpen(OpenEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.setConnectionStatus(ConnectionStatus.CONNECTED);
                    }
                });
            }

            @Override
            public void onIdentity(final IdentityEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.setIdentity(evt.getIdentity());
                        ui.showNicks(Collections.singletonList(evt.getIdentity()));
                    }
                });
            }

            @Override
            public void onNickChange(final NickChangeEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.showNicks(Collections.singletonList(evt.getSession()));
                    }
                });
            }

            @Override
            public void onMessage(final MessageEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.showMessages(Collections.singletonList(evt.getMessage()));
                    }
                });
            }

            @Override
            public void onPresenceChange(final PresenceChangeEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (evt.isPresent()) {
                            ui.showNicks(evt.getSessions());
                        } else {
                            ui.removeNicks(evt.getSessions());
                        }
                    }
                });
            }

            @Override
            public void onLogEvent(final LogEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.showMessages(evt.getMessages());
                    }
                });
            }

            @Override
            public void onClose(final CloseEvent evt) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ui.setConnectionStatus(evt.isFinal() ? ConnectionStatus.DISCONNECTED :
                                                               ConnectionStatus.RECONNECTING);
                    }
                });
            }
        });
        ui.addEventListener(new UIListener() {
            @Override
            public void onNewNick(NewNickEvent evt) {
                conn.setNick(evt.getNewNick());
            }

            @Override
            public void onMessageSend(MessageSendEvent evt) {
                conn.postMessage(evt.getText(), evt.getParent());
            }

            @Override
            public void onLogRequest(LogRequestEvent evt) {
                conn.requestLogs(evt.getBefore(), DEFAULT_LOG_REQUEST_AMOUNT);
            }

            @Override
            public void onRoomSwitch(RoomSwitchEvent evt) {
                openRoom(evt.getRoomName());
            }

            @Override
            public void onClose(UICloseEvent evt) {
                closeRoom(evt.getRoomUI().getRoomName());
            }
        });
    }

}

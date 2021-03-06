package io.euphoria.xkcd.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.connection.SessionCookieStore;
import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.impl.connection.ConnectionManagerImpl;
import io.euphoria.xkcd.app.impl.connection.SessionCookieStoreImpl;
import io.euphoria.xkcd.app.impl.ui.RoomUIManagerImpl;
import io.euphoria.xkcd.app.ui.RoomUIManager;

/**
 * @author N00bySumairu
 */

public class RoomControllerFragment extends Fragment {

    private RoomUIManager roomUIManager;
    private ConnectionManager connManager;
    private RoomController roomController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roomUIManager = new RoomUIManagerImpl();
        SessionCookieStore sessionCookieStore = new SessionCookieStoreImpl(new Settings(getActivity().getApplicationContext()));
        connManager = new ConnectionManagerImpl(sessionCookieStore);
        roomController = new RoomController(getActivity().getApplicationContext(), roomUIManager, connManager);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        roomController.shutdown();
    }

    public RoomController getRoomController() {
        return roomController;
    }

}

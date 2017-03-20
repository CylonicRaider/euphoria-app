package io.euphoria.xkcd.app.control;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import io.euphoria.xkcd.app.connection.ConnectionManager;
import io.euphoria.xkcd.app.impl.connection.ConnectionManagerImpl;

/** Created by Xyzzy on 2017-03-19. */

public class ConnectionService extends Service {
    class CBinder extends Binder {
        ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    private final CBinder BINDER = new CBinder();
    private ConnectionManager mgr;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }

    @Override
    public void onCreate() {
        mgr = ConnectionManagerImpl.getInstance();
    }

    @Override
    public void onDestroy() {
        mgr.shutdown();
    }
}

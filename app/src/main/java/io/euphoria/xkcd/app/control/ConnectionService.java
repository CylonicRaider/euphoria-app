package io.euphoria.xkcd.app.control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/** Created by Xyzzy on 2017-03-19. */

public class ConnectionService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

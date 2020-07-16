package io.euphoria.xkcd.app.updates;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Created by Xyzzy on 2020-07-14. */

public class ManifestDownloader {

    public interface Callback {

        void downloadFinished(Manifest result);

        void downloadFailed(Throwable error);

    }

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    private ManifestDownloader() {}

    public static void download(final URL source, Executor exec, final Callback handler) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection conn = source.openConnection();
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    Reader inputReader = new InputStreamReader(input, "UTF-8");
                    String result = UpdateUtilities.readAll(inputReader);
                    handler.downloadFinished(new Manifest(source, new JSONObject(result)));
                } catch (IOException | JSONException | Manifest.MappingException exc) {
                    handler.downloadFailed(exc);
                }
            }
        });
    }
    public static void download(URL source, Callback handler) {
        download(source, EXECUTOR, handler);
    }

}

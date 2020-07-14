package io.euphoria.xkcd.app.updates;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/** Created by Xyzzy on 2020-07-14. */

public class ManifestDownloader {

    private ManifestDownloader() {}

    public static Future<Manifest> download(final URL source, ExecutorService exec) {
        return exec.submit(new Callable<Manifest>() {
            @Override
            public Manifest call() throws IOException, JSONException, Manifest.MappingException {
                URLConnection conn = source.openConnection();
                conn.connect();
                InputStream input = conn.getInputStream();
                Reader inputReader = new InputStreamReader(input, "UTF-8");
                String result = UpdateUtilities.readAll(inputReader);
                return new Manifest(source, new JSONObject(result));
            }
        });
    }

}

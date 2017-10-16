package com.movilesunal.movichat.common;

import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by erick on 29/6/2017.
 */
public class DownloadFile extends AsyncTask<String, String, InputStream> {

    private final StorageReference ref;
    private Context context;

    public DownloadFile(Context context, StorageReference ref) {
        this.context = context;
        this.ref = ref;
    }

    @Override
    protected InputStream doInBackground(String... strings) {
        return downloadImage(strings[0]);
    }

    @Override
    protected void onPostExecute(InputStream result) {
        if (result != null) {
            ref.putStream(result);
        }
    }

    private InputStream downloadImage(String url) {
        try {
            URL urlConnection = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}

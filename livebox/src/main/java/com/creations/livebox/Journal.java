package com.creations.livebox;

import android.util.Log;

import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class Journal {

    private static final String TAG = "Journal";
    private static final String FILENAME = "journal.livebox";
    private static final Executor JOURNAL_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Map<String, Long> mTimestamps = new HashMap<>();

    private File mOutputFileDir;
    private Gson mGson = new Gson();
    private final TypeToken<Map<String, Long>> typeToken = new TypeToken<Map<String, Long>>() {
    };

    private final Runnable mJournalWriterRun = new Runnable() {
        @Override
        public void run() {

            boolean created = true;
            if (!mOutputFileDir.exists()) {
                created = mOutputFileDir.mkdir();
            }

            if (!created) {
                Log.e(TAG, "Cannot create journal output dir");
                return;
            }

            try (Writer writer = new BufferedWriter(new FileWriter(new File(mOutputFileDir, FILENAME)))) {
                mGson.toJson(mTimestamps, typeToken.getType(), writer);
                Logger.d(TAG, "Wrote to journal");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    private Journal(File file) {
        mOutputFileDir = file;
    }

    public static Journal create(File f) {
        return new Journal(f);
    }

    public Optional<Long> read(String key) {
        synchronized (this) {
            return Optional.ofNullable(mTimestamps.get(key));
        }
    }

    public void save(String key, long timestamp) {
        synchronized (this) {
            final Long oldTimestamp = mTimestamps.get(key);
            if (oldTimestamp == null || timestamp > oldTimestamp) {
                mTimestamps.put(key, timestamp);
                JOURNAL_EXECUTOR.execute(mJournalWriterRun);
            }
        }
    }

}

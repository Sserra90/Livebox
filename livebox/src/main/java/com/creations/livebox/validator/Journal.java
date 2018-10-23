package com.creations.livebox.validator;

import com.creations.livebox.util.Optional;
import com.creations.livebox_common.util.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.creations.livebox.util.io.UtilsKt.closeQuietly;


public class Journal {

    private static final String TAG = "Journal";
    private static final String FILENAME = "journal_livebox.txt";
    private static final String SEPARATOR = ":";
    private static final String LINE_BREAK = System.getProperty("line.separator");

    private static volatile Journal INSTANCE;

    private Executor mExecutor;
    private Map<String, Long> mTimestamps = new HashMap<>();
    private File mOutputFileDir, mOutputFile;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private final class JournalWriterRun implements Runnable {

        private Map<String, Long> mValues;
        private Writer mWriter;

        JournalWriterRun(Map<String, Long> values) {
            mValues = values;
        }

        @Override
        public void run() {
            try {

                // Create a new writer to erase the contents, this is probably not the best to
                // do this. What can we do ??
                createWriter();

                if (mWriter == null) {
                    return;
                }

                for (Map.Entry<String, Long> entry : mValues.entrySet()) {
                    mWriter.write(buildLine(entry));
                    mWriter.write(LINE_BREAK);
                }
                mWriter.flush();
                System.out.println("Wrote values: " + mValues);
            } catch (IOException e) {
                e.printStackTrace();
                // no op
            }
        }

        private void createWriter() {
            try {
                mWriter = new BufferedWriter(new FileWriter(mOutputFile));
            } catch (IOException e) {
                e.printStackTrace();
                closeQuietly(mWriter);
                mWriter = null;
            }
        }

        private String buildLine(Map.Entry<String, Long> entry) {
            return entry.getKey() + ":" + entry.getValue();
        }
    }

    public static Journal create(File f, Executor executor) {
        if (INSTANCE == null) {
            synchronized (Journal.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Journal(f, executor);
                }
            }
        }
        return INSTANCE;
    }

    public static Journal create(File f) {
        return create(f, Executors.newSingleThreadExecutor());
    }

    private Journal(File file, Executor executor) {
        mExecutor = executor;
        mOutputFileDir = file;
        mOutputFile = new File(mOutputFileDir, FILENAME);
        rebuildFromDisk();
    }

    private void rebuildFromDisk() {

        if (!mOutputFileDir.exists() && !mOutputFileDir.mkdir()) {
            Logger.e(TAG, "Cannot create journal output dir");
            return;
        }

        if (!mOutputFile.exists()) {
            Logger.d(TAG, "No journal file found");
            return;
        }

        try {
            final Scanner scan = new Scanner(mOutputFile);
            while (scan.hasNextLine()) {
                final String[] values = scan.nextLine().split(SEPARATOR);
                mTimestamps.put(values[0], Long.valueOf(values[1]));
            }
            System.out.println("Rebuilt from disk values: " + mTimestamps);
        } catch (Exception e) {
            //
        }

    }

    public Optional<Long> read(String key) {

        Optional<Long> res;
        readWriteLock.readLock().lock();
        try {
            res = Optional.ofNullable(mTimestamps.get(key));
        } finally {
            readWriteLock.readLock().unlock();
        }
        return res;
    }

    public void save(String key, long timestamp) {

        readWriteLock.writeLock().lock();
        try {
            final Long oldTimestamp = mTimestamps.get(key);
            if (oldTimestamp == null || timestamp > oldTimestamp) {
                mTimestamps.put(key, timestamp);
                mExecutor.execute(new JournalWriterRun(new HashMap<>(mTimestamps)));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

}

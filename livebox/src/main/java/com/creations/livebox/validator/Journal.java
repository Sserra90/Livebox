package com.creations.livebox.validator;

import com.creations.livebox.util.Optional;
import com.creations.livebox_common.util.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import kotlin.Pair;

import static com.creations.livebox.util.io.UtilsKt.closeQuietly;


public class Journal {

    private static final String TAG = "Journal";
    private static final String FILENAME = "journal_livebox.txt";
    private static final String SEPARATOR = ":";
    private static final String LINE_BREAK = System.getProperty("line.separator");
    private static final int LIMIT = 300;

    private Executor mExecutor;
    private Map<String, Long> mTimestamps;
    private File mOutputFileDir, mOutputFile;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private Writer mWriter;

    private static final class JournalWriterRun implements Runnable {

        private Map<String, Long> mValues;
        private Writer mWriter;

        JournalWriterRun(Map<String, Long> values, Writer writer) {
            mValues = values;
            mWriter = writer;
        }

        @Override
        public void run() {
            try {

                if (mWriter == null) {
                    return;
                }

                for (Map.Entry<String, Long> entry : mValues.entrySet()) {
                    mWriter.write(buildLine(entry));
                    mWriter.write(LINE_BREAK);
                }
                mWriter.flush();
                Logger.d(TAG, "Wrote values: " + mValues);
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    private static final class JournalLineWriterRun implements Runnable {

        private Pair<String, Long> mEntry;
        private Writer mWriter;

        JournalLineWriterRun(Pair<String, Long> entry, Writer writer) {
            mEntry = entry;
            mWriter = writer;
        }

        @Override
        public void run() {
            try {

                if (mWriter == null) {
                    return;
                }

                mWriter.write(buildLine(mEntry));
                mWriter.write(LINE_BREAK);
                mWriter.flush();

                Logger.d(TAG, "Wrote values: " + mEntry);
            } catch (IOException e) {
                e.printStackTrace();
                // no op
            }
        }

    }

    private static String buildLine(Map.Entry<String, Long> entry) {
        return entry.getKey() + ":" + entry.getValue();
    }

    private static String buildLine(Pair<String, Long> entry) {
        return entry.getFirst() + ":" + entry.getSecond();
    }

    public static Journal create(File f, Executor executor, int limit) {
        return new Journal(f, executor, limit);
    }

    public static Journal create(File f, int limit) {
        return create(f, Executors.newSingleThreadExecutor(), limit);
    }

    public static Journal create(File f) {
        return create(f, Executors.newSingleThreadExecutor(), LIMIT);
    }

    private Journal(File file, Executor executor, int limit) {
        mExecutor = executor;
        mOutputFileDir = file;
        mOutputFile = new File(mOutputFileDir, FILENAME);
        mTimestamps = new MaxSizeHashMap<>(limit);
        mWriter = createWriter();
        rebuildFromDisk();
    }

    private Writer createWriter(final boolean append) {
        try {

            if (!mOutputFile.exists()) {
                if (!mOutputFile.mkdir()) {
                    return null;
                }
            }

            return new BufferedWriter(new FileWriter(mOutputFile, append));
        } catch (IOException e) {
            e.printStackTrace();
            closeQuietly(mWriter);
        }
        return null;
    }

    private Writer createWriter() {
        return createWriter(true);
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

            // Write to file, eliminate duplicate entries
            if (!mTimestamps.isEmpty()) {
                mExecutor.execute(new JournalWriterRun(mTimestamps, createWriter(false)));
            }

            Logger.d(TAG, "Rebuilt from disk values: " + mTimestamps);
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
                mExecutor.execute(new JournalLineWriterRun(new Pair<>(key, timestamp), mWriter));
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public int size() {
        return mTimestamps.size();
    }

    public static class MaxSizeHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        MaxSizeHashMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}

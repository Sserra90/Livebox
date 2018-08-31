package com.creations.livebox;

import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;
import com.creations.livebox.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Journal {

    private static final String TAG = "Journal";
    private static final String FILENAME = "journal_livebox.txt";

    private Executor mExecutor;
    private Map<String, Long> mTimestamps = new HashMap<>();
    private File mOutputFileDir, mOutputFile;
    private Writer mWriter;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private final class JournalWriterRun implements Runnable {

        private String mLine;

        JournalWriterRun(String line) {
            this.mLine = line;
        }

        @Override
        public void run() {
            try {
                mWriter.write(mLine);
                mWriter.write(System.getProperty("line.separator")); // New line
                mWriter.flush();
                System.out.println("Wrote line: " + mLine);
            } catch (IOException e) {
                e.printStackTrace();
                // no op
            }
        }
    }

    public static Journal create(File f, Executor executor) {
        return new Journal(f, executor);
    }

    public static Journal create(File f) {
        return new Journal(f, Executors.newSingleThreadExecutor());
    }

    private Journal(File file, Executor executor) {
        mExecutor = executor;
        mOutputFileDir = file;
        mOutputFile = new File(mOutputFileDir, FILENAME);
        rebuildFromDisk();
        createWriter();
    }

    private void rebuildFromDisk() {

        mTimestamps = new HashMap<>();

        boolean created = true;
        if (!mOutputFileDir.exists()) {
            created = mOutputFileDir.mkdir();
        }

        if (!created || !mOutputFile.exists()) {
            Logger.e(TAG, "Cannot create journal output dir");
            return;
        }

        try {
            String line;
            Scanner scan = new Scanner(mOutputFile);
            while (scan.hasNextLine()) {
                line = scan.nextLine();
                final String[] values = line.split(":");
                mTimestamps.put(values[0], Long.valueOf(values[1]));
            }
        } catch (IOException e) {
            //
        }

    }

    private void createWriter() {
        try {
            mWriter = new OutputStreamWriter(new FileOutputStream(mOutputFile, true), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            Utils.closeQuietly(mWriter);
            mWriter = null;
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
                if (mWriter != null) {
                    mExecutor.execute(new JournalWriterRun(buildLine(key, timestamp)));
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private String buildLine(String key, long timestamp) {
        return key + ":" + timestamp;
    }
}

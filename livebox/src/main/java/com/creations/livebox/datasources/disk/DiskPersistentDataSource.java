package com.creations.livebox.datasources.disk;

import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox_common.util.Logger;
import com.creations.livebox.util.Optional;
import com.creations.livebox_common.serializers.Serializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

import okio.BufferedSource;
import okio.Okio;

import static com.creations.livebox.Livebox.TAG;
import static com.creations.livebox_common.util.OkioUtils.copy;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public class DiskPersistentDataSource<I, O> implements LocalDataSource<I, O> {

    private static final String SUFFIX = "_livebox.json";
    private static Config mConfig;
    private Serializer mSerializer;
    private Type mType;

    private DiskPersistentDataSource(Serializer serializer, Type type) {
        mSerializer = serializer;
        mType = type;
    }

    public static <I, O> DiskPersistentDataSource<I, O> create(Serializer serializer, Type type) {
        return new DiskPersistentDataSource<>(serializer, type);
    }

    public static void setConfig(Config config) {
        mConfig = config;
    }

    @Override
    public Optional<O> read(String key) {
        Logger.d(TAG, "Read from disk with  key: " + key);
        return readFromDisk(key);
    }

    @Override
    public void save(String key, I input) throws IllegalStateException {
        Logger.d(TAG, "Save to disk with  key: " + key);
        writeToDisk(key, mSerializer.serialize(input, mType));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void clear(String key) {
        final File outputFile = new File(mConfig.getOutputDir(), key + SUFFIX);
        if (outputFile.exists()) {
            Logger.d(TAG, "Delete file: " + outputFile.getName());
            outputFile.delete();
        }
    }

    private Optional<O> readFromDisk(String fileName) {

        final File outputFile = new File(mConfig.getOutputDir(), fileName + SUFFIX);
        if (!outputFile.canRead()) {
            return Optional.empty();
        }

        Logger.d(TAG, "File available, read it");

        O data = null;
        try {
            final BufferedSource bs = Okio.buffer(Okio.source(outputFile));
            data = mSerializer.deserialize(bs, mType);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return Optional.ofNullable(data);
    }

    private void writeToDisk(String fileName, BufferedSource input) {

        boolean created = true;
        if (!mConfig.getOutputDir().exists()) {
            created = mConfig.getOutputDir().mkdir();
        }

        // Cannot create file
        if (!created) {
            Logger.e(TAG, "Cannot create DiskPersistentDataSource output dir");
            return;
        }

        final File outputFile = new File(mConfig.getOutputDir(), fileName + SUFFIX);
        try (
                final InputStream is = input.inputStream();
                final OutputStream os = Okio.buffer(Okio.sink(outputFile)).outputStream()
        ) {
            copy(is, os);
            Logger.d(TAG, "Success data saved in diskPersistentDataSource.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return "DiskPersistentDataSource";
    }

    public static class Config {
        private File mOutputDir;

        public Config(File outputDir) {
            this.mOutputDir = outputDir;
        }

        File getOutputDir() {
            return mOutputDir;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "OutputDir=" + mOutputDir +
                    '}';
        }
    }
}

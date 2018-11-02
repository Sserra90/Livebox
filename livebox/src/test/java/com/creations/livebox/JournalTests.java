package com.creations.livebox;

import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.Journal;
import com.creations.livebox_common.util.Logger;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class JournalTests {

    private final static File RES_FILE = new File("src/test/resources");

    public JournalTests() {
        Logger.disable();
    }

    @Test
    public void writeAndReadToJournal() {

        Journal journal = Journal.create(RES_FILE);
        journal.save("key1", 10);
        journal.save("key2", 20);
        journal.save("key3", 30);
        journal.save("key4", 40);
        journal.save("key3", 60);

        assertEquals(10L, (long) journal.read("key1").get());
        assertEquals(60L, (long) journal.read("key3").get());
    }

    @Test
    public void shouldEliminateDuplicates() {

        Journal journal = Journal.create(RES_FILE);
        journal.save("key1", 10);
        journal.save("key2", 20);
        journal.save("key2", 25);
        journal.save("key3", 30);
        journal.save("key4", 40);
        journal.save("key3", 60);

        // Recreate
        journal = Journal.create(RES_FILE);

        assertEquals(4, journal.size());
        assertEquals(25L, (long) journal.read("key2").get());
        assertEquals(60L, (long) journal.read("key3").get());
    }

    @Test
    public void writeAndReadToJournalWithLimit() {

        final int limit = 300;
        Journal journal = Journal.create(RES_FILE, limit);

        // Write to file
        for (int i = 0; i <= limit + 100; i++) {
            journal.save("key" + i, i);
        }

        // Recreate
        journal = Journal.create(RES_FILE, limit);

        assertEquals(journal.size(), limit);
        assertTrue(journal.read("key0").isAbsent());
        assertTrue(journal.read("key101").isPresent());
        assertTrue(journal.read("key400").isPresent());
    }

    @Test
    public void noDuplicates() {

        final int limit = 300;
        Journal journal = Journal.create(RES_FILE, limit);

        // Write to file
        for (int i = 1, j = 0; i < limit; i++) {
            journal.save("key" + j, i);
            if (i % 2 == 0) {
                j++;
            }
        }

        // Recreate
        journal = Journal.create(RES_FILE, limit);

        assertEquals(journal.size(), limit / 2);
        assertEquals((long) journal.read("key0").get(), 2L);
        assertEquals((long) journal.read("key149").get(), 299L);
    }

    @Test
    public void multiThreadAccess() throws InterruptedException {

        final String key = "key";
        final Journal journal = Journal.create(RES_FILE);
        final AtomicReference<Optional<Long>> result = new AtomicReference<>();

        final List<Thread> threads = new ArrayList<>();
        threads.add(createWriterThread(journal, 10));
        threads.add(createWriterThread(journal, 20));
        threads.add(createWriterThread(journal, 30));
        threads.add(createWriterThread(journal, 40));
        threads.add(createWriterThread(journal, 50));

        final Thread readThread = new Thread(() -> {
            System.out.println("Thread read");
            result.set(journal.read(key));
            System.out.println("Thread read finish");
        });

        for (Thread thread : threads) {
            thread.start();
        }
        readThread.start();

        readThread.join();

        assertNotNull(result.get());
        assertEquals(50, (long) result.get().get());
    }

    private Thread createWriterThread(Journal journal, long timestamp) {
        return new Thread(new JournalWriter(journal, timestamp));
    }

    private class JournalWriter implements Runnable {
        private long value;
        private Journal journal;
        private String key = "key";

        JournalWriter(Journal journal, long value) {
            this.value = value;
            this.journal = journal;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " save");
            journal.save(key, value);
            System.out.println(Thread.currentThread().getName() + " save finish");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        if (RES_FILE.exists()) {
            File[] files = RES_FILE.listFiles((dir, name) -> name.startsWith("journal"));
            for (File file : files) {
                file.delete();
            }
        }
    }

}

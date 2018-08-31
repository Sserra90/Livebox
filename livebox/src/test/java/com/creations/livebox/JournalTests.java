package com.creations.livebox;

import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class JournalTests {

    public JournalTests() {
        Logger.disable();
    }


    @Test
    public void writeAndReadToJournal() throws InterruptedException {

        File f = new File("src/test/resources");
        Journal journal = Journal.create(f);
        journal.save("key1", 10);
        journal.save("key2", 20);
        journal.save("key3", 30);
        journal.save("key4", 40);
        journal.save("key3", 60);

        // Recreate, rebuild from file
        journal = Journal.create(f);
        journal.save("key5", 80);
        final Optional<Long> value = journal.read("key3");

        assertTrue(value.isPresent());
        assertEquals(60L, (long) value.get());
    }

    @Test
    public void multiThreadAccess() throws InterruptedException {

        final String key = "key";
        final Journal journal = Journal.create(new File("src/test/resources"));
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

        // Write threads start and join
        for (Thread thread : threads) {
            thread.start();
            thread.join();
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

}

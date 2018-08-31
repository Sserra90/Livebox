package com.creations.livebox;

import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JournalTests {

    public JournalTests() {
        Logger.disable();
    }

    @Test
    public void writeAndReadToJournal() {

        final Journal journal = Journal.create(new File("src/test/resources"));

        final long timestamp = System.currentTimeMillis();
        journal.save("key1", timestamp);
        final Optional<Long> value = journal.read("key1");

        assertTrue(value.isPresent());
        assertEquals(timestamp, (long) value.get());
    }

}

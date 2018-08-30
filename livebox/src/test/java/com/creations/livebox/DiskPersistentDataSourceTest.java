package com.creations.livebox;

import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link DiskPersistentDataSourceTest}
 *
 * @author Sérgio Serra on 25/08/2018.
 */
public class DiskPersistentDataSourceTest {

    @Before
    public void setup() {
        Logger.disable();
    }

    @Test
    public void saveAndRead() {

        // Setup
        final String key = "1000";
        LiveboxBuilder.persistentCacheConfig(new DiskPersistentDataSource.Config(
                new File("src/test/resources")
        ));

        final List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        values.add("three");
        final Bag<String> bag = new Bag<>("100", values);

        TypeToken<Bag<String>> typeToken = new TypeToken<Bag<String>>() {
        };

        // Exercise
        final DiskPersistentDataSource<Bag<String>, Bag<String>> dataSource
                = DiskPersistentDataSource.create(typeToken.getType());
        dataSource.save(key, bag);
        final Optional<Bag<String>> newBagOpt = dataSource.read(key);

        // Verify
        Assert.assertTrue(newBagOpt.isPresent());
        Assert.assertEquals(bag, newBagOpt.get());

    }

    @Test
    public void saveAndReadBigJsonToDisk() {

    }

}

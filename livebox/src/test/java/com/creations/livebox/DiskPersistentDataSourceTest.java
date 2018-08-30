package com.creations.livebox;

import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.serializers.LiveboxGsonSerializer;
import com.creations.livebox.serializers.Serializer;
import com.creations.livebox.util.Logger;
import com.creations.livebox.util.Optional;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSource;
import okio.Okio;

/**
 * Unit tests for {@link DiskPersistentDataSourceTest}
 *
 * @author Sérgio Serra on 25/08/2018.
 */
public class DiskPersistentDataSourceTest {

    @Before
    public void setup() {
        Logger.disable();
        LiveboxBuilder.persistentCacheConfig(new DiskPersistentDataSource.Config(
                new File("src/test/resources")
        ));
    }

    @Test
    public void saveAndRead() {

        // Setup
        final String key = "1000";

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

        // Setup
        InputStream is = getClass().getClassLoader().getResourceAsStream("bigJson.json");
        BufferedSource source = Okio.buffer(Okio.source(is));

        TypeToken<List<Bag<String>>> bagType = new TypeToken<List<Bag<String>>>() {
        };
        final Serializer<List<Bag<String>>> serializer = LiveboxGsonSerializer.create(bagType);
        final List<Bag<String>> bags = serializer.deserialize(source);

        // Exercise
        final String key = "2000";
        final DiskPersistentDataSource<List<Bag<String>>, List<Bag<String>>> dataSource
                = DiskPersistentDataSource.create(bagType.getType());
        dataSource.save(key, bags);
        final Optional<List<Bag<String>>> newBagOpt = dataSource.read(key);

        // Verify
        Assert.assertTrue(newBagOpt.isPresent());
        Assert.assertEquals(bags, newBagOpt.get());

    }

}
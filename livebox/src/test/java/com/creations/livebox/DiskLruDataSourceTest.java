package com.creations.livebox;

import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox_common.util.Bag;
import com.creations.livebox_common.util.Logger;
import com.creations.serializer_gson.LiveboxGsonSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.creations.livebox.LiveboxTest.testConfig;

/**
 * Unit tests for {@link DiskLruDataSource}
 *
 * @author Sérgio Serra on 25/08/2018.
 */
public class DiskLruDataSourceTest {

    @Before
    public void setup() {
        Logger.disable();
    }

    @Test
    public void saveAndRead() {
        Livebox.init(testConfig);

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
        final DiskLruDataSource<Bag<String>, Bag<String>> dataSource = DiskLruDataSource.create(
                LiveboxGsonSerializer.create(new Gson()),
                typeToken.getType()
        );
        dataSource.save(key, bag);
        final Bag<String> newBagOpt = dataSource.read(key);

        // Verify
        Assert.assertNotNull(newBagOpt);
        Assert.assertEquals(bag, newBagOpt);
    }

}

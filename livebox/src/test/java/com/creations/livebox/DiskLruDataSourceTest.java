package com.creations.livebox;

import android.os.Looper;

import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.serializers.LiveboxGsonSerializer;
import com.creations.livebox.serializers.Serializer;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for {@link DiskLruDataSource}
 *
 * @author Sérgio Serra on 25/08/2018.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Looper.class)
public class DiskLruDataSourceTest {

    @Before
    public void setup() {
        // IgDiskCache uses Looper class to check if we are running on main thread.
        // We need to mock the call otherwise we cannot run unit tests.
        PowerMockito.mockStatic(Looper.class);
        PowerMockito.when(Looper.getMainLooper()).thenReturn(Mockito.mock(Looper.class));
    }

    @Test
    public void saveAndRead() {

        // Setup
        final String key = "1000";
        Livebox.init(new DiskLruDataSource.Config(
                new File("src/test/resources"),
                10 * 1024 * 1024 // 10 mg
        ));

        final List<String> values = new ArrayList<>();
        values.add("one");
        values.add("two");
        values.add("three");
        final Bag<String> bag = new Bag<>("100", values);

        // Exercise
        final Serializer<Bag<String>> serializer = LiveboxGsonSerializer.create(Bag.class);
        final DiskLruDataSource dataSource = DiskLruDataSource.create(key);
        dataSource.save(serializer.serialize(bag));
        final Bag<String> newBag = serializer.deserialize(dataSource.read());

        // Verify
        Assert.assertEquals(bag, newBag);
    }

    @Test
    public void saveAndReadBigJsonToDisk() {

    }

}

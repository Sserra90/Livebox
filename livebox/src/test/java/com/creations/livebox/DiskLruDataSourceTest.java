package com.creations.livebox;

import android.os.Looper;

import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox_common.util.Bag;
import com.creations.serializer_gson.LiveboxGsonSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static com.creations.livebox.LiveboxTest.testConfig;

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

package com.creations.app;

import com.creations.convert_jackson.LiveboxJacksonSerializer;
import com.creations.livebox.Livebox;
import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox_common.util.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import io.reactivex.observers.TestObserver;

public class LiveboxTests {

    private final static File RES_FILE = new File("src/test/resources");

    @Before
    public void setup() {
        Logger.disable();
        Livebox.init(
                new Config()
                        .log(false)
                        .addSerializer(LiveboxJacksonSerializer.create())
                        .lruCacheConfig(new DiskLruDataSource.Config(RES_FILE, 1024 * 1024 * 10))
                        .journalDir(RES_FILE)
        );
    }

    @Test
    public void fetchUsers() {

        final UsersRepo repo = new UsersRepo();
        final TestObserver<List<Integer>> observer = new TestObserver<>();
        repo.getUsers().subscribe(observer);

        observer
                .assertComplete()
                .assertNoErrors()
                .assertValue(res -> res.size() == 1);
    }

    @After
    public void teardown() {

    }
}
package com.creations.app;

import android.os.Looper;

import com.creations.app.repo.UsersRepo;
import com.creations.convert_jackson.LiveboxJacksonSerializer;
import com.creations.livebox.Livebox;
import com.creations.livebox.adapters.AndroidAdapter;
import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.disk.DiskLruDataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Looper.class)
public class LiveboxTests {

    private final static File RES_FILE = new File("src/test/resources");

    private void mockIgDiskCache() {
        // IgDiskCache uses Looper class to check if we are running on main thread.
        // We need to mock the call otherwise we cannot run unit tests.
        PowerMockito.mockStatic(Looper.class);
        PowerMockito.when(Looper.getMainLooper()).thenReturn(Mockito.mock(Looper.class));
    }

    @Before
    public void setup() {
        mockIgDiskCache();
        Livebox.Companion.init(
                new Config()
                        .disableLog()
                        .setSchedulerProvider(new AndroidAdapter.SchedulerProvider() {
                            public Scheduler subscribe() {
                                return Schedulers.io();
                            }

                            public Scheduler observer() {
                                return Schedulers.io();
                            }
                        })
                        .addSerializer(LiveboxJacksonSerializer.Companion.create())
                        .lruCacheConfig(new DiskLruDataSource.Config(RES_FILE, 1024 * 1024 * 10))
                        .journalDir(RES_FILE)
        );
    }

    @Test
    public void fetchUsers() {

        final UsersRepo repo = new UsersRepo();
        final TestObserver<List<Integer>> observer = new TestObserver<>();
        repo.getUsers().subscribe(observer);

        observer.awaitTerminalEvent();
        observer
                .assertComplete()
                .assertNoErrors()
                .assertValue(List::isEmpty);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void teardown() {
        if (RES_FILE.exists()) {
            for (File file : RES_FILE.listFiles()) {
                file.delete();
            }
        }
    }
}
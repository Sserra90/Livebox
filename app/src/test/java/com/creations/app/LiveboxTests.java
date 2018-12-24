package com.creations.app;

import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;

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
        /*mockIgDiskCache();
        Livebox.init(
                new Config(
                        new DiskLruConfig(RES_FILE, 1024 * 1024 * 10),
                        new DiskPersistentConfig(RES_FILE),
                        LiveboxJacksonSerializer.create(new ObjectMapper()),
                        RES_FILE,
                        true
                )
        );*/
    }

    /*
    @Test
    public void fetchUsers() {

        final UsersRepo repo = UsersRepo();
        final TestObserver<List<Integer>> observer = new TestObserver<>();
        repo.getGetUsers().subscribe(observer);

        observer.awaitTerminalEvent();
        observer
                .assertComplete()
                .assertNoErrors()
                .assertValue(List::isEmpty);
    }*/

    @Test
    public void fakeTest() {

        InputStream is = getClass().getClassLoader().getResourceAsStream("assets/users/users_success.json");
        System.out.println("is:" + is);
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
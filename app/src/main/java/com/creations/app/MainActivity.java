package com.creations.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.creations.app.api.Api;
import com.creations.app.api.GithubService;
import com.creations.app.api.UsersRes;
import com.creations.app.entities.Users;
import com.creations.livebox.Livebox;
import com.creations.livebox.LiveboxBuilder;
import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.disk.DiskPersistentDataSource;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;
import com.creations.livebox.util.Utils;
import com.creations.livebox.validator.Validator;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String LRU_DISK_CACHE_DIR = "livebox_disk_lru_cache";
    private static final String PERSISTENT_DISK_CACHE_DIR = "livebox_disk_persistent_cache";

    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100; // 100MB
    private static final int DEFAULT_DISK_CACHE_SIZE_PERCENT = 10; // 10% of free disk space

    private Livebox<UsersRes, Users> usersBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> getUsers());

        // Init livebox
        final File lruCacheDir = Utils.getCacheDirectory(this, LRU_DISK_CACHE_DIR);
        final long lurCacheSize = Utils.getCacheSizeInBytes(
                lruCacheDir,
                DEFAULT_DISK_CACHE_SIZE_PERCENT / 100F,
                DEFAULT_DISK_CACHE_SIZE
        );

        final File persistentCacheDir = Utils.getCacheDirectory(this, PERSISTENT_DISK_CACHE_DIR);
        LiveboxBuilder.persistentCacheConfig(new DiskPersistentDataSource.Config(persistentCacheDir));
        LiveboxBuilder.lruCacheConfig(new DiskLruDataSource.Config(lruCacheDir, lurCacheSize));


        final GithubService service = Api.getInstance().getGithubService();

        Validator<UsersRes> persistentDiskValidator = item -> Objects.nonNull(item) && !item.getItems().isEmpty();
        Validator<UsersRes> diskValidator = item -> Objects.nonNull(item) && !item.getItems().isEmpty();
        Validator<UsersRes> memoryValidator = item -> Objects.nonNull(item) && !item.getItems().isEmpty();

        //TypeToken<List<UsersRes>> token = new TypeToken<List<UsersRes>>() {};
        usersBox = new LiveboxBuilder<UsersRes, Users>()
                .withKey("get_users")
                .fetch(service::getUserList, UsersRes.class)
                .addSource(Sources.MEMORY_LRU, memoryValidator)
                .addSource(Sources.DISK_PERSISTENT, persistentDiskValidator)
                //.addSource(Sources.DISK_LRU, diskValidator)
                //.addSource(DiskLruDataSource.create(UsersRes.class), diskValidator)
                .addConverter(UsersRes.class, usersRes -> Optional.of(Users.fromUsersRes(usersRes)))
                .retryOnFailure()
                .build();

    }

    @SuppressLint("CheckResult")
    private void getUsers() {

        //liveData.observe(this, users -> Log.d(TAG, "UsersRes: " + users));
        usersBox.asAndroidObservable()
                .subscribe(users -> Log.d(TAG, "UsersRes: " + users), Throwable::printStackTrace);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

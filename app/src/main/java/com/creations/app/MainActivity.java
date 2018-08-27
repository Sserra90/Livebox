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
import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.datasources.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;
import com.creations.livebox.util.Utils;
import com.creations.livebox.validator.Validator;

import java.io.File;

import io.reactivex.Observable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String DISK_CACHE_DIR = "livebox_disk_lru_cache";
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 100; // 100MB
    private static final int DEFAULT_DISK_CACHE_SIZE_PERCENT = 10; // 10% of free disk space

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> getUsers());

        // Init livebox
        final File cacheDir = Utils.getCacheDirectory(this, DISK_CACHE_DIR);
        DiskLruDataSource.Config diskCacheConfig = new DiskLruDataSource.Config(
                cacheDir,
                Utils.getCacheSizeInBytes(
                        cacheDir,
                        DEFAULT_DISK_CACHE_SIZE_PERCENT / 100F,
                        DEFAULT_DISK_CACHE_SIZE
                )
        );
        Livebox.init(diskCacheConfig);

    }

    @SuppressLint("CheckResult")
    private void getUsers() {
        final GithubService service = Api.getInstance().getGithubService();

        Validator<UsersRes> diskValidator = item -> Objects.nonNull(item) && !item.getItems().isEmpty();

        //TypeToken<List<UsersRes>> token = new TypeToken<List<UsersRes>>() {};
        Livebox<UsersRes, Users> box = new Livebox<>("get_users");
        Observable<Users> usersObservable = box
                .fetch(service::getUserList, UsersRes.class)
                .addSource(Sources.DISK_LRU, diskValidator)
                /*.addLocalSourceFactory(new DataSourceFactory<UsersRes>() {
                    @Override
                    public <T> Optional<LocalDataSource<UsersRes, T>> get(int id) {
                        return Optional.of(DiskLruDataSource.create("", Integer.class));
                    }
                })*/
                //.addSource(DiskLruDataSource.create("get_users", UsersRes.class), diskValidator)
                .addConverter(UsersRes.class, usersRes -> Optional.of(Users.fromUsersRes(usersRes)))
                .retryOnFailure()
                //.keepDataFresh()
                .asAndroidObservable();

        //liveData.observe(this, users -> Log.d(TAG, "UsersRes: " + users));
        usersObservable.subscribe(users -> Log.d(TAG, "UsersRes: " + users), Throwable::printStackTrace);

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

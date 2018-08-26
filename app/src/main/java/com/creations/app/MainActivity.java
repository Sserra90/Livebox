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
import com.creations.app.api.Users;
import com.creations.livebox.Livebox;
import com.creations.livebox.datasources.DiskLruDataSource;
import com.creations.livebox.util.Utils;

import java.io.File;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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

        Observable<Users> usersObservable = Livebox
                .<Users, Users, Users>build(new Livebox.BoxKey("get_users"))
                .remoteDataSource(() -> service.getUserList().toFlowable(BackpressureStrategy.BUFFER))
                .localDataSource(DiskLruDataSource.create("get_users", Users.class))
                .remoteDataSourceMapper(users -> users)
                .localDataSourceMapper(users -> users)
                .setDataValidator(item -> true)
                .asObservable();

        usersObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(users -> Log.d(TAG, "Users: " + users), Throwable::printStackTrace);

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

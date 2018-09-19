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
import com.creations.livebox.adapters.LiveDataAdapter;
import com.creations.livebox.config.Config;
import com.creations.livebox.datasources.disk.DiskLruDataSource;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;
import com.creations.livebox.datasources.fetcher.Fetcher;
import com.creations.livebox.datasources.fetcher.FileFetcher;
import com.creations.livebox.util.Objects;
import com.creations.livebox.util.Optional;
import com.creations.livebox.validator.AgeValidator;
import com.creations.livebox.validator.Validator;
import com.creations.serializer_gson.LiveboxGsonSerializer;
import com.creations.serializer_gson.Utils;
import com.google.gson.reflect.TypeToken;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Livebox<UsersRes, Users> usersBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> getUsers());

        final GithubService service = Api.getInstance().getGithubService();

        Livebox.init(this, LiveboxGsonSerializer.create());
        /*Livebox.init(new Config()
                .lruCacheConfig(new DiskLruDataSource.Config(
                        new File("somePath"), 10
                ))
                .addSerializer(LiveboxGsonSerializer.create())
        );*/

        Validator<UsersRes> persistentDiskValidator = (key, item) -> Objects.nonNull(item) && !item.getItems().isEmpty();
        Validator<UsersRes> diskValidator = (key, item) -> Objects.nonNull(item) && !item.getItems().isEmpty();
        Validator<UsersRes> memoryValidator = (key, item) -> Objects.nonNull(item) && !item.getItems().isEmpty();

        // 2 minutes TTL(time to live)
        Validator<UsersRes> ageValidator = AgeValidator.create(TimeUnit.MINUTES.toMillis(2));
        try {

            //TypeToken<List<UsersRes>> token = new TypeToken<List<UsersRes>>() {};
            Type type = Utils.fromType(new TypeToken<UsersRes>() {
            });
            final Fetcher<UsersRes> fileFetcher = FileFetcher.create(
                    this, "user_res.json", UsersRes.class, LiveboxGsonSerializer.create()
            );

            usersBox = new LiveboxBuilder<UsersRes, Users>()
                    .withKey("get_users")
                    //.fetch(fileFetcher, UsersRes.class)
                    .fetch(service::getUserList, UsersRes.class)
                    .addSource(Sources.MEMORY_LRU, ageValidator)
                    .addSource(Sources.DISK_PERSISTENT, persistentDiskValidator)
                    //.addSource(Sources.DISK_LRU, diskValidator)
                    //.addSource(DiskLruDataSource.create(UsersRes.class), diskValidator)
                    .addConverter(UsersRes.class, usersRes -> Optional.of(Users.fromUsersRes(usersRes)))
                    .retryOnFailure()
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("CheckResult")
    private void getUsers() {

        //usersBox.as(new LiveDataAdapter<>()).observe(this, users -> Log.d(TAG, "UsersRes: " + users));
        //usersBox.asLiveData().observe(this, users -> Log.d(TAG, "UsersRes: " + users));

        //liveData.observe(this, users -> Log.d(TAG, "UsersRes: " + users));
        usersBox.scoped(AndroidLifecycleScopeProvider.from(this))
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

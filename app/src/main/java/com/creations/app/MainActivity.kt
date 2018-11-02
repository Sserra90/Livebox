package com.creations.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.creations.app.api.Api
import com.creations.app.api.UsersRes
import com.creations.app.entities.Users
import com.creations.app.repo.UsersRepo
import com.creations.app.vm.UsersVm
import com.creations.livebox.Livebox
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources
import com.creations.livebox.validator.minutes
import com.fixeads.adapter_autodispose.AutoDisposeAdapter
import com.fixeads.adapter_livedata.LiveDataAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sserra.livebox_jackson.box
import com.sserra.livebox_jackson.config
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import java.util.*

class MainActivity : AppCompatActivity() {

    private val usersBox: Livebox<UsersRes, Users>? = null
    private lateinit var box: Livebox<List<String>, List<Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { getUsers() }

        val service = Api.getInstance().githubService

        Livebox.init(config(this))
        //val fileFetcher = fileFetcher<UsersRes>(this,"user_res.json")

        box = box<List<String>, List<Int>>()
                .withKey("some_key")
                .fetch { Observable.just(listOf("1")) }
                .addSource<List<String>>(Sources.DISK_LRU, 2.minutes())
                .addConverter<List<Int>> { listOf() }
                .build()

        val usersVm = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (UsersVm::class.java.isAssignableFrom(modelClass)) {
                    @Suppress("UNCHECKED_CAST")
                    return UsersVm(UsersRepo()) as T
                }
                throw IllegalArgumentException()
            }
        })[UsersVm::class.java]

        usersVm.usersLiveData.observe(this, androidx.lifecycle.Observer {
            Log.d(TAG, "Users: $it")
        })
        usersVm.getUsers()
    }

    @SuppressLint("CheckResult")
    private fun getUsers() {

        /*usersBox.`as`(LiveDataAdapter()).observe(this, android.arch.lifecycle.Observer {

        })*/
        /*usersBox?.asLiveData(this) {
            Log.d(TAG, "UsersRes: $it")
        }*/
        //usersBox.asLiveData().observe(this, users -> Log.d(TAG, "UsersRes: " + users));

        //liveData.observe(this, users -> Log.d(TAG, "UsersRes: " + users));
        box.`as`(AutoDisposeAdapter.of(AndroidLifecycleScopeProvider.from(this), android = true))
                .subscribe(
                        { data -> Log.d(TAG, "Data: $data") },
                        { it.printStackTrace() }
                )

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = "MainActivity"
        private val LRU_DISK_CACHE_DIR = "livebox_disk_lru_cache"
    }
}

fun <I, O> Livebox<I, O>.asLiveData(owner: LifecycleOwner, block: (O) -> Unit = {}) {
    `as`(LiveDataAdapter()).observe(owner, Observer<O> {
        it?.let(block)
    })
}

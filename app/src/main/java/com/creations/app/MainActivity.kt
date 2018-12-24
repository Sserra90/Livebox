package com.creations.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.creations.app.databinding.ActivityMainBinding
import com.creations.app.room.Db
import com.creations.app.vm.UsersVm
import com.creations.app.vm.VmFactory
import com.creations.livebox.Livebox
import com.fixeads.adapter_livedata.LiveDataAdapter
import com.sserra.livebox_jackson.config
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var box: Livebox<List<String>, List<Int>>
    private lateinit var usersVm: UsersVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Db.appContext = applicationContext
        Livebox.init(config(this))

        val db = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        setSupportActionBar(db.toolbar)
        db.fab.setOnClickListener { getUsers() }

        //val fileFetcher = fileFetcher<UsersRes>(this,"user_res.json")

        /*box = box<List<String>, List<Int>>()
                .withKey("some_key")
                .fetch { Observable.just(listOf("1")) }
                .addSource<List<String>>(Sources.DISK_LRU, 2.minutes())
                .addConverter<List<Int>> { listOf(1) }
                .retryOnFailure()
                .build()*/

        usersVm = ViewModelProviders.of(this, VmFactory())[UsersVm::class.java]
        usersVm.usersLiveData.observe(this, androidx.lifecycle.Observer {
            Log.d(TAG, "Users: $it")
            data.text = it.toString()
        })

        db.setLifecycleOwner(this)
        db.vm = usersVm
    }

    @SuppressLint("CheckResult")
    private fun getUsers() {

        //usersBox.asLiveData().observe(this, users -> Log.d(TAG, "UsersRes: " + users));

        /*box.`adapt`(AutoDisposeAdapter.of(AndroidLifecycleScopeProvider.from(this)))
                .subscribe(
                        { data -> Log.d(TAG, "Data: $data") },
                        { it.printStackTrace() }
                )

        val obs = box.adapt(StateAdapter())

        obs.subscribe {
            when (it.status) {
                Error -> Log.d(TAG, "Error state: $it")
                Loading -> Log.d(TAG, "Loading state: $it")
                Success -> Log.d(TAG, "Success state: $it")
            }
        }*/

         usersVm.getUsers()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // adapt you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

fun <I, O> Livebox<I, O>.asLiveData(owner: LifecycleOwner, block: (O) -> Unit = {}) {
    adapt(LiveDataAdapter()).observe(owner, Observer<O> {
        it?.let(block)
    })
}

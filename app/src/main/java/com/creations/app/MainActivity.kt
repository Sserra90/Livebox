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
import androidx.lifecycle.ViewModelProviders
import com.creations.app.room.Db
import com.creations.app.vm.UsersVm
import com.creations.app.vm.VmFactory
import com.creations.livebox.Livebox
import com.fixeads.adapter_livedata.LiveDataAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sserra.livebox_jackson.config
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var box: Livebox<List<String>, List<Int>>
    private lateinit var usersVm: UsersVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { getUsers() }

        Db.appContext = applicationContext
        Livebox.init(config(this))

        //val fileFetcher = fileFetcher<UsersRes>(this,"user_res.json")

        /*box = box<List<String>, List<Int>>()
                .withKey("some_key")
                .fetch { Observable.just(listOf("1")) }
                .addSource<List<String>>(Sources.DISK_LRU, 2.minutes())
                .addConverter<List<Int>> { listOf() }
                .build()*/

        usersVm = ViewModelProviders.of(this, VmFactory())[UsersVm::class.java]
        usersVm.usersLiveData.observe(this, androidx.lifecycle.Observer {
            Log.d(TAG, "Users: $it")
            data.text = it.toString()
        })
    }

    @SuppressLint("CheckResult")
    private fun getUsers() {

        //usersBox.asLiveData().observe(this, users -> Log.d(TAG, "UsersRes: " + users));

        /*box.`as`(AutoDisposeAdapter.of(AndroidLifecycleScopeProvider.from(this), android = true))
                .subscribe(
                        { data -> Log.d(TAG, "Data: $data") },
                        { it.printStackTrace() }
                )*/

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
        // as you specify a parent activity in AndroidManifest.xml.
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
    `as`(LiveDataAdapter()).observe(owner, Observer<O> {
        it?.let(block)
    })
}

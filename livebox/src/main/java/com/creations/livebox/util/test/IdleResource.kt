package com.creations.livebox.util.test

import androidx.test.espresso.idling.CountingIdlingResource

// Static reference to idle resource
object CountingIdleResource {

    val resource: CountingIdlingResource =
            CountingIdlingResource("global_counting_resource")

    fun increment() {
        resource.increment()
    }

    fun decrement() {
        resource.decrement()
    }
}

package com.creations.livebox.validator

import com.creations.livebox.Livebox
import java.util.concurrent.TimeUnit.*

/**
 * @author Sérgio Serra on 01/09/2018.
 * sergioserra99@gmail.com
 */
class AgeValidator<R> private constructor(
        private val mJournal: Journal?,
        // Time to live before expiring the resources
        private val mTtl: Long
) : Validator<R> {

    override fun validate(key: String, item: R): Boolean {

        if (mJournal == null) return true

        val lastTimestampOpt = mJournal.read(key)
        // By default if no timestamp is available resource is valid
        return lastTimestampOpt.isAbsent || lastTimestampOpt.get() + mTtl >= System.currentTimeMillis()
    }

    companion object {
        @JvmStatic
        fun <T> create(ttl: Long): AgeValidator<T> = AgeValidator(Livebox.journal, ttl)

        @JvmStatic
        fun <T> minutes(minutes: Int): AgeValidator<T> = create(MINUTES.toMillis(minutes.toLong()))

        @JvmStatic
        fun <T> hours(hours: Int): AgeValidator<T> = create(HOURS.toMillis(hours.toLong()))

        @JvmStatic
        fun <T> days(days: Int): AgeValidator<T> = create(DAYS.toMillis(days.toLong()))
    }
}

fun <T> Int.minutes(): AgeValidator<T> = AgeValidator.minutes(this)
fun <T> Int.hours(): AgeValidator<T> = AgeValidator.hours(this)
fun <T> Int.days(): AgeValidator<T> = AgeValidator.days(this)

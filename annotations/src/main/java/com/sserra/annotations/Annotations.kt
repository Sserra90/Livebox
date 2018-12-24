package com.sserra.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Assets(val folder: String = "", val name: String = "", val mapsTo: KClass<*>)
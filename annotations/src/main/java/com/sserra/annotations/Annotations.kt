package com.sserra.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Assets(val folder: String = "", val name: String = "")
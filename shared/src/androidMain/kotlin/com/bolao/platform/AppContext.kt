package com.bolao.platform

import android.content.Context

/**
 * Singleton para expor o ApplicationContext no módulo shared/androidMain.
 * Inicializado pela Application class do androidApp.
 */
object AppContext {
    private lateinit var appContext: Context
    fun init(context: Context) { appContext = context.applicationContext }
    fun get(): Context = appContext
}

package com.chatwing

import android.app.Application
import android.util.Log
import com.chatwing.db.AppDatabase
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor

/**
 * ChatWing Application 入口
 * 初始化数据库、Flutter Engine 缓存
 */
class ChatWingApplication : Application() {

    companion object {
        private const val TAG = "ChatWingApp"
        lateinit var instance: ChatWingApplication; private set
        lateinit var database: AppDatabase; private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        Log.i(TAG, "ChatWing 应用初始化完成")
    }

    /**
     * 预加载 Flutter Engine（提升首次启动速度）
     */
    fun preloadFlutterEngine() {
        val flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        FlutterEngineCache.getInstance().put("chatwing_engine", flutterEngine)
    }
}

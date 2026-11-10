package dev.podcast.nativeapp

import android.app.Application
import android.os.Build
import android.os.ProfilingManager
import android.os.ProfilingTrigger
import android.os.ProfilingResult
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

class ProfileLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 37) {
            Android17Profiles.register(this)
        }
    }
}

@RequiresApi(37)
private object Android17Profiles {
    private const val TAG = "CastflowProfiles"

    fun register(app: Application) {
        val manager = app.getSystemService(ProfilingManager::class.java) ?: return
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                // API 36 exposes the builder; these two public constants were added in API 37.
                val triggers = listOf("TRIGGER_TYPE_COLD_START", "TRIGGER_TYPE_ANOMALY").map { name ->
                    val type = ProfilingTrigger::class.java.getField(name).getInt(null)
                    ProfilingTrigger.Builder(type).build()
                }
                manager.registerForAllProfilingResults(executor) { result ->
                    if (result.errorCode == ProfilingResult.ERROR_NONE) {
                        Log.i(TAG, "System profiling result available in private storage")
                    } else {
                        Log.i(TAG, "System profiling result code=" + result.errorCode)
                    }
                }
                manager.addProfilingTriggers(triggers)
                Log.i(TAG, "Cold-start and anomaly triggers registered")
            } catch (e: ReflectiveOperationException) {
                Log.w(TAG, "Profiling triggers unavailable: " + e.javaClass.simpleName)
            } catch (e: RuntimeException) {
                Log.w(TAG, "Profiling service unavailable: " + e.javaClass.simpleName)
            }
        }
    }
}

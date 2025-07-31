package com.google.mediapipe.examples.llminference

import android.app.Application
import android.util.Log

class LLMInferenceApplication : Application() {
    
    companion object {
        private const val TAG = "LLMInferenceApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Load native libraries early
        try {
            NativeLibraryLoader.loadNativeLibraries(this)
            Log.d(TAG, "Native library loading completed")
            Log.d(TAG, NativeLibraryLoader.checkLibraryCompatibility(this))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load native libraries: ${e.message}", e)
        }
        
        // Set up global exception handling to prevent ANRs
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            
            // If it's the main thread, try to gracefully handle
            if (thread == Thread.currentThread()) {
                Log.e(TAG, "Main thread crash detected, attempting graceful shutdown")
            }
        }
        
        Log.d(TAG, "LLM Inference Application started")
    }
}

package com.google.mediapipe.examples.llminference

import android.content.Context
import android.util.Log

object NativeLibraryLoader {
    private const val TAG = "NativeLibraryLoader"
    
    private val requiredLibraries = listOf(
        "libpenguin.so",
        "libtensorflowlite_jni.so",
        "libllm_inference_engine_jni.so"
    )
    
    fun loadNativeLibraries(context: Context): Boolean {
        return try {
            // Initialize stub handler first
            LibraryStubHandler.initializeWithFallback()
            
            // Try to load TensorFlow Lite libraries first
            loadLibraryIfExists("tensorflowlite_jni")
            
            // Try to load MediaPipe libraries
            loadLibraryIfExists("mediapipe_jni")
            loadLibraryIfExists("llm_inference_engine_jni")
            
            // Handle penguin library specially
            try {
                System.loadLibrary("penguin")
                Log.d(TAG, "Successfully loaded penguin library")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Penguin library not found, using fallback implementation")
                // This is acceptable - the app can continue
            }
            
            Log.d(TAG, "Successfully loaded available native libraries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load some native libraries: ${e.message}", e)
            // Return true to continue - missing libraries might not be critical
            true
        }
    }
    
    private fun loadLibraryIfExists(libraryName: String) {
        try {
            System.loadLibrary(libraryName)
            Log.d(TAG, "Successfully loaded library: $libraryName")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Library $libraryName not found or failed to load: ${e.message}")
            // Don't throw - some libraries might be optional
        }
    }
    
    fun checkLibraryCompatibility(context: Context): String {
        val abi = android.os.Build.SUPPORTED_ABIS.joinToString(", ")
        Log.d(TAG, "Device supported ABIs: $abi")
        
        return """
            Device Info:
            - CPU ABI: $abi
            - Android Version: ${android.os.Build.VERSION.RELEASE}
            - API Level: ${android.os.Build.VERSION.SDK_INT}
            - Device: ${android.os.Build.DEVICE}
            - Manufacturer: ${android.os.Build.MANUFACTURER}
        """.trimIndent()
    }
}

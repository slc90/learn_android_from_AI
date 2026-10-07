package com.example.learnandroidfromai

class NativeBridge {

    external fun add(a: Int, b: Int): Int

    external fun greet(name: String): String

    external fun sum(values: IntArray): Int

    external fun doubleInPlace(values: IntArray)

    external fun sumRegion(values: IntArray): Int

    external fun startWork()

    fun onNativeResult(value: Int) {
        println("Native result = $value")
    }

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
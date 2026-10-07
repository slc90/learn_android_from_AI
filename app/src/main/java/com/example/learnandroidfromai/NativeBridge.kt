package com.example.learnandroidfromai

import com.example.learnandroidfromai.model.Person

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

    external fun describePerson(person: Person): String

    external fun createPerson(): Person

    external fun describePeople(people: List<Person>): String

    external fun createPeople(): List<Person>

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
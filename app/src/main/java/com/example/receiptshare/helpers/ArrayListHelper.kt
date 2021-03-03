package com.example.receiptshare.helpers

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

fun <T> ArrayList<T>.findSecondLargest(): T? {
    return if (this.isEmpty() || this.size == 1) null
    else {
        try {
            val tempSet = HashSet(this)
            val sortedSet = TreeSet(tempSet)
            sortedSet.elementAt(sortedSet.size - 2)
        } catch (e: Exception) {
            null
        }
    }
}
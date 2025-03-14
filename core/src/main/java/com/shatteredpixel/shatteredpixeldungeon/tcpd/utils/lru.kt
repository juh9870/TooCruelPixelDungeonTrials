package com.shatteredpixel.shatteredpixeldungeon.tcpd.utils

class LRUCache<K, V>(private val capacity: Int) : LinkedHashMap<K, V>(capacity, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>?): Boolean {
        return size > capacity
    }
}
package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.watabou.utils.Bundle

fun bundleFromString(data: String): Bundle {
    return Bundle.read(data.byteInputStream())
}

fun tryBundleFromString(data: String): Bundle? {
    return try {
        bundleFromString(data)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
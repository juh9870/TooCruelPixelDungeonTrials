package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.items.Heap
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.filterMapInPlace

inline fun Heap.transformItems(crossinline cb: (Item) -> Item?) {
    items.filterMapInPlace(cb)
    sprite?.link()
}

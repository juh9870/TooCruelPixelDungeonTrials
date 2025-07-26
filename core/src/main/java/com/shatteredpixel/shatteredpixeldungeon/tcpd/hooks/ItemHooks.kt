package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier

fun Item.doPickUpHook(): Boolean {
    if (Modifier.COMPLETE_KNOWLEDGE.active()) {
        identify()
    }
    return true
}

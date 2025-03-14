package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.margins

private val CHROME_MARGIN_CACHE: Array<Margins> = Array(Chrome.Type.entries.size) { i ->
    Chrome.get(Chrome.Type.entries[i]).margins()
}

fun Chrome.Type.margins(): Margins {
    return CHROME_MARGIN_CACHE[this.ordinal]
}
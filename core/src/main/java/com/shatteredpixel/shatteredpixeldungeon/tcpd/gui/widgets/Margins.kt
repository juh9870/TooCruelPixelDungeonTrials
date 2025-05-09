package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.InnerResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui

class Margins(
    val margins: Margins,
) {
    inline fun <T> show(
        ui: Ui,
        crossinline block: () -> T,
    ): InnerResponse<T> =
        ui.withLayout(
            margins = margins,
            block = block,
        )
}

inline fun <T> Ui.margins(
    margins: Margins,
    crossinline block: () -> T,
): InnerResponse<T> = Margins(margins).show(this, block)

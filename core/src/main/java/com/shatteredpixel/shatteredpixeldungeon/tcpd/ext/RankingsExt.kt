package com.shatteredpixel.shatteredpixeldungeon.tcpd.ext

import com.shatteredpixel.shatteredpixeldungeon.Rankings
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDGameInfoData

fun Rankings.Record.tcpdDataReadOnly(): TCPDGameInfoData? = this.gameData?.get(Rankings.TCPD_DATA) as? TCPDGameInfoData

fun Rankings.Record.setTCPDData(data: TCPDGameInfoData) {
    if (this.gameData == null) {
        return
    }
    this.gameData.put(Rankings.TCPD_DATA, data)
}

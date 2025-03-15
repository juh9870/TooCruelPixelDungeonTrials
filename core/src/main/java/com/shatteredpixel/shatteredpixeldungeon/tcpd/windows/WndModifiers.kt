package com.shatteredpixel.shatteredpixeldungeon.tcpd.windows

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifiers
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDGameInfoData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdComponent
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdWindow
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.PaginatedList
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.iconButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.label
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.redCheckbox
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.rightToLeft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.shrinkToFitLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.verticalJustified
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.ui.Window.TITLE_COLOR
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput

open class WndModifiers(
    private val modifiers: Modifiers,
    private val trial: Trial?,
    private val editable: Boolean
) :
    TcpdWindow() {

    constructor(data: TCPDData) : this(data.modifiers, data.trial, false)
    constructor(data: TCPDGameInfoData) : this(data.modifiers, data.trials, false)

    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        drawModifiers(modifiers, trial, editable)
    }
}

open class ModifiersComponent(
    private val modifiers: Modifiers,
    private val trial: Trial?,
    private val editable: Boolean
) :
    TcpdComponent() {

    constructor(data: TCPDData) : this(data.modifiers, data.trial, false)
    constructor(data: TCPDGameInfoData) : this(data.modifiers, data.trials, false)

    override fun Ui.drawUi() {
        drawModifiers(modifiers, trial, editable)
    }
}

fun Ui.drawModifiers(modifiers: Modifiers, trial: Trial?, editable: Boolean) {
    verticalJustified {
        rightToLeft {
            iconButton(Icons.SCROLL_COLOR.descriptor()).onClick {
                ShatteredPixelDungeon.scene().add(
                    object : WndTextInput(
                        Messages.get(WndModifiers::class.java, "edit_title"),
                        Messages.get(WndModifiers::class.java, "edit_body"),
                        modifiers.serializeToString(),
                        256,
                        false,
                        Messages.get(WndModifiers::class.java, "edit_apply"),
                        Messages.get(WndModifiers::class.java, "edit_cancel"),
                    ) {
                        override fun onSelect(positive: Boolean, text: String) {
                            if (positive && editable) {
                                try {
                                    val trimmed = text.trim()
                                    if (trimmed.isBlank()) {
                                        modifiers.disableAll()
                                    } else {
                                        modifiers.enableFrom(
                                            Modifiers.deserializeFromString(
                                                trimmed
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    ShatteredPixelDungeon.scene().add(
                                        WndError(
                                            Messages.get(
                                                WndModifiers::class.java,
                                                "edit_error"
                                            )
                                        )
                                    )
                                }
                            }
                            super.onSelect(positive, text)
                        }
                    }
                )
            }
            verticalJustified {
                label(Messages.get(WndModifiers::class.java, "title"), 12).widget.hardlight(
                    TITLE_COLOR
                )
            }
        }

        if (trial != null) {
            shrinkToFitLabel(Messages.get(WndModifiers::class.java, "trial", trial.name), 9)
        }

        top().addSpace(2)

        val modifiersList by useMemo(Unit) {
            val allEnabled = Modifier.entries.filter { modifiers.isEnabled(it) }
            if (!editable) return@useMemo allEnabled

            val allDisabled = Modifier.entries.filter { !modifiers.isEnabled(it) }
            allEnabled + allDisabled
        }
        PaginatedList(modifiersList.size, 17).show(this) { i ->
            modifierBtn(modifiers, modifiersList[i], editable)
        }
    }
}

private fun Ui.modifierBtn(modifiers: Modifiers, modifier: Modifier, editable: Boolean) {
    rightToLeft {
        margins(Margins.only(top = 2)) {
            iconButton(Icons.INFO.descriptor()).onClick {
                ShatteredPixelDungeon.scene().add(
                    WndMessage(modifier.localizedDesc())
                )
            }
        }
        verticalJustified {
            withEnabled(editable) {
                redCheckbox(modifiers.isEnabled(modifier), modifier.localizedName(), 9).onClick {
                    modifiers.toggle(modifier)
                }
            }
        }
    }
}
package com.shatteredpixel.shatteredpixeldungeon.tcpd.windows

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TrialGroup
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trials
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdWindow
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.LoopingState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.NinePatchDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.PaginatedList
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.RED_BUTTON_MARGINS
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.activeLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.columns
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.drawRedCheckbox
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.horizontal
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.iconButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.image
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.label
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.redButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.rightToLeft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.shrinkToFitLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.spacer
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.verticalJustified
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.easeOutBack
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.ui.Window
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput
import com.watabou.gltextures.TextureCache
import com.watabou.glwrap.Texture
import com.watabou.noosa.Game
import com.watabou.utils.ColorMath
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.roundToInt
import kotlin.math.sin

class WndTrials : TcpdWindow() {
    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        verticalJustified {
            verticalJustified {
                activeLabel(Messages.get(WndTrials::class.java, "title"), 12).widget.hardlight(
                    TITLE_COLOR
                )
            }
            top().addSpace(2)
            columns(floatArrayOf(1f, 1f, 1f)) {
                redButton(
                    margins = Margins.ZERO
                ) {
                    verticalJustified {
                        label(Messages.get(WndTrials::class.java, "custom"), 9)
                    }
                }.onClick {
                    Trials.curTrial = Trial.CUSTOM
                    val modifiers = Trial.CUSTOM.getModifiers()!!
                    ShatteredPixelDungeon.scene().add(object : WndModifiers(modifiers, true) {
                        override fun onBackPressed() {
                            super.onBackPressed()
                            Trial.CUSTOM.setModifiers(modifiers)
                        }
                    })
                }
                redButton(
                    margins = Margins.ZERO
                ) {
                    verticalJustified {
                        activeLabel(Messages.get(WndTrials::class.java, "edit"), 9)
                    }
                }.onClick {
                    //TODO: edit mode
                }
                redButton(
                    margins = Margins.ZERO
                ) {
                    verticalJustified {
                        activeLabel(Messages.get(WndTrials::class.java, "add"), 9)
                    }
                }.onClick {
                    ShatteredPixelDungeon.scene().add(object : WndTextInput(
                        Messages.get(WndTrials::class.java, "add_title"),
                        Messages.get(WndTrials::class.java, "add_body"),
                        "",
                        1024,
                        false,
                        Messages.get(WndTrials::class.java, "add_confirm"),
                        Messages.get(WndTrials::class.java, "add_cancel"),
                    ) {
                        override fun onSelect(positive: Boolean, text: String?) {
                            if (!positive || text.isNullOrBlank()) return

                            try {
                                URL(text)
                            } catch (e: MalformedURLException) {
                                ShatteredPixelDungeon.scene().add(
                                    WndError(
                                        Messages.get(
                                            WndTrials::class.java, "bad_url", e.message
                                        )
                                    )
                                )
                                return
                            }

                            Trials.addGroup(text)

                            Trials.checkForUpdates() // TODO: individual updates?
                        }
                    })
                }
            }

            updateBtn()

            val trials = Trials.load()
            val sortedGroups = trials.getGroups().sortedWith(compareBy({
                it.updateError == null
            }, {
                !it.wantNotify
            }))
            PaginatedList(
                sortedGroups.size, 14, bodyBackground = NinePatchDescriptor.TextureId(
                    GROUPS_LIST_BG_KEY, Margins.only(top = 1)
                ), bodyMargins = Margins.only(top = 2, bottom = 2)
            ).show(this) { i ->
                trialGroupButton(sortedGroups[i])
            }
        }
    }
}

private val GROUPS_LIST_BG_KEY by lazy {
    val key = object {}

    val tx = TextureCache.create(key, 1, 2)

    tx.filter(Texture.NEAREST, Texture.NEAREST)
    tx.wrap(Texture.CLAMP, Texture.CLAMP)
    val pm = tx.bitmap

    // RGBA format here
    pm.drawPixel(0, 0, 0X000000FFu.toInt())
    pm.drawPixel(0, 1, 0XC5BC9FFFu.toInt())

    return@lazy key
}

fun Ui.updateBtn() {
    val trials = Trials.load()
    var doASpin by useState(Unit) { false }
    val updatingCount = trials.getGroups().count { it.isUpdating }
    val spinner by useMemo(Unit) { LoopingState() }
    val actualUpdating = doASpin || updatingCount > 0
    val rotProgress = spinner.animate(actualUpdating, 1f, 0.5f) { easeOutBack(it) }
    val visualUpdating = actualUpdating || (spinner.active() && !spinner.paused())
    withEnabled(!visualUpdating) {
        redButton(
            margins = Margins.only(left = 3, right = 1),
            background = Chrome.Type.RED_BUTTON.descriptor()
        ) {
            rightToLeft {
                doASpin = false
                val img = image(Icons.CHANGES.descriptor())

                val flip = spinner.repeats % 2 == 0
                img.widget.angle = rotProgress * 180 + if (flip) 180 else 0

                img.widget.origin.set(img.widget.width / 2, img.widget.height / 2)

                verticalJustified {
                    val totalSize = trials.getGroups().size
                    val alreadyUpdated = totalSize - updatingCount
                    val text = shrinkToFitLabel(
                        if (visualUpdating) Messages.get(
                            WndTrials::class.java,
                            "update_in_progress",
                            alreadyUpdated,
                            totalSize
                        )
                        else Messages.get(WndTrials::class.java, "update"),
                        9,
                        img.response.rect.height()
                    )

                    if (!top().isEnabled()) {
                        text.widget.alpha(0.3f)
                    }
                }
            }
        }.onClick {
            doASpin = true
            Trials.checkForUpdates()
        }
    }
}

private val GRADIENT = intArrayOf(
    0xFF6A675Cu.toInt(),
    0xFF807C6Cu.toInt(),
    0xFF97917Du.toInt(),
    0xFFAEA68Eu.toInt(),
    0xFFC5BC9Fu.toInt(),
)

fun Ui.trialGroupButton(group: TrialGroup) {
    rightToLeft {
        if (group.updateError != null) {
            iconButton(Icons.WARNING.descriptor()).onClick {
                ShatteredPixelDungeon.scene().add(
                    WndError(group.updateError!!)
                )
            }
        } else {
            spacer(Vec2(14, 14))
        }
        verticalJustified {
            withEnabled(!group.isUpdating) {
                redButton(
                    margins = RED_BUTTON_MARGINS.copy(top = 4),
                    background = NinePatchDescriptor.Gradient(GRADIENT)
                ) {
                    val label = shrinkToFitLabel(group.name, 9)
                    if (group.wantNotify) {
                        label.widget.hardlight(
                            ColorMath.interpolate(
                                0xFFFFFF,
                                Window.SHPX_COLOR,
                                0.5f + sin((Game.timeTotal * 5).toDouble()).toFloat() / 2f
                            )
                        )
                    } else {
                        label.widget.resetColor()
                    }
                }.onClick {
                    group.wantNotify = false
                    ShatteredPixelDungeon.scene().add(
                        WndTrialsGroup(group)
                    )
                }
            }
        }
    }
}

class WndTrialsGroup(val group: TrialGroup) : TcpdWindow() {
    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        val name = group.name.ifBlank {
            var url = group.url
            if (url.startsWith("https://")) {
                url = url.substring(8)
            } else if (url.startsWith("http://")) {
                url = url.substring(7)
            }
            if (url.length > 16) {
                "${url.substring(0, 8)}[...]${url.substring(url.length - 8)}"
            } else {
                url
            }
        }
        verticalJustified {
            verticalJustified {
                shrinkToFitLabel(name, 12).widget.hardlight(
                    TITLE_COLOR
                )
            }
            top().addSpace(2)
            PaginatedList(group.trials.size, 19).show(this) { i ->
                trialButton(group.trials[i])
            }
        }
    }
}


fun Ui.trialButton(trial: Trial) {
    rightToLeft {
        val valid = trial.isValid()
        margins(Margins.only(top = 2)) {
            if (valid) {
                iconButton(Icons.INFO.descriptor()).onClick {
                    ShatteredPixelDungeon.scene().add(
                        WndModifiers(trial.getModifiers()!!, false)
                    )
                }
            } else {
                iconButton(Icons.WARNING.descriptor()).onClick {
                    ShatteredPixelDungeon.scene().add(
                        WndError(trial.localizedErrorMessage()!!)
                    )
                }
            }
        }

        verticalJustified {
            withEnabled(valid) {
                redButton(margins = Margins.ZERO) {
                    horizontal {
                        val redCheckboxWidth by useMemo(Unit) {
                            Icons.CHECKED.get().width
                        }
                        val res = margins(Margins(3, 0, 1 + redCheckboxWidth.roundToInt(), 0)) {
                            trial.lockedClass?.let { heroClass ->
                                image(TextureDescriptor.HeroClass(heroClass, 6))
                            }
                            shrinkToFitLabel(trial.name, 9, 15)
                        }
                        drawRedCheckbox(Trials.curTrial === trial, res.inner.response.rect)
                    }
                }.onClick {
                    Trials.curTrial = trial
                }
            }
        }
    }
}
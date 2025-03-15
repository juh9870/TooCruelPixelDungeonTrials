package com.shatteredpixel.shatteredpixeldungeon.tcpd.windows

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.Dungeon
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TrialGroup
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trials
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdWindow
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.AnimationState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.LoopingState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.NinePatchDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.InteractiveResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.PaginatedList
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.RED_BUTTON_MARGINS
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.activeLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.columns
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.customButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.dimInactiveText
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.dimInactiveVisual
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.highlightTouchedVisual
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.horizontal
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.iconButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.image
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.redButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.rightToLeft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.shrinkToFitLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.verticalJustified
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.easeInOutBack
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.easeOutBack
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.ui.Window
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptionsCondensed
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput
import com.watabou.gltextures.TextureCache
import com.watabou.glwrap.Texture
import com.watabou.noosa.Game
import com.watabou.noosa.Image
import com.watabou.utils.ColorMath
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.roundToInt
import kotlin.math.sin

class WndTrials : TcpdWindow() {
    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    private var editMode = false
    private var isOnTop = true

    override fun isUpdating(): Boolean {
        return isOnTop
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
                withEnabled(!editMode) {
                    redButton(
                        margins = Margins.ZERO
                    ) {
                        verticalJustified {
                            activeLabel(Messages.get(WndTrials::class.java, "custom"), 9)
                        }
                    }.onClick {
                        val prevTrial = Trials.curTrial
                        Trials.curTrial = Trial.CUSTOM
                        val modifiers = Trial.CUSTOM.getModifiers()!!
                        ShatteredPixelDungeon.scene()
                            .add(object : WndModifiers(modifiers, Trial.CUSTOM, true) {
                                override fun onBackPressed() {
                                    super.onBackPressed()
                                    Trial.CUSTOM.setModifiers(modifiers)
                                    if (!modifiers.isChallenged()) {
                                        if (prevTrial == Trial.CUSTOM) {
                                            Trials.curTrial = null
                                        } else {
                                            Trials.curTrial = prevTrial
                                        }
                                    }
                                }
                            })
                    }
                }
                redButton(
                    margins = Margins.ZERO
                ) {
                    verticalJustified {
                        activeLabel(
                            Messages.get(
                                WndTrials::class.java,
                                if (editMode) "edit_done" else "edit"
                            ), 9
                        )
                    }
                }.onClick {
                    editMode = !editMode
                }
                withEnabled(!editMode) {
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
                            Messages.get(WndTrials::class.java, "confirm"),
                            Messages.get(WndTrials::class.java, "cancel"),
                        ) {
                            override fun onSelect(positive: Boolean, text: String?) {
                                if (!positive || !validateUrl(text)) return

                                Trials.addGroup(text!!)

                                Trials.checkForUpdates() // TODO: individual updates?
                            }
                        })
                    }
                }
            }

            withEnabled(!editMode) {
                updateBtn()
            }

            val curTrial = Trials.curTrial

            if (curTrial != null) {
                withEnabled(!editMode) {
                    verticalJustified(background = Chrome.Type.GREY_BUTTON.descriptor()) {
                        activeLabel(Messages.get(WndTrials::class.java, "current"), 8)
                        trialButton(curTrial)
                    }
                }
            }

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


    override fun onBackPressed() {
        // disable edit mode on back instead of closing the window
        if (editMode) {
            editMode = false
        } else {
            super.onBackPressed()
        }
    }

    private fun Ui.updateBtn() {
        val trials = Trials.load()
        var doASpin by useState(Unit) { false }
        val updatingCount = trials.getGroups().count { it.isUpdating }
        val spinner by useMemo(Unit) { LoopingState() }
        val actualUpdating = doASpin || updatingCount > 0
        val rotProgress = spinner.animate(actualUpdating, 1f, 0.5f) { easeInOutBack(it) }
        val visualUpdating = actualUpdating || (spinner.active() && !spinner.paused())
        withEnabled(!visualUpdating) {
            redButton(
                margins = Margins.only(left = 3, right = 1),
                background = Chrome.Type.RED_BUTTON.descriptor()
            ) {
                rightToLeft {
                    doASpin = false
                    val img = image(Icons.CHANGES.descriptor())
                    dimInactiveVisual(img, !editMode)

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

                        dimInactiveText(text)
                    }
                }
            }.onClick {
                doASpin = true
                Trials.checkForUpdates()
            }
        }
    }

    private fun Ui.trialGroupButton(group: TrialGroup) {
        val groupName = group.nameOrTrimmedUrl()
        top().setStyle(top().style().copy(backgroundInteractionAnimationDuration = 0.1f))
        rightToLeft {
            margins(Margins.only(right = 1)) {
                withEnabled(editMode) {
                    val icoDelete = appearingIconButton(Icons.CLOSE.descriptor(), duration = 0.3f)
                    if (group.internalId != null) {
                        icoDelete.inner.alpha(0.5f)
                    }
                    icoDelete.onClick {
                        if (group.internalId != null) {
                            ShatteredPixelDungeon.scene()
                                .add(
                                    WndMessage(
                                        Messages.get(
                                            WndTrials::class.java,
                                            "remove_internal"
                                        )
                                    )
                                )
                            return@onClick
                        }
                        ShatteredPixelDungeon.scene().add(
                            object : WndOptionsCondensed(
                                Messages.get(WndTrials::class.java, "remove_group_title"),
                                Messages.get(
                                    WndTrials::class.java,
                                    "remove_group_body",
                                    groupName,
                                    group.url
                                ),
                                Messages.get(WndTrials::class.java, "confirm"),
                                Messages.get(WndTrials::class.java, "cancel"),
                            ) {
                                override fun onSelect(index: Int) {
                                    if (index == 0) {
                                        Trials.load().removeGroup(group)
                                    }
                                }
                            }
                        )

                    }
                    val icoEdit =
                        appearingIconButton(Icons.SCROLL_COLOR.descriptor(), duration = 0.3f)
                    if (group.internalId != null) {
                        icoEdit.inner.alpha(0.5f)
                    }
                    icoEdit.onClick {
                        if (group.internalId != null) {
                            ShatteredPixelDungeon.scene()
                                .add(
                                    WndMessage(
                                        Messages.get(
                                            WndTrials::class.java,
                                            "edit_internal"
                                        )
                                    )
                                )
                            return@onClick
                        }
                        ShatteredPixelDungeon.scene().add(object : WndTextInput(
                            Messages.get(WndTrials::class.java, "edit_title"),
                            Messages.get(WndTrials::class.java, "edit_body", groupName, group.url),
                            group.url,
                            1024,
                            false,
                            Messages.get(WndTrials::class.java, "confirm"),
                            Messages.get(WndTrials::class.java, "cancel"),
                        ) {
                            override fun onSelect(positive: Boolean, text: String?) {
                                if (!positive || !validateUrl(text)) return

                                group.url = text!!
                                Trials.save()

                                Trials.checkForUpdates() // TODO: individual updates?
                            }
                        })
                    }
                }
                withEnabled(group.updateError != null) {
                    appearingIconButton(Icons.WARNING.descriptor(), duration = 0.3f).onClick {
                        ShatteredPixelDungeon.scene().add(
                            WndError(group.updateError!!)
                        )
                    }
                }
            }
            verticalJustified {
                withEnabled(!group.isUpdating) {
                    redButton(
                        margins = RED_BUTTON_MARGINS.copy(top = 4),
                        background = NinePatchDescriptor.Gradient(GRADIENT)
                    ) {
                        val label = shrinkToFitLabel(groupName, 9)
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
                        group.notificationShown()
                        isOnTop = false
                        ShatteredPixelDungeon.scene().add(
                            object : WndTrialsGroup(group) {
                                override fun onBackPressed() {
                                    super.onBackPressed()
                                    isOnTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun validateUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false

    try {
        URL(url)
    } catch (e: MalformedURLException) {
        ShatteredPixelDungeon.scene().add(
            WndError(
                Messages.get(
                    WndTrials::class.java, "bad_url", e.message
                )
            )
        )
        return false
    }
    return true
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

private val GRADIENT = intArrayOf(
    0xFF6A675Cu.toInt(),
    0xFF807C6Cu.toInt(),
    0xFF97917Du.toInt(),
    0xFFAEA68Eu.toInt(),
    0xFFC5BC9Fu.toInt(),
)

fun Ui.appearingIconButton(
    image: TextureDescriptor,
    show: Boolean? = null,
    duration: Float = 0.2f,
    easingAppear: ((Float) -> Float)? = ::easeOutBack,
    easingDisappear: ((Float) -> Float)? = ::easeOutBack
) = appearingIconButton(image, show, duration, easingAppear, easingDisappear) { _, _ -> }

@Suppress("NAME_SHADOWING")
inline fun Ui.appearingIconButton(
    image: TextureDescriptor,
    show: Boolean? = null,
    duration: Float = 0.2f,
    noinline easingAppear: ((Float) -> Float)? = ::easeOutBack,
    noinline easingDisappear: ((Float) -> Float)? = ::easeOutBack,
    crossinline extraAnimation: (Image, Float) -> Unit
): InteractiveResponse<Image> {
    val show = show ?: top().isEnabled()
    return customButton { interaction ->
        val top = top()
        val showAnim = ctx().getOrPutMemory(top.id.with("showAnim")) {
            AnimationState(show).also {
                it.easingUp = easingAppear
                it.easingDown = easingDisappear
            }
        }
        val imageId = top.nextAutoId()

        val progress = showAnim.animate(show, duration) { it }
        val imageSize = image.size()
        val allocated = top.allocateSize(Vec2((imageSize.x * progress).roundToInt(), imageSize.y))

        val img = top.painter().drawImage(imageId, allocated.min, image)
        img.scale.set(progress)
        img.origin.set(img.width / 2f * progress, img.height / 2f)

        extraAnimation(img, progress)

        val res = WidgetResponse(img, UiResponse(allocated, imageId))
        highlightTouchedVisual(interaction, res, top.style().interactionAnimationDuration)
        dimInactiveVisual(res)
        img
    }
}

private open class WndTrialsGroup(val group: TrialGroup) : TcpdWindow() {
    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        verticalJustified {
            verticalJustified {
                shrinkToFitLabel(group.nameOrTrimmedUrl(), 12).widget.hardlight(
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

private fun Ui.trialButton(trial: Trial) {
    rightToLeft {
        val valid = trial.isValid()
        margins(Margins.only(top = 2)) {
            if (valid) {
                iconButton(Icons.INFO.descriptor()).onClick {
                    ShatteredPixelDungeon.scene().add(
                        WndModifiers(trial.getModifiers()!!, trial, false)
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

        var ping by useState(trial) { false }
        val pingController by useMemo(trial) { LoopingState() }
        val isSelected = Trials.curTrial === trial
        withEnabled(isSelected) {
            margins(Margins.only(top = 1)) {
                appearingIconButton(
                    Icons.ENTER.descriptor(),
                    Trials.curTrial === trial,
                    easingDisappear = null
                ) { button, progress ->
                    val x = pingController.animate(ping && progress >= 1f, 0.5f, 0f) { it }
                    button.angle = sin(20 * x) * (1 - x) * 30
                    ping = false
                }.onClick {
                    Dungeon.hero = null
                    Dungeon.daily = false
                    Dungeon.dailyReplay = false
                    Dungeon.tcpdData = null
                    Dungeon.initSeed()
                    ActionIndicator.clearAction()
                    InterlevelScene.mode = InterlevelScene.Mode.DESCEND
                    Game.switchScene(InterlevelScene::class.java)
                }
            }
        }

        verticalJustified {
            withEnabled(valid) {
                redButton(margins = Margins.ZERO) {
                    horizontal {
                        val redCheckboxWidth = Icons.CHECKED.descriptor().size().x
                        val res = margins(Margins(3, 0, 1 + redCheckboxWidth, 0)) {
                            trial.lockedClass?.let { heroClass ->
                                image(TextureDescriptor.HeroClass(heroClass, 6))
                            }
                            val res = shrinkToFitLabel(trial.name, 9, 15)
                            dimInactiveText(res)
                            res
                        }
//                        drawRedCheckbox(Trials.curTrial === trial, res.inner.response.rect)
                    }
                }.onClick {
                    if (isSelected) {
                        ping = true
                    } else {
                        Trials.curTrial = trial
                    }
                }
            }
        }
    }
}
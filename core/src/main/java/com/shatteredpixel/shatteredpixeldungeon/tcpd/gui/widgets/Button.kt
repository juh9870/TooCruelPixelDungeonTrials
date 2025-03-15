package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Pos2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.AnimationState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiId
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.ComponentConstructor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.NinePatchDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.Painter
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.ui.Button
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.watabou.noosa.Visual
import com.watabou.noosa.ui.Component

val RED_BUTTON_MARGINS = Margins(3, 3, 1, 3);

class UiButton {
    @PublishedApi
    internal fun createPainter(ui: Ui, id: UiId): Pair<Painter, Interaction> {
        val painter = ui.top().painter().withComponent(id, UiButtonComponent.Companion);
        val group = painter.getGroup()
        val interaction = if (group == null) {
            Interaction.NONE
        } else {
            val btn = (group as UiButtonComponent)
            btn.active = ui.top().isEnabled()
            if (!btn.isActive) {
                btn.resetInputs()
                Interaction.NONE
            } else {
                btn.resetInputs()
            }
        }
        return Pair(painter, interaction)
    }

    inline fun <T> show(
        ui: Ui,
        crossinline block: (interaction: Interaction) -> T
    ): InteractiveResponse<T> {
        val id = ui.top().nextAutoId().with("button")
        val (painter, interaction) = createPainter(ui, id)

        val response = ui.withLayout(painter = painter, id = id) {
            block(interaction)
        }
        return InteractiveResponse(interaction, response.response, response.inner)
    }
}

inline fun <T> Ui.customButton(crossinline block: (interaction: Interaction) -> T): InteractiveResponse<T> {
    return UiButton().show(this, block)
}

fun Ui.iconButton(image: TextureDescriptor): InteractiveResponse<Unit> {
    return customButton { interaction ->
        val img = image(image)

        highlightTouchedVisual(interaction, img, top().style().interactionAnimationDuration)
        dimInactiveVisual(img)
    }
}

fun <T : Visual> Ui.highlightTouchedVisual(
    interaction: Interaction,
    response: WidgetResponse<T>,
    duration: Float
) {
    highlightTouchedVisual(
        interaction.isPointerDown,
        response.widget,
        response.response.id,
        duration
    )
}

fun Ui.highlightTouchedVisual(held: Boolean, visual: Visual, id: UiId, duration: Float) {
    val anim =
        ctx().getOrPutMemory(id.with("highlight")) { AnimationState(held) }

    val brightness = anim.animate(held, duration) {
        1f + 0.2f * it
    }
    visual.brightness(brightness)
}

fun Ui.redButton(
    text: String,
    size: Int = 9,
    margins: Margins = RED_BUTTON_MARGINS,
    background: NinePatchDescriptor = Chrome.Type.RED_BUTTON.descriptor()
): InteractiveResponse<Unit> {
    return redButton(margins, background) {
        activeLabel(text, size)
    }
}

fun Ui.redCheckbox(
    checked: Boolean,
    text: String,
    size: Int = 9,
    margins: Margins = RED_BUTTON_MARGINS
): InteractiveResponse<Unit> {
    return redButton(margins) {
        val res = activeLabel(text, size)
        drawRedCheckbox(checked, res.response.rect)
    }
}

fun Ui.drawRedCheckbox(checked: Boolean, alignRect: Rect) {
    val ui = top();
    val space = ui.layout.getFullAvailableSpace()
    val checkboxId = ui.nextAutoId();
    val image = ui.painter().drawImage(
        checkboxId, Pos2(0, 0), if (checked) {
            Icons.CHECKED
        } else {
            Icons.UNCHECKED
        }.descriptor()
    )
    image.x = (space.right() - image.width - 1)
    image.y =
        ((alignRect.top() + (alignRect.height() - image.height) / 2) + 1)
    PixelScene.align(image)
    dimInactiveVisual(WidgetResponse(image, UiResponse(Rect.ZERO, checkboxId)))
}

inline fun <T> Ui.redButton(
    margins: Margins = RED_BUTTON_MARGINS,
    background: NinePatchDescriptor = Chrome.Type.RED_BUTTON.descriptor(),
    crossinline content: (interaction: Interaction) -> T
): InteractiveResponse<T> {
    return customButton { interaction ->
        withRedButtonBackground(this, interaction.isPointerDown, margins, background) {
            content(interaction)
        }
    }
}

inline fun <T> withRedButtonBackground(
    ui: Ui,
    held: Boolean,
    margins: Margins,
    background: NinePatchDescriptor = Chrome.Type.RED_BUTTON.descriptor(),
    crossinline content: () -> T
): T {
    return ui.vertical(background = background) {
        ui.margins(margins) {
            val bg = (ui.top().painter().getGroup() as NinePatchComponent).ninePatch
            if (bg != null) {
                ui.highlightTouchedVisual(
                    held,
                    bg,
                    ui.top().id(),
                    ui.top().style().backgroundInteractionAnimationDuration
                )
            }
            content()
        }.inner
    }.inner
}

data class Interaction(
    val justClicked: Boolean,
    val justRightClicked: Boolean,
    val justMiddleClicked: Boolean,
    val justPointerDown: Boolean,
    val justPointerUp: Boolean,
    val isPointerDown: Boolean,
) {
    companion object {
        val NONE = Interaction(
            justClicked = false,
            justRightClicked = false,
            justMiddleClicked = false,
            justPointerDown = false,
            justPointerUp = false,
            isPointerDown = false,
        )
    }
}

data class InteractiveResponse<T>(
    val interaction: Interaction, val response: UiResponse, val inner: T
) {
    fun clicked(): Boolean {
        return interaction.justClicked
    }

    inline fun <T> onClick(block: () -> T): T? {
        return if (clicked()) {
            block()
        } else {
            null
        }
    }

    fun isPointerDown(): Boolean {
        return interaction.isPointerDown
    }
}

private class UiButtonComponent : Button() {
    companion object : ComponentConstructor {
        override fun construct(): Component {
            return UiButtonComponent()
        }

        override fun componentClass(): Class<out Component> {
            return UiButtonComponent::class.java
        }
    }

    private var interaction = Interaction.NONE

    fun resetInputs(): Interaction {
        val i = interaction
        interaction = Interaction.NONE.copy(isPointerDown = i.isPointerDown)
        return i
    }

    override fun onClick() {
        super.onClick()
        interaction = interaction.copy(justClicked = true)
    }

    override fun onMiddleClick() {
        super.onMiddleClick()
        interaction = interaction.copy(justMiddleClicked = true)
    }

    override fun onRightClick() {
        super.onRightClick()
        interaction = interaction.copy(justRightClicked = true)
    }

    override fun onPointerDown() {
        super.onPointerDown()
        interaction = interaction.copy(justPointerDown = true, isPointerDown = true)
    }

    override fun onPointerUp() {
        super.onPointerUp()
        interaction = interaction.copy(justPointerUp = true, isPointerDown = false)
    }

    override fun clear() {
        super.clear()
        add(hotArea)
    }
}
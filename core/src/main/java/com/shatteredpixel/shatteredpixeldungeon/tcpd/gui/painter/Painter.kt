package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite
import com.shatteredpixel.shatteredpixeldungeon.tcpd.ext.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Pos2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiId
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.LRUCache
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock
import com.watabou.gltextures.SmartTexture
import com.watabou.gltextures.TextureCache
import com.watabou.noosa.ColorBlock
import com.watabou.noosa.Gizmo
import com.watabou.noosa.Group
import com.watabou.noosa.Image
import com.watabou.noosa.NinePatch
import com.watabou.noosa.Visual
import com.watabou.noosa.ui.Component
import kotlin.math.roundToInt

class Painter internal constructor(
    private val group: Group?,
    private val cache: PaintCache,
) {
    companion object {
        fun create(group: Group?, cache: PaintCache): Painter {
            cache.restart()
            return Painter(group, cache)
        }
    }

    /**
     * Returns a new painter with the new clip rect, optionally using a
     * component as a group.
     *
     * The new clip rect is the intersection of the current clip rect and the given clip rect.
     *
     * @param clip The new clip rect.
     */
    fun clipped(id: UiId, clip: Rect?, component: ComponentConstructor? = null): Painter {
        // We are already inside non-visible area.
        if (this.group == null) {
            return this
        }

        val group = add(id, VisualElement.Group(component)) as Group
        if (component == null) {
            group.clear()
        } else {
            component.clearChildren(group as Component)
        }
        val cam = this.group.camera().withClip(clip) ?: return Painter(null, cache)
        group.camera = cam

        return Painter(group, cache)
    }

    /**
     * Same as [clipped], but without clipping.
     */
    fun withComponent(id: UiId, component: ComponentConstructor): Painter {
        if (this.group == null) {
            return this
        }
        val group = add(id, VisualElement.Group(component)) as Component

        component.clearChildren(group)

        return Painter(group, cache)
    }

    fun drawRect(id: UiId, rect: Rect, color: Int): ColorBlock {
        return add(id, VisualElement.ColoredRect(rect, color)) as ColorBlock
    }

    fun drawNinePatch(id: UiId, rect: Rect, descriptor: NinePatchDescriptor): NinePatch {
        return add(id, VisualElement.NinePatch(rect, descriptor)) as NinePatch
    }

    fun drawSizedImage(id: UiId, rect: Rect, texture: TextureDescriptor): Image {
        return add(id, VisualElement.SizedImage(rect, texture)) as Image
    }

    fun drawImage(id: UiId, pos: Pos2, texture: TextureDescriptor): Image {
        return add(id, VisualElement.NativeImage(pos, texture)) as Image
    }

    fun drawText(
        id: UiId,
        rect: Rect,
        text: String,
        size: Int,
        multiline: Boolean
    ): RenderedTextBlock {
        return add(id, VisualElement.Text(rect, text, size, multiline)) as RenderedTextBlock
    }

    fun drawComponent(id: UiId, rect: Rect, component: ComponentConstructor): Component {
        return add(id, VisualElement.Component(rect, component)) as Component
    }

    fun getGroup(): Group? {
        return group
    }

    private fun add(id: UiId, element: VisualElement): Gizmo {
        val gizmo = cache.get(id, element);

        group?.addToFront(gizmo)
        if (gizmo is Component) {
            PixelScene.align(gizmo)
        } else if (gizmo is Visual) {
            PixelScene.align(gizmo)
        }
        return gizmo
    }
}

internal sealed class VisualElement {
    data class ColoredRect(val rect: Rect, val color: Int) : VisualElement()
    data class NinePatch(val rect: Rect, val descriptor: NinePatchDescriptor) : VisualElement()

    data class SizedImage(val rect: Rect, val texture: TextureDescriptor) : VisualElement()
    data class NativeImage(val pos: Pos2, val texture: TextureDescriptor) : VisualElement()
    data class Component(val rect: Rect, val component: ComponentConstructor) : VisualElement()
    data class Text(val rect: Rect, val text: String, val size: Int, val multiline: Boolean) :
        VisualElement()

    data class Group(val component: ComponentConstructor?) : VisualElement()

    fun asGizmo(cached: Pair<VisualElement, Gizmo>?): Gizmo {
        when (this) {
            is ColoredRect -> {
                val block = if (cached?.first is ColoredRect && cached.second is ColorBlock) {
                    val old = cached.first as ColoredRect
                    if (old.color == color) {
                        return cached.second as ColorBlock
                    } else {
                        null
                    }
                } else {
                    null
                } ?: ColorBlock(rect.width().toFloat(), rect.height().toFloat(), color)

                block.x = rect.min.x.toFloat()
                block.y = rect.min.y.toFloat()
                block.size(rect.width().toFloat(), rect.height().toFloat())
                return block
            }

            is SizedImage -> {
                val image = if (cached?.second is Image) {
                    val image = cached.second as Image

                    texture.update(image)
                    image.resetColor()
                    image
                } else {
                    texture.asImage()
                }
                image.width = rect.width().toFloat()
                image.height = rect.height().toFloat()
                image.x = rect.min.x.toFloat()
                image.y = rect.min.y.toFloat()

                return image
            }

            is NativeImage -> {
                val image = if (cached?.second is Image) {
                    val image = cached.second as Image

                    texture.update(image)
                    image.resetColor()
                    image
                } else {
                    texture.asImage()
                }
                image.x = pos.x.toFloat()
                image.y = pos.y.toFloat()
                image.scale.set(1f)
                return image
            }

            is NinePatch -> {
                val block =
                    if (cached?.first is NinePatch && cached.second is com.watabou.noosa.NinePatch) {
                        val old = cached.first as NinePatch
                        if (old.descriptor == descriptor) {
                            val block = cached.second as com.watabou.noosa.NinePatch
                            block
                        } else {
                            null
                        }
                    } else {
                        null
                    } ?: descriptor.get()

                block.x = rect.min.x.toFloat()
                block.y = rect.min.y.toFloat()
                block.size(rect.width().toFloat(), rect.height().toFloat())
                block.resetColor()
                return block
            }

            is Component -> {
                val comp =
                    if (cached?.first == this && cached.second.javaClass == this.component.componentClass()) {
                        cached.second as com.watabou.noosa.ui.Component
                    } else {
                        this.component.construct()
                    }

                comp.setRect(
                    rect.min.x.toFloat(),
                    rect.min.y.toFloat(),
                    rect.width().toFloat(),
                    rect.height().toFloat()
                )
                return comp
            }

            is Group -> {
                if (this.component == null) {
                    if (cached?.second is com.watabou.noosa.Group) {
                        return cached.second as com.watabou.noosa.Group
                    }

                    return Group()
                } else {
                    val comp =
                        if (cached?.first == this && cached.second.javaClass == this.component.componentClass()) {
                            cached.second as com.watabou.noosa.ui.Component
                        } else {
                            this.component.construct()
                        }

                    return comp
                }
            }

            is Text -> {
                if (cached?.first is Text && cached.second is RenderedTextBlock && (cached.first as Text).size == size) {
                    var block = cached.second as RenderedTextBlock
                    val old = cached.first as Text

                    block.text(text)
                    if (multiline) {
                        block.maxWidth(rect.width())
                    } else {
                        if (old.multiline) {
                            block = PixelScene.renderTextBlock(text, size)
                        }
                    }
                    block.setPos(rect.min.x.toFloat(), rect.min.y.toFloat())
                    return block
                }
                val block = PixelScene.renderTextBlock(text, size)

                if (multiline) {
                    block.maxWidth(rect.width())
                }
                block.setPos(rect.min.x.toFloat(), rect.min.y.toFloat())
                return block
            }
        }
    }
}

private val TextureSizeCache = LRUCache<TextureDescriptor, Vec2>(256)

sealed interface TextureDescriptor {
    class ByName(val name: String) : TextureDescriptor
    class SmartTexture(val texture: com.watabou.gltextures.SmartTexture) : TextureDescriptor
    class Pixmap(val pixmap: com.badlogic.gdx.graphics.Pixmap) : TextureDescriptor
    class Icon(val icon: Icons) : TextureDescriptor
    class HeroClass(
        val heroClass: com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass,
        val armorTier: Int
    ) : TextureDescriptor

    fun asImage(): Image {
        return when (this) {
            is ByName -> Image(TextureCache.get(name))
            is SmartTexture -> Image(TextureCache.get(texture))
            is Pixmap -> Image(TextureCache.get(pixmap))
            is Icon -> Icons.get(icon)
            is HeroClass -> HeroSprite.avatar(heroClass, armorTier)
        }
    }

    fun update(image: Image) {
        when (this) {
            is ByName -> image.texture(TextureCache.get(name))
            is Pixmap -> image.texture(TextureCache.get(pixmap))
            is SmartTexture -> image.texture(TextureCache.get(texture))
            is Icon -> image.copy(Icons.get(icon))
            is HeroClass -> image.copy(HeroSprite.avatar(heroClass, armorTier))
        }
    }

    fun size(): Vec2 {
        return TextureSizeCache.getOrPut(this) {
            val image = asImage()
            Vec2(image.width.roundToInt(), image.height.roundToInt())
        }
    }
}

sealed interface NinePatchDescriptor {
    data class Chrome(val type: com.shatteredpixel.shatteredpixeldungeon.Chrome.Type) :
        NinePatchDescriptor

    data class FlatColor(val color: UInt) : NinePatchDescriptor
    data class TextureId(val key: Any, val margins: Margins) : NinePatchDescriptor
    data class Gradient(val colors: IntArray) : NinePatchDescriptor {
        companion object {
            fun colors(vararg colors: Int): Gradient {
                return Gradient(colors)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Gradient) return false

            return colors.contentEquals(other.colors)
        }

        override fun hashCode(): Int {
            return colors.contentHashCode()
        }
    }

    fun get(): NinePatch {
        return when (this) {
            is Chrome -> com.shatteredpixel.shatteredpixeldungeon.Chrome.get(type)
            is FlatColor -> NinePatch(TextureCache.createSolid(color.toInt()), 0)
            is Gradient -> NinePatch(TextureCache.createGradient(*colors), 0)
            is TextureId -> NinePatch(
                TextureCache.get(key),
                margins.left,
                margins.top,
                margins.right,
                margins.bottom
            )
        }
    }

    fun margins(): Margins {
        return when (this) {
            is Chrome -> type.margins()
            is FlatColor -> Margins.same(0)
            is Gradient -> Margins.same(0)
            is TextureId -> margins
        }
    }
}

interface ComponentConstructor {
    fun construct(): Component
    fun componentClass(): Class<out Component>
    fun clearChildren(comp: Component) {
        comp.clear()
    }
}

fun Chrome.Type.descriptor(): NinePatchDescriptor {
    return NinePatchDescriptor.Chrome(this)
}

fun Icons.descriptor(): TextureDescriptor {
    return TextureDescriptor.Icon(this)
}

fun TextureCache.get(obj: TextureDescriptor): SmartTexture {
    throw Error("Do not use TextureCache.get directly with TextureDescriptor. Use `asKey` first.")
}
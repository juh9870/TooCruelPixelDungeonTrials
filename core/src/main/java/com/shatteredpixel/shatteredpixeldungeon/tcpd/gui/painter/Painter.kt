package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
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
    fun clipped(clip: Rect?, component: ComponentConstructor? = null): Painter {
        // We are already inside non-visible area.
        if (this.group == null) {
            return this
        }

        val group = add(VisualElement.Group(component)) as Group
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
    fun withComponent(component: ComponentConstructor): Painter {
        if (this.group == null) {
            return this
        }
        val group = add(VisualElement.Group(component)) as Component

        component.clearChildren(group)

        return Painter(group, cache)
    }

    fun drawRect(rect: Rect, color: Int) {
        add(VisualElement.ColoredRect(rect, color))
    }

    fun drawNinePatch(rect: Rect, descriptor: NinePatchDescriptor): Gizmo {
        return add(VisualElement.NinePatch(rect, descriptor))
    }

    fun drawImage(rect: Rect, texture: TextureDescriptor): Gizmo {
        return add(VisualElement.Image(rect, texture))
    }

    fun drawText(rect: Rect, text: String, size: Int, multiline: Boolean): Gizmo {
        return add(VisualElement.Text(rect, text, size, multiline))
    }

    fun drawComponent(rect: Rect, component: ComponentConstructor): Gizmo {
        return add(VisualElement.Component(rect, component))
    }

    fun getGroup(): Group? {
        return group
    }

    private fun add(element: VisualElement): Gizmo {
        val gizmo = cache.advance(element);

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
    data class NinePatch(val rect: Rect, val descriptor: NinePatchDescriptor) :
        VisualElement()

    data class Image(val rect: Rect, val texture: TextureDescriptor) : VisualElement()
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

                block.origin.set(rect.min.x.toFloat(), rect.min.y.toFloat())
                block.size(rect.width().toFloat(), rect.height().toFloat())
                return block
            }

            is Image -> {
                val image = if (cached?.second is com.watabou.noosa.Image) {
                    val image = cached.second as com.watabou.noosa.Image

                    image.texture(texture.asKey())
                    image
                } else {
                    Image(texture.asKey())
                }
                image.width = rect.width().toFloat()
                image.height = rect.height().toFloat()
                image.origin.set(rect.min.x.toFloat(), rect.min.y.toFloat())
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
                if(this.component == null) {
                    if (cached?.second is com.watabou.noosa.Group) {
                        return cached.second as com.watabou.noosa.Group
                    }

                    return Group()
                } else {
                    val comp = if (cached?.first == this && cached.second.javaClass == this.component.componentClass()) {
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

sealed interface TextureDescriptor {
    class ByName(val name: String) : TextureDescriptor
    class SmartTexture(val texture: com.watabou.gltextures.SmartTexture) : TextureDescriptor
    class Pixmap(val pixmap: com.badlogic.gdx.graphics.Pixmap) : TextureDescriptor

    fun asKey(): Any {
        return when (this) {
            is ByName -> name
            is SmartTexture -> texture
            is Pixmap -> pixmap
        }
    }
}

sealed interface NinePatchDescriptor {
    data class Chrome(val type: com.shatteredpixel.shatteredpixeldungeon.Chrome.Type) :
        NinePatchDescriptor

    fun get(): NinePatch {
        return when (this) {
            is Chrome -> com.shatteredpixel.shatteredpixeldungeon.Chrome.get(type)
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

fun TextureCache.get(obj: TextureDescriptor): SmartTexture {
    throw Error("Do not use TextureCache.get directly with TextureDescriptor. Use `asKey` first.")
}
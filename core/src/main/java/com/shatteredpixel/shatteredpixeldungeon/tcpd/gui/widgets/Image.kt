package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.UiResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.WidgetResponse
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.watabou.noosa.Image

class UiImage {
    fun show(ui: Ui, descriptor: TextureDescriptor, allocatedSize: Vec2?): WidgetResponse<Image> {
        val top = ui.top()
        val id = top.nextAutoId()
        val space = top.layout.nextAvailableSpace(top.style())
        val img = top.painter().drawImage(id, space.min, descriptor)
        val imageSize = allocatedSize ?: Vec2(img.width.toInt(), img.height.toInt())
        val allocated = top.allocateSize(imageSize)

        val res = if (allocated.size() != imageSize) {
            val centered = allocated.centerInside(imageSize)
            img.x = centered.min.x.toFloat()
            img.y = centered.min.y.toFloat()
            PixelScene.align(img)

            UiResponse(centered, id)
        } else {
            img.x = allocated.min.x.toFloat()
            img.y = allocated.min.y.toFloat()
            UiResponse(allocated, id)
        }

        return WidgetResponse(img, res)
    }
}

fun Ui.image(descriptor: TextureDescriptor, allocatedSize: Vec2? = null): WidgetResponse<Image> {
    return UiImage().show(this, descriptor, allocatedSize)
}
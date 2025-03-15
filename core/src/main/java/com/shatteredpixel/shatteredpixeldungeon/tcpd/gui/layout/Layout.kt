@file:Suppress("MemberVisibilityCanBePrivate")

package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Pos2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Style
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

abstract class Layout(protected var availableSpace: Rect) {
    open class Horizontal(availableSpace: Rect) : Layout(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return Horizontal(availableSpace)
            }
        }

        protected var cursor: Pos2 = availableSpace.min

        override fun allocate(desired: Vec2, style: Style): Rect {
            val rect = Rect.fromSize(cursor, desired).allocateIntersect(availableSpace)
            cursor = Pos2(rect.right() + style.itemSpacing, cursor.y)
            return rect
        }

        override fun addSpace(amount: Int) {
            cursor = Pos2(cursor.x + amount, cursor.y)
        }

        override fun nextAvailableSpace(style: Style): Rect {
            return Rect.fromMinMax(cursor, availableSpace.max)
        }

        override fun childContinued(): LayoutConstructor {
            return Horizontal
        }
    }

    open class Vertical(availableSpace: Rect) : Layout(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return Vertical(availableSpace)
            }
        }

        protected var cursor: Pos2 = availableSpace.min

        override fun allocate(desired: Vec2, style: Style): Rect {
            val rect = Rect.fromSize(cursor, desired).allocateIntersect(availableSpace)
            cursor = Pos2(cursor.x, rect.bottom() + style.itemSpacing)
            return rect
        }

        override fun addSpace(amount: Int) {
            cursor = Pos2(cursor.x, cursor.y + amount)
        }

        override fun nextAvailableSpace(style: Style): Rect {
            return Rect.fromMinMax(cursor, availableSpace.max)
        }

        override fun childContinued(): LayoutConstructor {
            return Vertical
        }
    }

    open class Stack(availableSpace: Rect) : Layout(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return Stack(availableSpace)
            }
        }

        override fun allocate(desired: Vec2, style: Style): Rect {
            return Rect.fromSize(availableSpace.min, desired).allocateIntersect(availableSpace)
        }

        override fun addSpace(amount: Int) {
            // no-op
        }

        override fun nextAvailableSpace(style: Style): Rect {
            return availableSpace
        }

        override fun childContinued(): LayoutConstructor {
            return Stack
        }
    }

    open class StackJustified(availableSpace: Rect) : Stack(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return StackJustified(availableSpace)
            }
        }

        override fun allocate(desired: Vec2, style: Style): Rect {
            return super.allocate(Vec2(availableSpace.width(), desired.y), style)
        }

        override fun childContinued(): LayoutConstructor {
            return StackJustified
        }
    }

    open class StackFill(availableSpace: Rect) : Stack(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return StackFill(availableSpace)
            }
        }

        override fun allocate(desired: Vec2, style: Style): Rect {
            return super.allocate(availableSpace.size(), style)
        }

        override fun childContinued(): LayoutConstructor {
            return StackFill
        }
    }

    class VerticalJustified(availableSpace: Rect) : Vertical(availableSpace) {
        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return VerticalJustified(availableSpace)
            }
        }

        override fun allocate(desired: Vec2, style: Style): Rect {
            return super.allocate(Vec2(availableSpace.width(), desired.y), style)
        }

        override fun childContinued(): LayoutConstructor {
            return VerticalJustified
        }
    }

    class ColumnsLayout(
        private val columns: FloatArray,
        private val spacing: Int,
        availableSpace: Rect
    ) :
        Layout(availableSpace) {
        class ColumnsLayoutConstructor(private val columns: FloatArray, private val spacing: Int) :
            LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return ColumnsLayout(columns, spacing, availableSpace)
            }
        }

        companion object {
            fun constructor(columns: FloatArray, spacing: Int): LayoutConstructor {
                return ColumnsLayoutConstructor(columns, spacing)
            }
        }

        private var column = 0
        private var cursor = availableSpace.min
        private var nextRow = 0
        private val columnsSum = columns.sum()

        private val columnWidths = IntArray(columns.size)
        private val unusedSpace: Int

        init {
            if (columns.isEmpty()) {
                throw IllegalArgumentException("Columns layout requires at least one column")
            }
            val totalAvailable = availableSpace.width() - spacing * (columns.size - 1)
            for (i in columns.indices) {
                columnWidths[i] = (columns[i] * totalAvailable / columnsSum).toInt()
            }

            unusedSpace = totalAvailable - columnWidths.sum()
            if (unusedSpace > 0) {
                val distributeToEach = unusedSpace / columns.size
                if (distributeToEach > 0) {
                    for (i in columnWidths.indices) {
                        columnWidths[i] += distributeToEach
                    }
                }
                val remaining = unusedSpace - distributeToEach * columns.size
                if (remaining > 0) {
                    columnWidths[0] += floor(remaining / 2f).toInt()
                    columnWidths[columnWidths.size - 1] += ceil(remaining / 2f).toInt()
                }
            }
        }

        private fun nextWidth(): Int {
            return columnWidths[column]
        }

        override fun allocate(desired: Vec2, style: Style): Rect {
            val width = nextWidth()
            val rect =
                Rect.fromSize(cursor, Vec2(width, desired.y)).allocateIntersect(availableSpace)
            nextRow = max(nextRow, rect.bottom())

            column += 1
            if (column >= columns.size) {
                column = 0
                cursor = Pos2(availableSpace.min.x, nextRow + spacing)
            } else {
                cursor = Pos2(cursor.x + width + spacing, cursor.y)
            }
            return rect
        }

        override fun addSpace(amount: Int) {
            // no-op
        }

        override fun nextAvailableSpace(style: Style): Rect {
            return Rect.fromMinMax(
                cursor,
                Pos2(cursor.x + nextWidth(), availableSpace.max.y)
            )
        }

        override fun childContinued(): LayoutConstructor {
            return VerticalJustified
        }
    }

    open class RightToLeft(availableSpace: Rect) : Layout(availableSpace) {
        init {
            if (availableSpace.right() >= 1e6) {
                throw IllegalArgumentException("RightToLeft layout requires a bounded space")
            }
        }

        companion object : LayoutConstructor {
            override fun construct(availableSpace: Rect): Layout {
                return RightToLeft(availableSpace)
            }
        }

        protected var cursorRight: Pos2 = availableSpace.topRight()

        override fun allocate(desired: Vec2, style: Style): Rect {
            val rect = Rect.fromSize(Pos2(cursorRight.x - desired.x, cursorRight.y), desired)
                .allocateIntersect(availableSpace)
            cursorRight = Pos2(rect.left() - style.itemSpacing, cursorRight.y)
            return rect
        }

        override fun addSpace(amount: Int) {
            cursorRight = Pos2(cursorRight.x - amount, cursorRight.y)
        }

        override fun nextAvailableSpace(style: Style): Rect {
            return Rect.fromMinMax(
                Pos2(availableSpace.left(), cursorRight.y),
                Pos2(cursorRight.x, availableSpace.bottom())
            )
        }

        override fun childContinued(): LayoutConstructor {
            return RightToLeft
        }
    }

    /**
     * Allocates space for the next widget.
     */
    abstract fun allocate(desired: Vec2, style: Style): Rect

    /**
     * Adds the space before the next widget.
     */
    abstract fun addSpace(amount: Int)

    /**
     * Returns the available space for the next widget.
     */
    abstract fun nextAvailableSpace(style: Style): Rect

    /**
     * Returns the child layout with the same direction as this layout, whenever it
     * makes sense.
     */
    abstract fun childContinued(): LayoutConstructor

    fun getFullAvailableSpace(): Rect {
        return availableSpace
    }
}

fun Rect.allocateIntersect(availableSpace: Rect): Rect {
    return this.intersection(availableSpace)
        ?: throw IllegalArgumentException("Attempted to allocate a widget outside of the available space: $this does not intersect $availableSpace")
}

interface LayoutConstructor {
    fun construct(availableSpace: Rect): Layout
}

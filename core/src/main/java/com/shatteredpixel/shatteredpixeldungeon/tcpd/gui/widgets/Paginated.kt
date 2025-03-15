package com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets

import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.NinePatchDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.assertEq
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

const val PAGINATED_HEADER_HEIGHT = 11

class PaginatedList(
    val count: Int, val itemHeight: Int,
    val bodyBackground: NinePatchDescriptor? = null,
    val bodyMargins: Margins = Margins.ZERO
) {
    inline fun show(
        ui: Ui,
        crossinline showItem: (Int) -> Unit
    ) {
        ui.verticalJustified {
            val spacing = ui.top().style().itemSpacing
            val available = ui.top().nextAvailableSpace()

            val marginVertical = (bodyBackground?.margins()?.size()?.y ?: 0) + bodyMargins.size().y

            val requiredTotalHeight = count * itemHeight + (count - 1) * spacing + marginVertical
            if (available.height() >= requiredTotalHeight) {
                ui.verticalJustified(background = bodyBackground) {
                    ui.margins(bodyMargins) {
                        for (i in 0 until count) {
                            val res = ui.verticalJustified { showItem(i) }
                            assertEq(res.response.rect.height(), itemHeight)
                        }
                    }
                }
                return@verticalJustified
            }

            val pageAvailableHeight =
                available.height() - marginVertical - PAGINATED_HEADER_HEIGHT - spacing
            val itemsPerPage = (pageAvailableHeight + spacing) / (itemHeight + spacing)
            val pagesCount = ceil(count.toFloat() / itemsPerPage).toInt()

            var currentPage by ui.useState(Unit) { 0 }
            currentPage = max(0, min(currentPage, pagesCount - 1))

            if (pagesCount < 0) {
                val res = ui.columns(FloatArray(pagesCount) { 1f }) {
                    for (i in 0 until pagesCount) {
                        ui.pageSwitchBtn(i, currentPage).onClick {
                            currentPage = i
                        }
                    }
                }
                assertEq(res.response.rect.height(), PAGINATED_HEADER_HEIGHT)
            } else {
                val res = ui.columns(floatArrayOf(1f, 1f, 2f, 1f, 1f)) {
                    ui.pageSwitchBtn(0, currentPage).onClick {
                        currentPage = 0
                    }
                    ui.withEnabled(currentPage > 0) {
                        ui.redButton("<", margins = Margins.ZERO).onClick {
                            currentPage--
                        }
                    }
                    ui.margins(Margins.symmetric(0, 2)) {
                        ui.label("${currentPage + 1} / $pagesCount", 9).widget.hardlight(0xFFFF44)
                    }
                    ui.withEnabled(currentPage < pagesCount - 1) {
                        ui.redButton(">", margins = Margins.ZERO).onClick {
                            currentPage++
                        }
                    }
                    ui.pageSwitchBtn(pagesCount - 1, currentPage).onClick {
                        currentPage = pagesCount - 1
                    }
                }
                assertEq(res.response.rect.height(), PAGINATED_HEADER_HEIGHT)
            }

            val lastItem = min(
                (currentPage + 1) * itemsPerPage,
                count
            )

            ui.verticalJustified(background = bodyBackground) {
                ui.margins(bodyMargins) {
                    for (i in currentPage * itemsPerPage until lastItem) {
                        val res = ui.verticalJustified { showItem(i) }
                        assertEq(res.response.rect.height(), itemHeight)
                    }

                    if (currentPage == pagesCount - 1) {
                        for (i in lastItem until pagesCount * itemsPerPage) {
                            ui.spacer(Vec2(0, itemHeight))
                        }
                    }
                }
            }
        }
    }
}


@PublishedApi
internal fun Ui.pageSwitchBtn(page: Int, currentPage: Int): InteractiveResponse<Unit> {
    return withEnabled(page != currentPage) {
        customButton {
            withRedButtonBackground(
                this,
                page == currentPage || it.isPointerDown,
                margins = Margins.ZERO
            ) {
                val label = label("${page + 1}", 9)
                if (page == currentPage) {
                    label.widget.hardlight(0xFFFF44)
                } else {
                    label.widget.resetColor()
                }
            }
        }
    }.inner
}
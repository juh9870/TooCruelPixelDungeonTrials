package com.shatteredpixel.shatteredpixeldungeon.tcpd.windows

import com.shatteredpixel.shatteredpixeldungeon.Chrome
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifier
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Modifiers
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDGameInfoData
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDIcons
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDScores
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Tag
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Rect
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdComponent
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdWindow
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.AnimationState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.MutableHookRef
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useAnimation
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useState
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.PaginatedList
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.appearingIcon
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.columns
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.customButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.dimInactiveText
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.drawRedCheckbox
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.horizontal
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.iconButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.image
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.label
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.margins
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.measureTextWidth
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.redButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.rightToLeft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.shrinkToFitLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.stack
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.vertical
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.verticalJustified
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.withRedButtonBackground
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.easeInBack
import com.shatteredpixel.shatteredpixeldungeon.tcpd.utils.easeOutBack
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.ui.Window.TITLE_COLOR
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

open class WndModifiers(
    private val modifiers: Modifiers,
    private val trial: Trial?,
    private val editable: Boolean,
) : TcpdWindow() {
    constructor(data: TCPDData) : this(data.modifiers, data.trial, false)
    constructor(data: TCPDGameInfoData) : this(data.modifiers, data.trials, false)
    constructor(trial: Trial) : this(Modifiers(trial.modifiers), trial, false)

    init {
        maxSize = Vec2(125, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        drawModifiers(modifiers, trial, editable)
    }
}

open class ModifiersComponent(
    private val modifiers: Modifiers,
    private val trial: Trial?,
    private val editable: Boolean,
) : TcpdComponent() {
    constructor(data: TCPDData) : this(data.modifiers, data.trial, false)
    constructor(data: TCPDGameInfoData) : this(data.modifiers, data.trials, false)

    override fun Ui.drawUi() {
        drawModifiers(modifiers, trial, editable)
    }
}

private fun Ui.drawModifiers(
    modifiers: Modifiers,
    trial: Trial?,
    editable: Boolean,
) {
    verticalJustified {
        rightToLeft {
            editStringButton(modifiers, editable)
            verticalJustified {
                label(Messages.get(WndModifiers::class.java, "title"), 12).widget.hardlight(
                    TITLE_COLOR,
                )
            }
        }

        if (trial != null) {
            shrinkToFitLabel(Messages.get(WndModifiers::class.java, "trial", trial.name), 9)
        }

        top().addSpace(2)

        val sort =
            useState(Unit) {
                FilterOptions(SortType.Default, false)
            }

        if (editable) {
            filterBar(sort)
        }

        var resetPage = false
        val modifiersList by useMemo(sort.get()) {
            resetPage = true
            val allEnabled = mutableListOf<Modifier>()
            Modifier.entries.filterTo(allEnabled) { modifiers.isEnabled(it) }.toMutableList()
            sort.get().apply(allEnabled, filter = false)

            if (!editable) return@useMemo allEnabled

            val allDisabled = mutableListOf<Modifier>()
            Modifier.entries.filterTo(allDisabled) { !modifiers.isEnabled(it) }
            sort.get().apply(allDisabled)
            allEnabled + allDisabled
        }
        val paginated = PaginatedList(modifiersList.size, 17)
        @Suppress("KotlinConstantConditions") // false positive
        if (resetPage) {
            paginated.setPage(0)
        }
        paginated.show(this) { i ->
            modifierBtn(
                modifiers,
                modifiersList[i],
                editable,
                sort.get().sortType == SortType.Completed,
            )
        }
    }
}

private fun Ui.modifierBtn(
    modifiers: Modifiers,
    modifier: Modifier,
    editable: Boolean,
    showCompletion: Boolean,
) {
    rightToLeft {
        margins(Margins.only(top = 2)) {
            iconButton(Icons.INFO.descriptor()).onClick {
                var desc = modifier.localizedDesc()
                if (modifier.tags.isNotEmpty()) {
                    val tagsDesc =
                        modifier.tags.joinToString(
                            separator = ", ",
                            transform = { it.localizedName() },
                        )
                    desc = Messages.get(WndModifiers::class.java, "modifier_tags", desc, tagsDesc)
                }
                if (modifier.dependencies.isNotEmpty()) {
                    val dependenciesDesc =
                        modifier.dependencies.joinToString(
                            separator = ", ",
                            transform = { Modifier.ALL[it].localizedName() },
                        )
                    desc =
                        Messages.get(
                            WndModifiers::class.java,
                            "modifier_dependencies",
                            desc,
                            dependenciesDesc,
                        )
                }
                if (modifier.conflicts().isNotEmpty()) {
                    val conflictsDesc =
                        modifier.conflicts().joinToString(
                            separator = ", ",
                            transform = { it.localizedName() },
                        )
                    desc =
                        Messages.get(
                            WndModifiers::class.java,
                            "modifier_conflicts",
                            desc,
                            conflictsDesc,
                        )
                }
                ShatteredPixelDungeon.scene().add(
                    WndMessage(desc),
                )
            }
        }
        horizontal {
            stack {
                val iconsShow =
                    useAnimation(Unit, showCompletion, 0.2f) {
                        it
                    }

                val offset =
                    if (iconsShow > 0f) {
                        val iconsRes =
                            vertical(background = Chrome.Type.GREY_BUTTON.descriptor()) {
                                val score = TCPDScores.load().modifierScore(modifier)

                                val winsText = score.wins.toString()
                                val lossesText = score.losses.toString()

                                val minWidth =
                                    measureTextWidth(
                                        "0".repeat(
                                            max(
                                                winsText.length,
                                                lossesText.length,
                                            ),
                                        ),
                                        6,
                                    )

                                val winsWidth = measureTextWidth(winsText, 6)
                                val lossesWidth = measureTextWidth(lossesText, 6)
                                val winsPadding = max(0f, minWidth - winsWidth)
                                val lossesPadding = max(0f, minWidth - lossesWidth)

                                top().setStyle(top().style().copy(itemSpacing = 1))
                                horizontal {
                                    val icon =
                                        if (score.wins > 0) {
                                            TCPDIcons.AMULET_SMALL
                                        } else {
                                            TCPDIcons.AMULET_SMALL_DULL
                                        }
                                    image(icon.descriptor(), allocatedSize = Vec2(5, 6))
                                    top().addSpace(winsPadding.roundToInt())
                                    label(winsText, 6)
                                }

                                horizontal {
                                    val icon =
                                        if (score.losses > 50) {
                                            TCPDIcons.SKULL_SMALL_BLACK
                                        } else if (score.losses > 0) {
                                            TCPDIcons.SKULL_SMALL_RED
                                        } else {
                                            TCPDIcons.SKULL_SMALL
                                        }
                                    image(icon.descriptor(), allocatedSize = Vec2(5, 6))
                                    top().addSpace(lossesPadding.roundToInt())
                                    label(lossesText, 6)
                                }
                            }
                        (iconsRes.response.rect.width() * iconsShow).roundToInt() - 1
                    } else {
                        0
                    }

                margins(Margins.only(left = offset)) {
                    verticalJustified {
                        withEnabled(editable) {
                            redButton {
                                val res =
                                    shrinkToFitLabel(
                                        modifier.localizedName(),
                                        9,
                                        availableSpace =
                                            top()
                                                .nextAvailableSpace()
                                                .width() -
                                                Icons.CHECKED
                                                    .descriptor()
                                                    .size()
                                                    .x,
                                    )
                                drawRedCheckbox(modifiers.isEnabled(modifier), res.response.rect)
                            }.onClick {
                                modifiers.toggle(modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Ui.editStringButton(
    modifiers: Modifiers,
    editable: Boolean,
) {
    iconButton(Icons.SCROLL_COLOR.descriptor()).onClick {
        ShatteredPixelDungeon.scene().add(
            object : WndTextInput(
                Messages.get(WndModifiers::class.java, "edit_title"),
                Messages.get(WndModifiers::class.java, "edit_body"),
                modifiers.serializeToString(),
                256,
                false,
                Messages.get(WndModifiers::class.java, "edit_set"),
                Messages.get(WndModifiers::class.java, "edit_clear"),
            ) {
                override fun onSelect(
                    positive: Boolean,
                    text: String,
                ) {
                    val textValue =
                        if (editable && !positive) {
                            ""
                        } else {
                            text
                        }
                    if (editable) {
                        try {
                            val trimmed = textValue.trim()
                            if (trimmed.isBlank()) {
                                modifiers.disableAll()
                            } else {
                                modifiers.enableFrom(
                                    Modifiers.deserializeFromString(
                                        trimmed,
                                    ),
                                )
                            }
                        } catch (e: Exception) {
                            ShatteredPixelDungeon.scene().add(
                                WndError(
                                    Messages.get(
                                        WndModifiers::class.java,
                                        "edit_error",
                                    ),
                                ),
                            )
                        }
                    }
                    super.onSelect(positive, textValue)
                }
            },
        )
    }
}

private fun Ui.filterBar(sort: MutableHookRef<FilterOptions>) {
    var curSort by sort
    horizontal {
        rightToLeft {
            redButton(margins = Margins.only(top = 1, bottom = 2, left = 1, right = 1)) {
                rightToLeft {
                    val icon =
                        sort
                            .get()
                            .filter.selectedTag
                            ?.icon() ?: Icons.MAGNIFY.descriptor()
                    image(icon, Vec2(16, 16))
                }
            }.onClick {
                ShatteredPixelDungeon.scene().add(
                    object : WndFilter(curSort.filter) {
                        override fun onBackPressed() {
                            curSort = curSort.copy(filter = filter)
                            super.onBackPressed()
                        }
                    },
                )
            }
            columns(floatArrayOf(1f, 1f), spacing = 1) {
                arrayOf(
                    SortType.Default,
                    SortType.Name,
                    SortType.New,
                    SortType.Completed,
                ).forEach { sortType ->
                    val selected = curSort.sortType == sortType
                    withEnabled(sortType.available()) {
                        customButton {
                            withRedButtonBackground(
                                this,
                                selected,
                                margins = Margins.ZERO,
                            ) {
                                horizontal {
                                    val arrow =
                                        appearingIcon(
                                            Icons.COMPASS.descriptor(),
                                            selected,
                                            easingDisappear = null,
                                        )

                                    val rotateAnim =
                                        ctx().getOrPutMemory(top().id.with("rotateAnim")) {
                                            AnimationState(curSort.sortReverse).also {
                                                it.easingUp = ::easeOutBack
                                                it.easingDown = ::easeInBack
                                            }
                                        }
                                    arrow.widget.hardlight(0f, 0f, 0f)
                                    arrow.widget.am = 1.5f
                                    arrow.widget.originToCenter()
                                    arrow.widget.angle =
                                        rotateAnim.animate(
                                            curSort.sortReverse,
                                            0.2f,
                                        ) { it * 180f }

                                    val label = label(sortType.localizedName(), 7)
                                    if (selected) {
                                        label.widget.hardlight(0xFFFF44)
                                    } else {
                                        label.widget.resetColor()
                                        dimInactiveText(label, sortType.available())
                                    }
                                }
                            }
                        }.onClick {
                            curSort =
                                if (selected) {
                                    curSort.copy(
                                        sortType = sortType,
                                        sortReverse = !curSort.sortReverse,
                                    )
                                } else {
                                    curSort.copy(sortType = sortType, sortReverse = false)
                                }
                        }
                    }
                }
            }
        }
    }
}

private open class WndFilter(
    var filter: Filter,
) : TcpdWindow() {
    init {
        maxSize = Vec2(120, (PixelScene.uiCamera.height * 0.9f).toInt())
    }

    override fun Ui.drawUi() {
        val allTags = Tag.entries
        val tagWidth = 20
        val tagHeight = 20
        val availableSpace = top().nextAvailableSpace()
        val itemsPerRow = (availableSpace.width() + 2) / (tagWidth + 2)
        val rows = ceil(allTags.size.toFloat() / itemsPerRow).toInt()
        val requiredWidth = itemsPerRow * (tagWidth + 2) - 2

        withLayout(
            availableSpace =
                Rect.fromSize(
                    availableSpace.min,
                    Vec2(requiredWidth, availableSpace.max.y),
                ),
        ) {
            verticalJustified {
                label(Messages.get(WndModifiers::class.java, "tags"), 9).widget.hardlight(
                    TITLE_COLOR,
                )
                top().addSpace(2)
                val selected = filter.selectedTag
                rightToLeft {
                    val icon = selected?.icon() ?: Icons.MAGNIFY.descriptor()
                    val name =
                        selected?.localizedName() ?: Messages.get(
                            WndModifiers::class.java,
                            "select_tag",
                        )
                    withEnabled(selected != null) {
                        margins(Margins.only(top = 2)) {
                            iconButton(Icons.INFO.descriptor()).onClick {
                                ShatteredPixelDungeon.scene().add(
                                    WndMessage(selected!!.localizedDesc()),
                                )
                            }
                        }
                    }
                    verticalJustified {
                        horizontal(Chrome.Type.GREY_BUTTON.descriptor()) {
                            image(icon, Vec2(16, 16))

                            margins(Margins.only(top = 4)) {
                                shrinkToFitLabel(name, 9)
                            }
                        }
                    }
                }
                PaginatedList(rows, tagHeight).show(this) { row ->
                    columns(FloatArray(itemsPerRow) { 1f }) {
                        for (i in 0 until itemsPerRow) {
                            allTags.getOrNull(row * itemsPerRow + i)?.let { tag ->
                                customButton {
                                    withRedButtonBackground(
                                        this,
                                        filter.selectedTag == tag,
                                        Margins.ZERO,
                                    ) {
                                        image(tag.icon(), Vec2(16, 16))
                                    }
                                }.onClick {
                                    filter =
                                        if (filter.selectedTag == tag) {
                                            Filter()
                                        } else {
                                            Filter(tag)
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface SortType {
    fun sort(modifiers: MutableList<Modifier>)

    fun id(): String

    fun localizedName(): String = Messages.get(WndModifiers::class.java, "sort_${id()}")

    fun available(): Boolean = true

    data object Default : SortType {
        override fun sort(modifiers: MutableList<Modifier>) {}

        override fun id(): String = "default"
    }

    data object Name : SortType {
        override fun sort(modifiers: MutableList<Modifier>) {
            modifiers.sortBy {
                it.localizedName()
            }
        }

        override fun id(): String = "name"
    }

    data object New : SortType {
        override fun sort(modifiers: MutableList<Modifier>) {
            modifiers.sortByDescending {
                it.id
            }
        }

        override fun id(): String = "new"
    }

    data object Completed : SortType {
        val comparator: Comparator<Modifier> =
            Comparator { a, b ->
                val scores = TCPDScores.load()
                val aScore = scores.modifierScore(a)
                val bScore = scores.modifierScore(b)
                when {
                    (aScore.wins > 0 || bScore.wins > 0) && (aScore.wins != bScore.wins) ->
                        -aScore.wins.compareTo(
                            bScore.wins,
                        )

                    (aScore.losses > 0 || bScore.losses > 0) && (aScore.losses != bScore.losses) ->
                        -aScore.losses.compareTo(
                            bScore.losses,
                        )

                    else -> 0
                }
            }

        override fun sort(modifiers: MutableList<Modifier>) {
            modifiers.sortWith(comparator)
        }

        override fun id(): String = "completed"

//        override fun available(): Boolean = false
    }
}

private data class FilterOptions(
    val sortType: SortType,
    val sortReverse: Boolean,
    val filter: Filter = Filter(),
) {
    fun apply(
        modifiers: MutableList<Modifier>,
        filter: Boolean = true,
        sort: Boolean = true,
    ) {
        if (filter && this.filter.selectedTag != null) {
            modifiers.retainAll {
                it.tags.contains(this.filter.selectedTag)
            }
        }
        if (sort) {
            sortType.sort(modifiers)
            if (sortReverse) {
                modifiers.reverse()
            }
        }
    }
}

private data class Filter(
    val selectedTag: Tag? = null,
)

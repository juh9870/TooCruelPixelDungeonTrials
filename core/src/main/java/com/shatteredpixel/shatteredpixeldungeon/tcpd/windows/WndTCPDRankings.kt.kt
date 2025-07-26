package com.shatteredpixel.shatteredpixeldungeon.tcpd.windows

import com.shatteredpixel.shatteredpixeldungeon.Rankings
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet
import com.shatteredpixel.shatteredpixeldungeon.tcpd.TCPDScores
import com.shatteredpixel.shatteredpixeldungeon.tcpd.Trial
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.TcpdWindow
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.Vec2
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.hooks.useMemo
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.layout.Ui
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.descriptor
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.PaginatedList
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.bitmapLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.customButton
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.fixedSize
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.flare
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.horizontal
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.image
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.label
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.rightToLeft
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.shrinkToFitLabel
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.stackJustified
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.widgets.verticalJustified
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRanking
import kotlin.math.min

class WndTCPDRankings(
    private val trial: Trial,
) : TcpdWindow() {
    init {
        maxSize =
            Vec2(
                min(160, (PixelScene.uiCamera.width * 0.9f).toInt()),
                (PixelScene.uiCamera.height * 0.9f).toInt(),
            )
    }

    val score = TCPDScores.load().trialScore(trial)

    override fun Ui.drawUi() {
        verticalJustified {
            shrinkToFitLabel(trial.name, 12).widget.hardlight(
                TITLE_COLOR,
            )

            top().addSpace(2)

            val latestRecord by useMemo<Rankings.Record?>(Unit) {
                Rankings.INSTANCE.load()
                if (Rankings.INSTANCE.lastRecord < Rankings.INSTANCE.records.size) {
                    Rankings.INSTANCE.records[Rankings.INSTANCE.lastRecord]
                } else {
                    null
                }
            }

            val score = score
            if (score == null) {
                val text = label(Messages.get(WndTCPDRankings::class.java, "no_games"), 8)
                text.widget.hardlight(0xCCCCCC)
            } else {
                val records = score.load()
                PaginatedList(records.size, 16).show(this) {
                    val rec = records[it]
                    record(rec, it, rec.gameID == latestRecord?.gameID)
                }
            }
        }
    }

    private fun Ui.record(
        record: Rankings.Record,
        pos: Int,
        latest: Boolean,
    ) {
        val odd = pos % 2

        var hr = 1f
        var hg = 1f
        var hb = 1f
        val seeded = record.customSeed != null && record.customSeed.isNotEmpty()

        val textHardlight =
            if (record.win) {
                TEXT_WIN[odd]
            } else {
                TEXT_LOSE[odd]
            }

        val shield =
            if (seeded) {
                hr = 1f
                hg = 1.5f
                hb = 0.67f
                Icons.SEED.descriptor()
            } else if (record.win) {
                TextureDescriptor.ItemSprite(ItemSpriteSheet.AMULET)
            } else if (record.ascending) {
                hr = 0.4f
                hg = 0.4f
                hb = 0.7f
                TextureDescriptor.ItemSprite(ItemSpriteSheet.AMULET)
            } else {
                TextureDescriptor.ItemSprite(ItemSpriteSheet.TOMB)
            }

        customButton {
            rightToLeft {
                withLayout { }
                fixedSize(Vec2(16, 16)) {
                    stackJustified {
                        val icon = image(TextureDescriptor.HeroClassIcon(record.heroClass))
                        if (record.heroClass == HeroClass.ROGUE) {
                            icon.widget.brightness(2f)
                        }

                        if (record.herolevel != 0) {
                            val l = bitmapLabel(record.herolevel.toString())
                            l.widget.hardlight(textHardlight)
                        }
                    }
                }

                fixedSize(Vec2(16, 16)) {
                    if (!record.win && record.depth != 0) {
                        stackJustified {
                            image(Icons.STAIRS.descriptor())
                            val l = bitmapLabel(record.depth.toString())
                            l.widget.hardlight(textHardlight)
                        }
                    }
                }

                horizontal {
                    fixedSize(Vec2(16, 16)) {
                        stackJustified {
                            if (latest) {
                                flare(6, 24f).let { flare ->
                                    flare.widget.angularSpeed = 90f
                                    flare.widget.color(if (record.win) FLARE_WIN else FLARE_LOSE)
                                }
                            }
                            val shieldImg = image(shield)
                            shieldImg.widget.hardlight(hr, hg, hb)
                            val l = bitmapLabel((pos + 1).toString())
                            l.widget.hardlight(textHardlight)
                        }
                    }

                    fixedSize(Vec2(top().nextAvailableSpace().width(), 16)) {
                        stackJustified(justifyHorizontal = false) {
                            val desc =
                                label(
                                    Messages.titleCase(record.desc()),
                                    7,
                                    multiline = true,
                                )
                            desc.widget.hardlight(textHardlight)
                        }
                    }
                }
            }
        }.onClick {
            ShatteredPixelDungeon.scene().add(WndRanking(record))
        }
    }

    companion object {
        private val TEXT_WIN: IntArray = intArrayOf(0xFFFF88, 0xB2B25F)
        private val TEXT_LOSE: IntArray = intArrayOf(0xDDDDDD, 0x888888)
        const val FLARE_WIN: Int = 0x888866
        const val FLARE_LOSE: Int = 0x666666
    }
}

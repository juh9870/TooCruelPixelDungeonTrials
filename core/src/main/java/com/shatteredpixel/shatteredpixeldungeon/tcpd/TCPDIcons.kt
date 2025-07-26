package com.shatteredpixel.shatteredpixeldungeon.tcpd

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.tcpd.gui.painter.TextureDescriptor
import com.watabou.noosa.Image

enum class TCPDIcons {
    CHECKED,
    RED_CHECKED,
    DIAMOND_CHECKED,
    UNCHECKED,
    CLOSE,
    NOTES,

    AMULET_SMALL,
    AMULET_SMALL_DULL,
    AMULET_GREEN,
    SKULL_SMALL,
    SKULL_SMALL_RED,
    SKULL_SMALL_BLACK,

    EYE_OPEN,
    EYE_CLOSED,

    GOLDEN_RING,
    RANKINGS_DULL,

    CHALLENGE_DULL,
    CHALLENGE_GOLD,
    CHALLENGE_GOLD_BLOOD,
    CHALLENGE_GOLD_OOZE,
    CHALLENGE_DIAMOND,
    CHALLENGE_DIAMOND_BLOOD,
    CHALLENGE_DIAMOND_OOZE,

    RAT,
    MAP,
    MIMIC,
    CHEST,
    STAR,
    TOKENS,
    CAKE,
    UP_DOWN_ARROWS,
    DWARF_KING,
    ;

    fun descriptor(): TextureDescriptor = TextureDescriptor.TCPDIcon(this)

    companion object {
        fun get(type: TCPDIcons): Image {
            val icon = Image(Assets.Interfaces.ICONS_TCPD)
            when (type) {
                CHECKED ->
                    icon.frame(icon.texture.uvRect(0f, 0f, 12f, 12f))

                RED_CHECKED ->
                    icon.frame(icon.texture.uvRect(24f, 0f, 36f, 12f))

                DIAMOND_CHECKED ->
                    icon.frame(icon.texture.uvRect(36f, 0f, 48f, 12f))

                UNCHECKED ->
                    icon.frame(icon.texture.uvRect(12f, 0f, 24f, 12f))

                CLOSE ->
                    icon.frame(icon.texture.uvRect(0f, 12f, 11f, 23f))

                NOTES ->
                    icon.frame(icon.texture.uvRect(11f, 12f, 21f, 23f))

                AMULET_SMALL ->
                    icon.frame(icon.texture.uvRect(60f, 0f, 65f, 5f))

                AMULET_SMALL_DULL ->
                    icon.frame(icon.texture.uvRect(65f, 0f, 70f, 5f))

                AMULET_GREEN ->
                    icon.frame(icon.texture.uvRect(70f, 0f, 75f, 5f))

                SKULL_SMALL ->
                    icon.frame(icon.texture.uvRect(60f, 5f, 65f, 11f))

                SKULL_SMALL_RED ->
                    icon.frame(icon.texture.uvRect(65f, 5f, 70f, 11f))

                SKULL_SMALL_BLACK ->
                    icon.frame(icon.texture.uvRect(70f, 5f, 75f, 11f))

                EYE_OPEN ->
                    icon.frame(icon.texture.uvRect(99f, 0f, 106f, 7f))

                EYE_CLOSED ->
                    icon.frame(icon.texture.uvRect(106f, 0f, 113f, 7f))

                CHALLENGE_DULL ->
                    icon.frame(icon.texture.uvRect(0f, 23f, 14f, 35f))

                CHALLENGE_GOLD ->
                    icon.frame(icon.texture.uvRect(14f, 23f, 28f, 35f))

                CHALLENGE_GOLD_BLOOD ->
                    icon.frame(icon.texture.uvRect(28f, 23f, 42f, 35f))

                CHALLENGE_GOLD_OOZE ->
                    icon.frame(icon.texture.uvRect(42f, 23f, 56f, 35f))

                CHALLENGE_DIAMOND ->
                    icon.frame(icon.texture.uvRect(56f, 23f, 70f, 35f))

                CHALLENGE_DIAMOND_BLOOD ->
                    icon.frame(icon.texture.uvRect(70f, 23f, 84f, 35f))

                CHALLENGE_DIAMOND_OOZE ->
                    icon.frame(icon.texture.uvRect(84f, 23f, 98f, 35f))

                GOLDEN_RING ->
                    icon.frame(icon.texture.uvRect(0f, 35f, 18f, 53f))

                RANKINGS_DULL ->
                    icon.frame(icon.texture.uvRect(18f, 35f, 35f, 51f))

                RAT ->
                    icon.frame(icon.texture.uvRect(0f, 64f, 16f, 80f))

                MAP ->
                    icon.frame(icon.texture.uvRect(16f, 64f, 32f, 80f))

                MIMIC ->
                    icon.frame(icon.texture.uvRect(32f, 64f, 48f, 80f))

                CHEST ->
                    icon.frame(icon.texture.uvRect(48f, 64f, 64f, 80f))

                STAR ->
                    icon.frame(icon.texture.uvRect(64f, 64f, 80f, 80f))

                TOKENS ->
                    icon.frame(icon.texture.uvRect(80f, 64f, 96f, 80f))

                CAKE ->
                    icon.frame(icon.texture.uvRect(96f, 64f, 112f, 80f))

                UP_DOWN_ARROWS ->
                    icon.frame(icon.texture.uvRect(112f, 64f, 128f, 80f))

                DWARF_KING ->
                    icon.frame(icon.texture.uvRect(0f, 80f, 16f, 96f))
            }
            return icon
        }
    }
}

package com.shatteredpixel.shatteredpixeldungeon.tcpd.items

import com.shatteredpixel.shatteredpixeldungeon.Assets
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero
import com.shatteredpixel.shatteredpixeldungeon.items.Item
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog
import com.watabou.noosa.audio.Sample
import com.watabou.utils.Bundle

class IOU : Item {
    init {
        image = ItemSpriteSheet.GUIDE_PAGE
    }

    private var messageKey: String = "iou"
    private var item: Item = DummyItem()

    constructor() : super()

    constructor(messageKey: String, item: Item) : this() {
        this.messageKey = messageKey
        this.item = item
    }

    override fun isUpgradable(): Boolean = false

    override fun isIdentified(): Boolean = true

    override fun name(): String = Messages.get(this, messageKey + "_name", item.name())

    override fun desc(): String = Messages.get(this, messageKey + "_desc", Messages.titleCase(item.name()))

    override fun doPickUp(
        hero: Hero,
        pos: Int,
    ): Boolean {
        GLog.w(desc())
        Sample.INSTANCE.play(Assets.Sounds.ITEM)
        hero.spendAndNext(TIME_TO_PICK_UP)
        return true
    }

    override fun storeInBundle(bundle: Bundle) {
        super.storeInBundle(bundle)
        bundle.put(MESSAGE_KEY, messageKey)
        bundle.put(ITEM, item)
    }

    override fun restoreFromBundle(bundle: Bundle) {
        super.restoreFromBundle(bundle)
        messageKey = bundle.getString(MESSAGE_KEY)
        item = bundle.get(ITEM) as Item
    }

    companion object {
        private const val MESSAGE_KEY = "message_key"
        private const val ITEM = "item"

        fun protected(item: Item): IOU = IOU("protected", item)
    }
}

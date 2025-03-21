package com.shatteredpixel.shatteredpixeldungeon.tcpd.hooks

import com.shatteredpixel.shatteredpixeldungeon.items.Item

/**
 * This hook is called when an item is generated by a generator
 *
 * This hook is only applied to calls to
 * [Generator.random][com.shatteredpixel.shatteredpixeldungeon.items.Generator.random]
 * and [Generator.randomUsingDefaults][com.shatteredpixel.shatteredpixeldungeon.items.Generator.randomUsingDefaults]
 * other item-generating methods do not call this method to minimize the
 * possible logic impact
 *
 * This hook should only be used to fully replace the generated item. For
 * modifying generated items, utilize level hooks
 *
 * This is a dangerous hook. Users might want to get specific results from the
 * generator and may cause infinite loop if they don't get the expected result.
 * A particularly bad example would be to curse every item generated by a
 * generator, as Statues and some rooms will keep rerolling until they get a
 * non-cursed item.
 */
fun transformGeneratedItem(item: Item): Item = item

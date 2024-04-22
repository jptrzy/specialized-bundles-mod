package xyz.jptrzy.specialized.bundles.items

import eu.pb4.polymer.core.api.item.PolymerItem
import eu.pb4.polymer.core.api.item.PolymerItemUtils
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BundleItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Rarity
import xyz.jptrzy.specialized.bundles.SpecializedBundlesMod.Companion.SCROLL_CASE_MODEL
import java.util.*
import kotlin.math.min


class ScrollCaseItem() : BundleLikeItem(
    64 * 16,
    FabricItemSettings()
        .maxCount(1)
        .rarity(Rarity.RARE)
), PolymerItem {

    override fun getPolymerItem(itemStack: ItemStack?, player: ServerPlayerEntity?): Item {
        return SCROLL_CASE_MODEL.item();
    }

    override fun getPolymerCustomModelData(itemStack: ItemStack?, player: ServerPlayerEntity?): Int {
        return SCROLL_CASE_MODEL.value()
    }

    override fun isAcceptedItemStack(stack: ItemStack): Boolean {
        return Arrays.stream(arrayOf(
            Items.BOOK,
            Items.ENCHANTED_BOOK,
            Items.WRITTEN_BOOK,
            Items.WRITABLE_BOOK,
            Items.MAP,
            Items.FILLED_MAP,
        )).anyMatch { item -> stack.isOf(item) }
    }
}
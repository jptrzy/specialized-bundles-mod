package xyz.jptrzy.specialized.bundles.items

import eu.pb4.polymer.core.api.item.PolymerItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.BundleItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.network.ServerPlayerEntity


class LunchboxItem : BundleLikeItem, PolymerItem {
    constructor() : super(
        64,
        FabricItemSettings()
    ){

    }

    override fun getPolymerItem(itemStack: ItemStack?, player: ServerPlayerEntity?): Item {
        return if (itemStack!!.count > 32) Items.REDSTONE else Items.SPRUCE_WOOD;
    }

    override fun getPolymerCustomModelData(itemStack: ItemStack?, player: ServerPlayerEntity?): Int {
        return super.getPolymerCustomModelData(itemStack, player)
    }

}
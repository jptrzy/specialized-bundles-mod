package xyz.jptrzy.specialized.bundles

import eu.pb4.polymer.core.api.item.PolymerItem
import eu.pb4.polymer.resourcepack.api.PolymerModelData
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils
import eu.pb4.polymer.resourcepack.impl.PolymerResourcePackMod
import eu.pb4.polymer.resourcepack.impl.client.rendering.PolymerResourcePack
import net.fabricmc.api.ModInitializer
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import xyz.jptrzy.specialized.bundles.items.LunchboxItem
import xyz.jptrzy.specialized.bundles.items.ScrollCaseItem


class SpecializedBundlesMod : ModInitializer {

    companion object {
        val MOD_ID = "specialized_bundles";
        val LOGGER = LoggerFactory.getLogger(MOD_ID)

        val LUNCHBOX_ITEM: Item = LunchboxItem()
        val SCROLL_CASE_ITEM: Item = ScrollCaseItem()

        lateinit var SCROLL_CASE_MODEL: PolymerModelData;

        fun id(name: String) : Identifier {
            return Identifier(MOD_ID, name)
        }
    }


    override fun onInitialize() {
        if (!PolymerResourcePackUtils.addModAssets(MOD_ID)) {
            LOGGER.warn("Can't add Polymer Mod Assets")
        }

        SCROLL_CASE_MODEL = PolymerResourcePackUtils.requestModel(Items.BUNDLE, id("item/scroll_case"))

        PolymerResourcePackUtils.markAsRequired()

        Registry.register(Registries.ITEM, id("lunchbox"), LUNCHBOX_ITEM)
        Registry.register(Registries.ITEM, id("scroll_case"), SCROLL_CASE_ITEM)
    }
}

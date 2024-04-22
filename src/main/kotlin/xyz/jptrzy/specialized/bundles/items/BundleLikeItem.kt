package xyz.jptrzy.specialized.bundles.items

import net.minecraft.client.item.TooltipContext
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.StackReference
import net.minecraft.item.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.ClickType
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import xyz.jptrzy.specialized.bundles.SpecializedBundlesMod.Companion.LOGGER
import java.util.*
import java.util.stream.Stream
import kotlin.math.min
import kotlin.reflect.cast


open class BundleLikeItem(
    var max_storage: Int,
    settings: Settings?
) : Item(settings) {

    // CLicked with This Item on another Item
    override fun onStackClicked(stack: ItemStack, slot: Slot, clickType: ClickType, player: PlayerEntity): Boolean {
        if (clickType != ClickType.RIGHT) {
            return false
        } else {
            val itemStack = slot.stack
            if (itemStack.isEmpty) {
                this.playRemoveOneSound(player)
                removeFirstStack(stack).ifPresent { removedStack: ItemStack? ->
                    addToBundle(stack, slot.insertStack(removedStack))
                }
            } else if (itemStack.item.canBeNested()) {

                val i = (max_storage - getBundleOccupancy(stack)) / getItemOccupancy(itemStack)
                if (i <= 0) {
                    return true
                }

                val taken = slot.takeStackRange(itemStack.count, i, player)
                val oldTaken = taken.copy()
                val j = addToBundle(stack, taken)
                if (j > 0) {
                    this.playInsertSound(player)
                } else {
                    slot.stack = oldTaken
                }
            }

            return true
        }
    }

    override fun onClicked(
        stack: ItemStack,
        otherStack: ItemStack,
        slot: Slot,
        clickType: ClickType,
        player: PlayerEntity,
        cursorStackReference: StackReference
    ): Boolean {
        if (clickType == ClickType.RIGHT && slot.canTakePartial(player)) {
            if (otherStack.isEmpty) {
                removeFirstStack(stack).ifPresent { removedStack: ItemStack? ->
                    this.playRemoveOneSound(
                        player
                    )
                    cursorStackReference.set(removedStack?.split(removedStack.item.maxCount))

                    if (removedStack != null && !removedStack.isEmpty) {
                        addToBundle(stack, removedStack)
                    }
                }
            } else {
                val i = addToBundle(stack, otherStack)
                if (i > 0) {
                    this.playInsertSound(player)
                    otherStack.decrement(i)
                }

                LOGGER.info("Clicked")
                slot.markDirty()


                return true
            }

            return true
        } else {
            return false
        }
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack = user.getStackInHand(hand)
        if (dropAllBundledItems(itemStack, user)) {
            this.playDropContentsSound(user)
            user.incrementStat(Stats.USED.getOrCreateStat(this))
            return TypedActionResult.success(itemStack, world.isClient())
        } else {
            return TypedActionResult.fail(itemStack)
        }
    }

/*
      Won't work, because of how polymer sync data with client.
      It can be working if we are displaying this item as a vanilla bundle
 */
//    override fun getTooltipData(stack: ItemStack): Optional<TooltipData>

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        val amount: Float = getBundleOccupancy(stack).toFloat() / 64f;

        tooltip.add(
            Text.translatable(
                "item.minecraft.bundle.fullness", *arrayOf<Any>(
                    if (amount.rem(1) != 0f) "⬇${amount.toInt() + 1}" else "${amount.toInt()}",
                    max_storage / 64
                )
            ).formatted(Formatting.GRAY)
        )
    }

    override fun onItemEntityDestroyed(entity: ItemEntity) {
        ItemUsage.spawnItemContents(entity, getBundledStacks(entity.stack))
    }

    private fun playRemoveOneSound(entity: Entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8f, 0.8f + entity.world.getRandom().nextFloat() * 0.4f)
    }

    private fun playInsertSound(entity: Entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8f, 0.8f + entity.world.getRandom().nextFloat() * 0.4f)
    }

    private fun playDropContentsSound(entity: Entity) {
        entity.playSound(
            SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,
            0.8f,
            0.8f + entity.world.getRandom().nextFloat() * 0.4f
        )
    }

    open fun isAcceptedItemStack(stack: ItemStack): Boolean {
        TODO("Not implemented filtering for BundleLikeItem Class");
    }

    companion object {

        fun getAmountFilled(stack: ItemStack): Float {
            return getBundleOccupancy(stack).toFloat() / (stack.item as BundleLikeItem).max_storage
        }

        private fun addToBundle(bundle: ItemStack, stack: ItemStack): Int {
            if (!stack.isEmpty && stack.item.canBeNested() && (bundle.item as BundleLikeItem).isAcceptedItemStack(stack)) {
                val nbtCompound = bundle.getOrCreateNbt()
                if (!nbtCompound.contains("Items")) {
                    nbtCompound.put("Items", NbtList())
                }

                val i = getBundleOccupancy(bundle)
                val j = getItemOccupancy(stack)
                val k =
                    min(stack.count.toDouble(), (((bundle.item as BundleLikeItem).max_storage - i) / j).toDouble()).toInt()
                var y = 0
                if (k == 0) {
                    return 0
                } else {
                    val nbtList = nbtCompound.getList("Items", 10)
                    val optional = canMergeStack(stack, nbtList)
                    if (optional.isPresent) {
                        val nbtCompound2 = optional.get()
                        val itemStack = ItemStack.fromNbt(nbtCompound2)
                        y = min(stack.maxCount - itemStack.count, k);
                        if (y > 0) {
                            itemStack.increment(y)
                            itemStack.writeNbt(nbtCompound2)
                            nbtList.remove(nbtCompound2)
                            nbtList.add(0, nbtCompound2)
                        }
                    }

                    if (k - y > 0) {
                        val itemStack2 = stack.copyWithCount(k - y)
                        if (itemStack2.isEmpty) return k
                        val nbtCompound3 = NbtCompound()
                        itemStack2.writeNbt(nbtCompound3)
                        nbtList.add(0, nbtCompound3)
                    }

                    return k
                }
            } else {
                return 0
            }
        }

        private fun canMergeStack(stack: ItemStack, items: NbtList): Optional<NbtCompound> {
            if (stack.isOf(Items.BUNDLE)) {
                return Optional.empty()
            } else {
                var var10000: Stream<*> = items.stream()
                Objects.requireNonNull(NbtCompound::class)
                var10000 = var10000.filter(NbtCompound::class::isInstance)
                Objects.requireNonNull(NbtCompound::class);

                return  var10000.map(NbtCompound::class::cast).filter { item: NbtCompound ->
                    ItemStack.canCombine(
                        ItemStack.fromNbt(item),
                        stack
                    )
                }.findFirst()
            }
        }

        private fun getItemOccupancy(stack: ItemStack): Int {
            if (stack.isOf(Items.BUNDLE)) {
                return 4 + getBundleOccupancy(stack)
            } else {
                if ((stack.isOf(Items.BEEHIVE) || stack.isOf(Items.BEE_NEST)) && stack.hasNbt()) {
                    val nbtCompound = BlockItem.getBlockEntityNbt(stack)
                    if (nbtCompound != null && !nbtCompound.getList("Bees", 10).isEmpty()) {
                        return 64
                    }
                }

                return 64 / stack.maxCount
            }
        }

        private fun getBundleOccupancy(stack: ItemStack): Int {
            return getBundledStacks(stack).mapToInt { itemStack: ItemStack ->
                getItemOccupancy(
                    itemStack
                ) * itemStack.count
            }.sum()
        }

        private fun removeFirstStack(stack: ItemStack): Optional<ItemStack> {
            val nbtCompound = stack.getOrCreateNbt()
            if (!nbtCompound.contains("Items")) {
                return Optional.empty()
            } else {
                val nbtList = nbtCompound.getList("Items", 10)
                if (nbtList.isEmpty()) {
                    return Optional.empty()
                } else {
                    val i: Int = 0
                    val nbtCompound2 = nbtList.getCompound(0)
                    val itemStack = ItemStack.fromNbt(nbtCompound2)
                    nbtList.removeAt(0)
                    if (nbtList.isEmpty()) {
                        stack.removeSubNbt("Items")
                    }

                    return Optional.of(itemStack)
                }
            }
        }

        private fun dropAllBundledItems(stack: ItemStack, player: PlayerEntity): Boolean {
            val nbtCompound = stack.getOrCreateNbt()
            if (!nbtCompound.contains("Items")) {
                return false
            } else {
                if (player is ServerPlayerEntity) {
                    val nbtList = nbtCompound.getList("Items", 10)

                    for (i in nbtList.indices) {
                        val nbtCompound2 = nbtList.getCompound(i)
                        val itemStack = ItemStack.fromNbt(nbtCompound2)
                        player.dropItem(itemStack, true)
                    }
                }

                stack.removeSubNbt("Items")
                return true
            }
        }

        private fun getBundledStacks(stack: ItemStack): Stream<ItemStack> {
            val nbtCompound = stack.nbt
            if (nbtCompound == null) {
                return Stream.empty()
            } else {
                val nbtList = nbtCompound.getList("Items", 10)
                val var10000: Stream<*> = nbtList.stream()
                Objects.requireNonNull(NbtCompound::class)
                return var10000.map(NbtCompound::class::cast).map(ItemStack::fromNbt)
            }
        }
    }
}
package dev.turtywurty.turtymultiloader.registration;

import net.minecraft.data.BlockFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * All registrations produced by {@link WoodSetBuilder}.
 */
public record WoodSet(
    Identifier id,
    QueuedValue<BlockSetType> blockSetType,
    QueuedValue<WoodType> woodType,
    BlockWithItem planks,
    BlockWithItem log,
    BlockWithItem strippedLog,
    BlockWithItem strippedWood,
    BlockWithItem wood,
    BlockWithItem leaves,
    BlockWithItem sapling,
    BlockWithItem stairs,
    BlockWithItem slab,
    BlockWithItem fence,
    BlockWithItem fenceGate,
    BlockWithItem door,
    BlockWithItem trapdoor,
    BlockWithItem pressurePlate,
    BlockWithItem button,
    RegistrationHandle<Block, Block> sign,
    RegistrationHandle<Block, Block> wallSign,
    RegistrationHandle<Block, Block> hangingSign,
    RegistrationHandle<Block, Block> wallHangingSign,
    RegistrationHandle<Item, Item> signItem,
    RegistrationHandle<Item, Item> hangingSignItem,
    RegistrationHandle<EntityType<?>, EntityType<Boat>> boatEntityType,
    RegistrationHandle<EntityType<?>, EntityType<ChestBoat>> chestBoatEntityType,
    RegistrationHandle<Item, Item> boatItem,
    RegistrationHandle<Item, Item> chestBoatItem,
    TagKey<Block> logsBlockTag,
    TagKey<Item> logsItemTag
) {
    public String name() {
        return id.getPath();
    }

    public BlockFamily createBlockFamily() {
        return new BlockFamily.Builder(planks.get())
            .button(button.get())
            .fence(fence.get())
            .fenceGate(fenceGate.get())
            .pressurePlate(pressurePlate.get())
            .sign(sign.get(), wallSign.get())
            .slab(slab.get())
            .stairs(stairs.get())
            .door(door.get())
            .trapdoor(trapdoor.get())
            .recipeGroupPrefix("wooden")
            .recipeUnlockedBy("has_planks")
            .getFamily();
    }
}

package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.client.registration.AdditionalModel;
import dev.turtywurty.turtymultiloader.client.registration.ClientRegistrations;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class ClientRegistrationSmokeTest {
    private static final ModelLayerLocation EMPTY_LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, "empty"),
        "main"
    );

    public static AdditionalModel<BlockStateModel> STONE_MODEL;

    private ClientRegistrationSmokeTest() {
    }

    public static void register() {
        ClientRegistrations.registerModelLayer(
            EMPTY_LAYER,
            () -> LayerDefinition.create(new MeshDefinition(), 16, 16)
        );
        ClientRegistrations.registerBlockTintSources(
            List.of(new BlockTintSource() {
                @Override
                public int color(BlockState state) {
                    return 0xFFFFFFFF;
                }
            }),
            TestModContent.TEST_LOG
        );
        ClientRegistrations.registerBlockStateModelAugmenter(
            TestModContent.TEST_LOG,
            (context, models) -> {
                // No-op wrapper used to compile and exercise loader registration in client runs.
            }
        );
        STONE_MODEL = ClientRegistrations.registerAdditionalBlockStateModel(
            Identifier.withDefaultNamespace("block/stone")
        );
    }
}

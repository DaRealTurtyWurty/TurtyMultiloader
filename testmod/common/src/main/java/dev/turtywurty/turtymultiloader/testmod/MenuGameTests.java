package dev.turtywurty.turtymultiloader.testmod;

import dev.turtywurty.turtymultiloader.menu.sync.MenuDataSlots;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

public final class MenuGameTests {
    public static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(TestModContent.MOD_ID, "menu_service");

    private MenuGameTests() {
    }

    public static void verifyMenuService(GameTestHelper helper) {
        helper.assertTrue(
            BuiltInRegistries.MENU.getValue(TestModContent.TEST_MENU.id()) == TestModContent.TEST_MENU.get(),
            "Extended menu was not registered through the menu service"
        );

        long[] longValue = {0x12345678FEDCBA98L};
        double[] doubleValue = {Math.PI};
        float[] floatValue = {-1234.5F};
        boolean[] booleanValue = {true};
        int[] shortValue = {-12_345};
        MenuDataSlots data = MenuDataSlots.builder()
            .addLong(() -> longValue[0], value -> longValue[0] = value)
            .addDouble(() -> doubleValue[0], value -> doubleValue[0] = value)
            .addFloat(() -> floatValue[0], value -> floatValue[0] = (float) value)
            .addBoolean(() -> booleanValue[0], value -> booleanValue[0] = value)
            .add(() -> shortValue[0], value -> shortValue[0] = value)
            .build();

        long expectedLong = longValue[0];
        int[] encodedLong = encodedSlots(data, 0, 4);
        longValue[0] = 0;
        applyOverNetwork(data, 0, encodedLong);
        helper.assertTrue(longValue[0] == expectedLong, "Long menu data did not survive slot splitting");

        double expectedDouble = doubleValue[0];
        int[] encodedDouble = encodedSlots(data, 4, 4);
        doubleValue[0] = 0;
        applyOverNetwork(data, 4, encodedDouble);
        helper.assertTrue(
            Double.doubleToRawLongBits(doubleValue[0]) == Double.doubleToRawLongBits(expectedDouble),
            "Double menu data did not survive slot splitting"
        );

        int expectedFloat = Float.floatToRawIntBits(floatValue[0]);
        int[] encodedFloat = encodedSlots(data, 8, 2);
        floatValue[0] = 0;
        applyOverNetwork(data, 8, encodedFloat);
        helper.assertTrue(
            Float.floatToRawIntBits(floatValue[0]) == expectedFloat,
            "Float menu data did not survive slot splitting"
        );

        data.set(10, 0);
        helper.assertTrue(!booleanValue[0], "Boolean menu data did not decode zero as false");
        data.set(11, (short) data.get(11));
        helper.assertTrue(shortValue[0] == -12_345, "Signed-short menu data did not survive packet encoding");
        helper.assertTrue(data.getCount() == 12, "Wide menu values used the wrong number of slots");
        helper.succeed();
    }

    private static int[] encodedSlots(MenuDataSlots data, int start, int count) {
        int[] encoded = new int[count];
        for (int index = 0; index < count; index++)
            encoded[index] = data.get(start + index);
        return encoded;
    }

    /**
     * Simulates ClientboundContainerSetDataPacket's writeShort/readShort truncation.
     */
    private static void applyOverNetwork(MenuDataSlots data, int start, int[] encoded) {
        for (int index = 0; index < encoded.length; index++)
            data.set(start + index, (short) encoded[index]);
    }
}

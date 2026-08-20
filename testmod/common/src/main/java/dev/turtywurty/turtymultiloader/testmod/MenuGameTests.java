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
        boolean[] booleanValue = {true};
        MenuDataSlots data = MenuDataSlots.builder()
            .addLong(() -> longValue[0], value -> longValue[0] = value)
            .addDouble(() -> doubleValue[0], value -> doubleValue[0] = value)
            .addBoolean(() -> booleanValue[0], value -> booleanValue[0] = value)
            .build();

        long expectedLong = longValue[0];
        int low = data.get(0);
        int high = data.get(1);
        longValue[0] = 0;
        data.set(0, low);
        data.set(1, high);
        helper.assertTrue(longValue[0] == expectedLong, "Long menu data did not survive slot splitting");

        double expectedDouble = doubleValue[0];
        int doubleLow = data.get(2);
        int doubleHigh = data.get(3);
        doubleValue[0] = 0;
        data.set(2, doubleLow);
        data.set(3, doubleHigh);
        helper.assertTrue(
            Double.doubleToRawLongBits(doubleValue[0]) == Double.doubleToRawLongBits(expectedDouble),
            "Double menu data did not survive slot splitting"
        );

        data.set(4, 0);
        helper.assertTrue(!booleanValue[0], "Boolean menu data did not decode zero as false");
        helper.succeed();
    }
}

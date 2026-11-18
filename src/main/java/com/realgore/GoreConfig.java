package com.realgore;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GoreConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DISMEMBER_CHANCE;
    public static final ForgeConfigSpec.IntValue BLOOD_AMOUNT;
    public static final ForgeConfigSpec.IntValue STAIN_LIFETIME_SEC;
    public static final ForgeConfigSpec.IntValue GIB_LIFETIME_SEC;
    public static final ForgeConfigSpec.DoubleValue GIB_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue GIB_ON_EXPLOSION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("gore");

        DISMEMBER_CHANCE = b
                .comment("Множитель шанса отрубания конечностей (1.0 = обычный, 0 = выключено, 3.0 = очень часто)")
                .defineInRange("dismemberChance", 1.0D, 0.0D, 10.0D);

        BLOOD_AMOUNT = b
                .comment("Количество крови в процентах (100 = обычное, 0 = без крови)")
                .defineInRange("bloodAmountPercent", 100, 0, 400);

        STAIN_LIFETIME_SEC = b
                .comment("Сколько секунд кровавые пятна лежат на земле (на клиенте)")
                .defineInRange("bloodStainSeconds", 120, 5, 3600);

        GIB_LIFETIME_SEC = b
                .comment("Сколько секунд лежат отрубленные части тела")
                .defineInRange("bodyPartSeconds", 120, 5, 3600);

        GIB_DAMAGE = b
                .comment("Если смертельный удар сильнее этого урона, зомби разлетается на куски целиком")
                .defineInRange("gibDamage", 14.0D, 1.0D, 1000.0D);

        GIB_ON_EXPLOSION = b
                .comment("Разрывать зомби на куски при смерти от взрыва")
                .define("gibOnExplosion", true);

        b.pop();
        SPEC = b.build();
    }

    private GoreConfig() {
    }
}

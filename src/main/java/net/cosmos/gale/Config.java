package net.cosmos.gale;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue MAX_THRUST = BUILDER
            .comment("Maximum thrust output in pico-Newtons (pN) at full throttle.")
            .defineInRange("maxThrust", 600.0, 0.0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue MAX_AIRFLOW = BUILDER
            .comment("Maximum airflow in meters per second at full throttle.")
            .defineInRange("maxAirflow", 60.0, 0.0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue CHARGE_CAPACITY = BUILDER
            .comment("Charge capacity provided by a single wind charge. Determines max charge the thruster can hold.")
            .defineInRange("chargeCapacity", 1.0, 0.0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue CHARGE_DRAIN_DIVISOR = BUILDER
            .comment("Number of ticks it takes to fully drain one charge at 100% throttle. Lower = faster drain.")
            .defineInRange("chargeDrainDivisor", 200.0, 1.0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue OUTPUT_RAMP_SPEED = BUILDER
            .comment("How fast the throttle ramps up/down per tick. Higher = snappier response to redstone changes.")
            .defineInRange("outputRampSpeed", 0.05, 0.001, 1.0);

    static final ModConfigSpec SPEC = BUILDER.build();
}

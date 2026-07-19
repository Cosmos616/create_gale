package net.cosmos.gale.blockentities;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.eriksonn.aeronautics.content.particle.AirPoofParticleData;
import dev.eriksonn.aeronautics.content.particle.GustParticleData;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.physics.impl.none.StaticPhysicsPipeline;
import net.cosmos.gale.blocks.GaleDriveBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class GaleDriveBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements IHaveGoggleInformation, BlockEntityPropeller, BlockEntitySubLevelPropellerActor {

    private static final float OUTPUT_CHANGE_PER_TICK = 0.05F;
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR;
    private static final float RADIUS = 1.2F;

    private int redstoneStrength;

    private float previousOutput;
    private float currentOutput;
    private float targetOutput;

    private static final double MAX_THRUST = 600.0;
    private static final double MAX_AIRFLOW = 60.0;
    private static final float CHARGE_CAPACITY = 1.0F;



    /**
     * At full redstone power, one Wind Charge lasts 200 ticks,
     * or 10 seconds.
     */
    private static final float FULL_POWER_DRAIN_PER_TICK =
            CHARGE_CAPACITY / 200.0F;

    private float chargeRemaining;

    private float previousChargeRemaining;

    public float getRenderedChargeRemaining(float partialTick) {
        return Mth.lerp(
                partialTick,
                previousChargeRemaining,
                chargeRemaining
        );
    }


    public GaleDriveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GALE_DRIVE.get(), pos, state);
    }


    public void explode(Vec3 pos) {
        this.level().explode(null, (DamageSource)null, EXPLOSION_DAMAGE_CALCULATOR, pos.x(), pos.y(), pos.z(), 1.2F, false, Level.ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
    }

    public Level level() {
        return this.level;
    }

    static {
        EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(true, false, Optional.of(1.22F), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity()));
    }


    @Override
    public BlockEntityPropeller getPropeller() {
        return this;
    }

    @Override
    public boolean isActive() {
        return currentOutput > 0.001F
                && hasLoadedCharge();
    }

    @Override
    public double getThrust() {
        return currentOutput * MAX_THRUST;
    }

    @Override
    public double getAirflow() {
        return currentOutput * MAX_AIRFLOW;
    }

    @Override
    public double getScaledThrust() {
        return currentOutput * MAX_THRUST;
    }


    @Override
    public Direction getBlockDirection() {
        return getBlockState().getValue(GaleDriveBlock.FACING);
    }



    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean isPlayerSneaking
    ) {
        tooltip.add(
                Component.literal("    ")
                        .append(
                                getBlockState()
                                        .getBlock()
                                        .getName()
                                        .withStyle(ChatFormatting.WHITE)
                        )
        );

        tooltip.add(
                Component.literal("     ")
                        .append(
                                Component.literal("Thrust: ")
                                        .withStyle(ChatFormatting.GRAY)
                        )
                        .append(
                                Component.literal(formatThrust((float) getThrust()))
                                        .withStyle(ChatFormatting.AQUA)
                        )
        );

        tooltip.add(
                Component.literal("     ")
                        .append(
                                Component.literal("Airflow: ")
                                        .withStyle(ChatFormatting.GRAY)
                        )
                        .append(
                                Component.literal(formatAirflow((float) getAirflow()))
                                        .withStyle(ChatFormatting.AQUA)
                        )
        );



        tooltip.add(
                Component.literal("     ")
                        .append(
                                Component.literal("Wind Charges: ")
                                        .withStyle(ChatFormatting.GRAY)
                        )
                        .append(
                                Component.literal("x" + getWindChargeCount())
                                        .withStyle(ChatFormatting.GREEN)
                        )
        );

        return true;
    }


    private static String formatThrust(float thrust) {
        return String.format(
                Locale.ROOT,
                "%.2f pN",
                thrust
        );
    }

    private static String formatAirflow(float thrust) {
        return String.format(
                Locale.ROOT,
                "%.2f m/s",
                thrust
        );
    }

    public int getStoredWindChargeCount() {
        return inventory
                .getStackInSlot(WIND_CHARGE_SLOT)
                .getCount();
    }

    public int getTotalAvailableChargeCount() {
        return getStoredWindChargeCount()
                + (hasLoadedCharge() ? 1 : 0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        previousChargeRemaining = chargeRemaining;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GaleDriveBlockEntity blockEntity
    ) {
        blockEntity.previousChargeRemaining =
                blockEntity.chargeRemaining;

        int newStrength = Mth.clamp(
                level.getBestNeighborSignal(pos),
                0,
                15
        );

        if (newStrength != blockEntity.redstoneStrength) {
            blockEntity.redstoneStrength = newStrength;
            blockEntity.setChanged();
        }

        float throttle =
                blockEntity.redstoneStrength / 15.0F;

        boolean wantsToRun = throttle > 0.0F;

        boolean hasFuel = blockEntity.chargeRemaining > 0.0F;

        if (wantsToRun && !hasFuel) {
            hasFuel = blockEntity.tryLoadWindCharge();
        }

        blockEntity.targetOutput = hasFuel
                ? throttle
                : 0.0F;

        blockEntity.previousOutput =
                blockEntity.currentOutput;

        blockEntity.currentOutput = Mth.approach(
                blockEntity.currentOutput,
                blockEntity.targetOutput,
                OUTPUT_CHANGE_PER_TICK
        );

        if (wantsToRun && hasFuel) {
            blockEntity.dischargeWindCharge(throttle);
        }

        blockEntity.syncOutputWhenNeeded();
        blockEntity.syncChargeWhenNeeded();
    }



    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GaleDriveBlockEntity blockEntity
    ) {
        blockEntity.previousOutput =
                blockEntity.currentOutput;

        blockEntity.currentOutput = Mth.approach(
                blockEntity.currentOutput,
                blockEntity.targetOutput,
                OUTPUT_CHANGE_PER_TICK
        );

        blockEntity.previousChargeRemaining =
                blockEntity.chargeRemaining;

        float throttle =
                blockEntity.redstoneStrength / 15.0F;

        if (blockEntity.chargeRemaining > 0.0F && throttle > 0.0F) {
            blockEntity.chargeRemaining = Math.max(
                    0.0F,
                    blockEntity.chargeRemaining
                            - FULL_POWER_DRAIN_PER_TICK * throttle
            );
        }
        blockEntity.spawnWindParticles();
    }

    private void spawnWindParticles() {
        if (level == null || !level.isClientSide()) {
            return;
        }

        float output = Mth.clamp(
                getCurrentOutput(),
                0.0F,
                1.0F
        );

        if (output <= 0.001F || !hasLoadedCharge()) {
            return;
        }

        /*
         * Spawn less frequently at low thrust.
         */
        for (int i = 0; i < 5; i++) {
            if (level.random.nextFloat() > output) {
                return;
            }

            Direction facing =
                    getBlockState().getValue(GaleDriveBlock.FACING);

            Vec3 exhaustDirection =
                    Vec3.atLowerCornerOf(facing.getNormal());

            Vec3 center =
                    Vec3.atCenterOf(worldPosition);

            /*
             * Your plate center is at approximately Z=20 in model space,
             * or 1.25 blocks from the model origin.
             */
            Vec3 origin = center.add(exhaustDirection.scale(4*output).reverse()).add(exhaustDirection.scale(-1.5 + level.random.nextFloat()));

            spawnTrailParticle(
                    origin,
                    exhaustDirection,
                    output
            );
        }


    }

    private void spawnTrailParticle(
            Vec3 origin,
            Vec3 exhaustDirection,
            float output
    ) {
        Vec3 perpendicularA = createPerpendicular(exhaustDirection);

        Vec3 perpendicularB = exhaustDirection.cross(perpendicularA).normalize();

        double radius = Mth.lerp(0.8, 1.5, output);
        /*
         * Uniform-ish random point within a disk.
         */
        double angle = level.random.nextDouble() * Math.PI * 2.0;

        double distance = Math.sqrt(level.random.nextDouble()) * radius;

        Vec3 radialOffset = perpendicularA.scale(Math.cos(angle) * distance).add(perpendicularB.scale(Math.sin(angle) * distance));

        Vec3 position = origin.add(radialOffset);

        double speed = (MAX_AIRFLOW*output/60);

        Vec3 velocity = exhaustDirection;

        Direction direction = getBlockState().getValue(GaleDriveBlock.FACING);


//        level.addParticle(
//                new GustParticleData(randomRotationForDirection(direction)),
//                position.x,
//                position.y,
//                position.z,
//                -velocity.x * speed,
//                -velocity.y * speed,
//                -velocity.z * speed
//        );

        level.addParticle(
                new PropellerAirParticleData(),
                position.x,
                position.y,
                position.z,
                -velocity.x * speed,
                -velocity.y * speed,
                -velocity.z * speed
        );
    }

    private Quaternionf randomRotationForDirection(
            Direction direction
    ) {
        float randomRoll =
                level.random.nextFloat()
                        * Mth.TWO_PI;

        return new Quaternionf()
                .rotationTo(
                        0.0F,
                        -1.0F,
                        0.0F,
                        direction.getStepX(),
                        direction.getStepY(),
                        direction.getStepZ()
                )
                .rotateY(randomRoll);
    }

    private static Vec3 createPerpendicular(Vec3 direction) {
        Vec3 reference =
                Math.abs(direction.y) < 0.99
                        ? new Vec3(0.0, 1.0, 0.0)
                        : new Vec3(1.0, 0.0, 0.0);

        return direction
                .cross(reference)
                .normalize();
    }

    public static final int WIND_CHARGE_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;


    private final ItemStackHandler inventory =
            new ItemStackHandler(INVENTORY_SIZE) {

                @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return slot == WIND_CHARGE_SLOT
                            && stack.is(Items.WIND_CHARGE);
                }

                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                    sync();
                }
            };


    private void dischargeWindCharge(float throttle) {
        if (chargeRemaining <= 0.0F || throttle <= 0.0F) {
            return;
        }


        chargeRemaining -= FULL_POWER_DRAIN_PER_TICK * throttle;

        if (chargeRemaining <= 0.0F) {
            chargeRemaining = 0.0F;
        }

        setChanged();
    }

    private boolean tryLoadWindCharge() {
        if (chargeRemaining > 0.0F) {
            return true;
        }

        ItemStack extracted = inventory.extractItem(
                WIND_CHARGE_SLOT,
                1,
                false
        );

        if (extracted.isEmpty()) {
            return false;
        }

        chargeRemaining = CHARGE_CAPACITY;
        setChanged();
        sync();

        return true;
    }
    private float lastSyncedCharge = -1.0F;

    private void syncChargeWhenNeeded() {
        boolean changedEnough =
                Math.abs(chargeRemaining - lastSyncedCharge) >= 0.025F;

        boolean reachedBoundary =
                (chargeRemaining == 0.0F
                        || chargeRemaining == CHARGE_CAPACITY)
                        && chargeRemaining != lastSyncedCharge;

        if (!changedEnough && !reachedBoundary) {
            return;
        }

        lastSyncedCharge = chargeRemaining;
        setChanged();
        sync();
    }

    private float lastSyncedOutput;

    private void syncOutputWhenNeeded() {
        boolean changedEnough =
                Math.abs(currentOutput - lastSyncedOutput) >= 0.01F;

        boolean reachedTarget =
                currentOutput == targetOutput
                        && currentOutput != lastSyncedOutput;

        if (!changedEnough && !reachedTarget) {
            return;
        }

        lastSyncedOutput = currentOutput;
        setChanged();
        sync();
    }

    private void sync() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        level.sendBlockUpdated(
                worldPosition,
                state,
                state,
                Block.UPDATE_CLIENTS
        );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack getWindChargeStack() {
        return inventory.getStackInSlot(WIND_CHARGE_SLOT);
    }

    public boolean hasWindCharge() {
        return !getWindChargeStack().isEmpty();
    }

    public int getWindChargeCount() {
        return inventory
                .getStackInSlot(WIND_CHARGE_SLOT)
                .getCount();
    }

    public ItemStack consumeWindCharge() {
        return inventory.extractItem(
                WIND_CHARGE_SLOT,
                1,
                false
        );
    }


    public void updateRedstoneStrength() {
        if (level == null || level.isClientSide()) {
            return;
        }

        int newStrength = Mth.clamp(
                level.getBestNeighborSignal(worldPosition),
                0,
                15
        );

        if (newStrength == redstoneStrength) {
            return;
        }

        redstoneStrength = newStrength;
        targetOutput = redstoneStrength / 15.0F;

        setChanged();
    }

    public float getChargeRemaining() {
        return chargeRemaining;
    }

    public float getChargePercentage() {
        return Mth.clamp(
                chargeRemaining / CHARGE_CAPACITY,
                0.0F,
                1.0F
        );
    }

    public boolean hasLoadedCharge() {
        return chargeRemaining > 0.0F;
    }

    public int getRedstoneStrength() {
        return redstoneStrength;
    }

    public float getTargetOutput() {
        return targetOutput;
    }

    public float getCurrentOutput() {
        return currentOutput;
    }

    public float getRenderedOutput(float partialTick) {
        return Mth.lerp(
                partialTick,
                previousOutput,
                currentOutput
        );
    }



    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putInt("RedstoneStrength", redstoneStrength);
        tag.putFloat("CurrentOutput", currentOutput);
        tag.putFloat("TargetOutput", targetOutput);
        tag.putFloat("ChargeRemaining", chargeRemaining);
        tag.put(
                "Inventory",
                inventory.serializeNBT(registries)
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        float oldChargeRemaining = chargeRemaining;
        chargeRemaining = Mth.clamp(
                tag.getFloat("ChargeRemaining"),
                0.0F,
                CHARGE_CAPACITY
        );

        previousChargeRemaining = oldChargeRemaining;

        redstoneStrength = tag.getInt("RedstoneStrength");
        currentOutput = tag.getFloat("CurrentOutput");
        targetOutput = tag.getFloat("TargetOutput");
        chargeRemaining = Mth.clamp(
                tag.getFloat("ChargeRemaining"),
                0.0F,
                CHARGE_CAPACITY
        );

        previousOutput = currentOutput;

        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(
                    registries,
                    tag.getCompound("Inventory")
            );
        }
    }
}
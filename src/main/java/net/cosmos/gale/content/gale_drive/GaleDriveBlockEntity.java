package net.cosmos.gale.content.gale_drive;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;
import net.cosmos.gale.registry.ModBlockEntities;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class GaleDriveBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements IHaveGoggleInformation, BlockEntityPropeller, BlockEntitySubLevelPropellerActor {

    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR;
    private int redstoneStrength;

    // Outputs

    private float previousOutputThrottle;
    public float getRenderedOutput(float partialTick) { return Mth.lerp(partialTick, previousOutputThrottle, currentOutputThrottle); }

    private float currentOutputThrottle;
    public float getCurrentOutputThrottle() { return currentOutputThrottle; }

    private float targetOutputThrottle;
    public float getTargetOutputThrottle() { return targetOutputThrottle; }

    // Constants

    private static final float OUTPUT_CHANGE_PER_TICK = 0.05F;

    private static final double MAX_THRUST = 600.0;
    private static final double MAX_AIRFLOW = 60.0;
    private static final float CHARGE_CAPACITY = 1.0F;

    // Inventory

    public static final int WIND_CHARGE_SLOT = 0;
    public static final int INVENTORY_SIZE = 1;

    private final ItemStackHandler inventory = CreateInventory();
    private ItemStackHandler CreateInventory() {
        return new ItemStackHandler(INVENTORY_SIZE) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == WIND_CHARGE_SLOT && stack.is(Items.WIND_CHARGE);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                sync();
            }
        };
    }

    // Charge
    private static final float FULL_POWER_DRAIN_PER_TICK = CHARGE_CAPACITY / 200.0F;

    private float chargeRemaining;

    private float previousClientChargeRemaining; // Rendering only
    public float getRenderedChargeRemaining(float partialTick) {
        return Mth.lerp(partialTick, previousClientChargeRemaining, chargeRemaining);
    }

    // Setup

    public GaleDriveBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.GALE_DRIVE.get(), pos, state); }

    static { EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(true, false, Optional.of(1.22F), BuiltInRegistries.BLOCK.getTag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())); }

    // Accessors

    public Level level() {
        return this.level;
    }
    @Override public BlockEntityPropeller getPropeller() {
        return this;
    }

    // Sable thruster needs

    @Override public boolean isActive() {
        return currentOutputThrottle > 0.001F && hasLoadedCharge();
    }
    @Override public double getThrust() {
        return currentOutputThrottle * MAX_THRUST;
    }
    @Override public double getAirflow() {
        return currentOutputThrottle * MAX_AIRFLOW;
    }
    @Override public double getScaledThrust() {
        return currentOutputThrottle * MAX_THRUST;
    }

    // BlockState

    @Override public Direction getBlockDirection() {
        return getBlockState().getValue(GaleDriveBlock.FACING);
    }

    // Tooltips

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
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
                                Component.literal(formatNumber((float) getThrust(), "%.2f pN"))
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
                                Component.literal(formatNumber((float) getAirflow(), "%.2f m/s"))
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
    private static String formatNumber(float value, String formatter) {
        return String.format(Locale.ROOT, formatter, value);
    }

    // Lifecycle

    public static void serverTick(Level level, BlockPos pos, BlockState state, GaleDriveBlockEntity blockEntity) {
        blockEntity.previousClientChargeRemaining = blockEntity.chargeRemaining;

        // Set redstone level
        final int newStrength = Mth.clamp(level.getBestNeighborSignal(pos), 0, 15);
        if (newStrength != blockEntity.redstoneStrength) {
            blockEntity.redstoneStrength = newStrength;
            blockEntity.setChanged();
        }

        // Get Throttle
        final float throttle = blockEntity.redstoneStrength / 15.0F; // 0-1 throttle
        final boolean hasThrottle = blockEntity.redstoneStrength >= 1;
        boolean hasFuel = blockEntity.chargeRemaining > 0.001f;

        // Turn items into charge
        if (hasThrottle && !hasFuel) hasFuel = blockEntity.tryLoadWindCharge();

        // Do stuff with that throttle
        blockEntity.previousOutputThrottle = blockEntity.currentOutputThrottle;
        blockEntity.targetOutputThrottle = hasFuel ? throttle : 0.0F;
        blockEntity.currentOutputThrottle = Mth.approach(blockEntity.currentOutputThrottle, blockEntity.targetOutputThrottle, OUTPUT_CHANGE_PER_TICK);

        // Lower charge value
        if (hasThrottle && hasFuel) {
            blockEntity.chargeRemaining -= FULL_POWER_DRAIN_PER_TICK * throttle;
            blockEntity.chargeRemaining = Math.max(0.0f, blockEntity.chargeRemaining);
            blockEntity.setChanged();
        }

        blockEntity.syncOutputWhenNeeded();
        blockEntity.syncChargeWhenNeeded();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GaleDriveBlockEntity blockEntity) {
        blockEntity.previousOutputThrottle = blockEntity.currentOutputThrottle;
        blockEntity.currentOutputThrottle = Mth.approach(blockEntity.currentOutputThrottle, blockEntity.targetOutputThrottle, OUTPUT_CHANGE_PER_TICK);

        blockEntity.previousClientChargeRemaining = blockEntity.chargeRemaining;

        blockEntity.spawnWindParticles();
    }

    // Particles

    private void spawnWindParticles() {
        if (level == null || !level.isClientSide()) return;

        final float output = Mth.clamp(getCurrentOutputThrottle(), 0.0F, 1.0F);
        if (output <= 0.001F || !hasLoadedCharge()) return;

        for (int i = 0; i < 5; i++) {
            if (level.random.nextFloat() > output) return;

            final Direction facing = getBlockState().getValue(GaleDriveBlock.FACING);
            final Vec3 exhaustDirection = Vec3.atLowerCornerOf(facing.getNormal());
            final Vec3 center = Vec3.atCenterOf(worldPosition);

            final Vec3 origin = center.add(exhaustDirection.scale(4*output).reverse()).add(exhaustDirection.scale(-1.5 + level.random.nextFloat()));

            spawnTrailParticle(origin, exhaustDirection, output);
        }
    }

    private void spawnTrailParticle(Vec3 origin, Vec3 exhaustDirection, float output) {
        if (level == null || !level.isClientSide()) return;

        final Vec3 perpendicularA = createPerpendicular(exhaustDirection);
        final Vec3 perpendicularB = exhaustDirection.cross(perpendicularA).normalize();

        // Get random point in disk
        final double radius = Mth.lerp(0.8, 1.5, output);
        final double angle = level.random.nextDouble() * Math.PI * 2.0;
        final double distance = Math.sqrt(level.random.nextDouble()) * radius;

        final Vec3 radialOffset = perpendicularA.scale(Math.cos(angle) * distance).add(perpendicularB.scale(Math.sin(angle) * distance));
        final Vec3 position = origin.add(radialOffset);

        final double speed = (MAX_AIRFLOW * output/60);

        level.addParticle(
                new PropellerAirParticleData(),
                position.x,
                position.y,
                position.z,
                -exhaustDirection.x * speed,
                -exhaustDirection.y * speed,
                -exhaustDirection.z * speed
        );
    }

    /// Explode for when we are borked
    public void explode(Vec3 pos) {
        if (level == null) return;
        level.explode(
                null,
                null,
                EXPLOSION_DAMAGE_CALCULATOR,

                pos.x(),
                pos.y(),
                pos.z(),

                1.2F,
                false,

                Level.ExplosionInteraction.TRIGGER,
                ParticleTypes.GUST_EMITTER_SMALL,
                ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST
        );
    }

    private static Vec3 createPerpendicular(Vec3 direction) {
        Vec3 reference = Math.abs(direction.y) < 0.99 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        return direction.cross(reference).normalize();
    }

    /// Loads a wind charge from inventory to actual "charge" value.
    private boolean tryLoadWindCharge() {
        if (chargeRemaining >= 0.001f) return false;

        ItemStack extracted = inventory.extractItem(WIND_CHARGE_SLOT, 1, false);
        if (extracted.isEmpty()) return false;

        chargeRemaining += CHARGE_CAPACITY;
        setChanged();
        sync();

        return true;
    }

    // Syncing

    private float lastSyncedCharge = -1.0F;
    private void syncChargeWhenNeeded() {
        final boolean changedEnough = Math.abs(chargeRemaining - lastSyncedCharge) >= 0.025F;
        final boolean reachedBoundary = (chargeRemaining == 0.0F || chargeRemaining == CHARGE_CAPACITY) && chargeRemaining != lastSyncedCharge;

        if (!changedEnough && !reachedBoundary) return;

        lastSyncedCharge = chargeRemaining;
        setChanged();
        sync();
    }

    private float lastSyncedOutput;
    private void syncOutputWhenNeeded() {
        final boolean changedEnough = Math.abs(currentOutputThrottle - lastSyncedOutput) >= 0.01F;
        final boolean reachedTarget = currentOutputThrottle == targetOutputThrottle && currentOutputThrottle != lastSyncedOutput;

        if (!changedEnough && !reachedTarget) return;

        lastSyncedOutput = currentOutputThrottle;
        setChanged();
        sync();
    }

    private void sync() {
        if (level == null || level.isClientSide()) return;
        final BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }
    public int getWindChargeCount() {
        return inventory.getStackInSlot(WIND_CHARGE_SLOT).getCount();
    }

    public void updateRedstoneStrength() {
        if (level == null || level.isClientSide()) return;

        int newStrength = Mth.clamp(level.getBestNeighborSignal(worldPosition), 0, 15);
        if (newStrength == redstoneStrength) return;

        redstoneStrength = newStrength;
        targetOutputThrottle = redstoneStrength / 15.0F;

        setChanged();
    }

    public boolean hasLoadedCharge() {
        return chargeRemaining > 0.0F;
    }

    // Saving

    @Override
    public void onLoad() {
        super.onLoad();
        previousClientChargeRemaining = chargeRemaining;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putInt("RedstoneStrength", redstoneStrength);
        tag.putFloat("CurrentOutput", currentOutputThrottle);
        tag.putFloat("TargetOutput", targetOutputThrottle);
        tag.putFloat("ChargeRemaining", chargeRemaining);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        float oldChargeRemaining = chargeRemaining;
        chargeRemaining = Mth.clamp(tag.getFloat("ChargeRemaining"), 0.0F, CHARGE_CAPACITY);

        previousClientChargeRemaining = oldChargeRemaining;

        redstoneStrength = tag.getInt("RedstoneStrength");
        currentOutputThrottle = tag.getFloat("CurrentOutput");
        targetOutputThrottle = tag.getFloat("TargetOutput");
        chargeRemaining = Mth.clamp(tag.getFloat("ChargeRemaining"), 0.0F, CHARGE_CAPACITY);

        previousOutputThrottle = currentOutputThrottle;

        if (tag.contains("Inventory")) inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }
}
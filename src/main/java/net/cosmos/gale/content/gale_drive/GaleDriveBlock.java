package net.cosmos.gale.content.gale_drive;

import com.mojang.serialization.MapCodec;
import net.cosmos.gale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GaleDriveBlock extends BaseEntityBlock {

    public static final MapCodec<GaleDriveBlock> CODEC = simpleCodec(GaleDriveBlock::new);

    /**
     * The direction the front/petaled plate points.
     *
     * The Gale Drive's physical thrust will later be applied in the
     * opposite direction.
     */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public GaleDriveBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(POWERED, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(
                state,
                level,
                pos,
                neighborBlock,
                neighborPos,
                movedByPiston
        );

        if (level.isClientSide()) return;

        boolean powered = level.hasNeighborSignal(pos);

        if (state.getValue(POWERED) != powered) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof GaleDriveBlockEntity galeDrive) galeDrive.updateRedstoneStrength();
    }


    /**
     * Makes the front of the block face toward the player.
     *
     * Clicking a wall:
     *     the front faces toward the player.
     *
     * Clicking the floor:
     *     the front faces upward.
     *
     * Clicking the ceiling:
     *     the front faces downward.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();

        if (context.isSecondaryUseActive()) facing = facing.getOpposite();

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof GaleDriveBlockEntity galeDrive) {

            ItemStackHandler inventory = galeDrive.getInventory();

            List<ItemStack> drops = new ArrayList<>();

            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);

                if (!stack.isEmpty()) {
                    drops.add(stack.copy());
                }
            }

            boolean burst = galeDrive.hasLoadedCharge();

            if (burst) {
                galeDrive.explode(new Vec3(pos.getX()+0.5f,pos.getY()+0.5f,pos.getZ()+0.5f));
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    for (ItemStack stack : drops) {
                        popResource(serverLevel, pos, stack);
                    }
                });
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof GaleDriveBlockEntity galeDrive)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.is(Items.WIND_CHARGE)) {
            if (!level.isClientSide()) {
                ItemStack remainder =
                        galeDrive.getInventory().insertItem(
                                GaleDriveBlockEntity.WIND_CHARGE_SLOT,
                                stack.copy(),
                                false
                        );

                int insertedAmount =
                        stack.getCount() - remainder.getCount();

                if (insertedAmount > 0
                        && !player.getAbilities().instabuild) {
                    stack.shrink(insertedAmount);
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos)
                instanceof GaleDriveBlockEntity galeDrive)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ItemStack extracted =
                    galeDrive.getInventory().extractItem(
                            GaleDriveBlockEntity.WIND_CHARGE_SLOT,
                            1,
                            false
                    );

            if (!extracted.isEmpty()) {
                if (!player.addItem(extracted)) {
                    player.drop(extracted, false);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }




    /**
     * Keep this as MODEL when the stationary casing is rendered through
     * a normal block model.
     *
     * The spring, plate, petals, and wind charge can later be rendered
     * with a block-entity renderer.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Temporary full-block collision shape.
     *
     * We can replace this with direction-specific shapes after the
     * Blockbench model and its intended bounds are finalized.
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GaleDriveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return createTickerHelper(
                    blockEntityType,
                    ModBlockEntities.GALE_DRIVE.get(),
                    GaleDriveBlockEntity::clientTick
            );
        }

        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.GALE_DRIVE.get(),
                GaleDriveBlockEntity::serverTick
        );

    }


}
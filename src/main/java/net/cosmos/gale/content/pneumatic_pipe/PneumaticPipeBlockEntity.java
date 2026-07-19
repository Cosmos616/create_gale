package net.cosmos.gale.content.pneumatic_pipe;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.cosmos.gale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PneumaticPipeBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public PneumaticPipeBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PNEUMATIC_PIPE.get(), pos, state); }

}

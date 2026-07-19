package net.cosmos.gale.content.gale_drive;

import net.cosmos.gale.registry.ModSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class GaleDriveSoundInstance extends AbstractTickableSoundInstance {

    private final GaleDriveBlockEntity blockEntity;

    public GaleDriveSoundInstance(GaleDriveBlockEntity blockEntity) {
        super(ModSoundEvents.GALE_DRIVE_LOOP.get(), SoundSource.BLOCKS, RandomSource.create());

        this.blockEntity = blockEntity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 0.5F;
        this.attenuation = SoundInstance.Attenuation.LINEAR;

        this.x = blockEntity.getBlockPos().getX() + 0.5;
        this.y = blockEntity.getBlockPos().getY() + 0.5;
        this.z = blockEntity.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (blockEntity.isRemoved()) {
            stop();
            return;
        }

        float output = blockEntity.getCurrentOutputThrottle();
        boolean hasCharge = blockEntity.hasLoadedCharge();

        if (output <= 0.001F || !hasCharge) {
            // Fade out quickly when inactive
            volume = Mth.lerp(0.25F, volume, 0.0F);
            if (volume < 0.001F) stop();
        } else {
            // Ramp volume and pitch based on throttle
            float targetVolume = Mth.clamp(output * 0.9F, 0.1F, 0.9F);
            float targetPitch = Mth.lerp(0.5F, 1.6F, output);

            volume = Mth.lerp(0.15F, volume, targetVolume);
            pitch = Mth.lerp(0.15F, pitch, targetPitch);
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }
}

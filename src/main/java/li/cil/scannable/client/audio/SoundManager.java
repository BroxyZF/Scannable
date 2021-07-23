package li.cil.scannable.client.audio;

import li.cil.scannable.api.API;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public enum SoundManager {
    INSTANCE;

    private static final SoundEvent SCANNER_CHARGE = new SoundEvent(new ResourceLocation(API.MOD_ID, "scanner_charge"));
    private static final SoundEvent SCANNER_ACTIVATE = new SoundEvent(new ResourceLocation(API.MOD_ID, "scanner_activate"));

    @Nullable
    private SimpleSound currentChargingSound;

    public void playChargingSound() {
        currentChargingSound = SimpleSound.forUI(SCANNER_CHARGE, 1);
        Minecraft.getInstance().getSoundManager().play(currentChargingSound);
    }

    public void stopChargingSound() {
        if (currentChargingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(currentChargingSound);
            currentChargingSound = null;
        }
    }

    public void playActivateSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSound.forUI(SCANNER_ACTIVATE, 1));
    }
}

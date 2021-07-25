package li.cil.scannable.util;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TooltipUtils {
    public static void tryAddDescription(final ItemStack stack, final List<Component> tooltip) {
        if (stack.isEmpty()) {
            return;
        }

        final String translationKey = stack.getDescriptionId() + ".desc";
        final Language language = Language.getInstance();
        if (language.has(translationKey)) {
            final TranslatableComponent description = new TranslatableComponent(translationKey);
            tooltip.add(description.withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}

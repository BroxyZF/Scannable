package li.cil.scannable.common.item;

import li.cil.scannable.api.API;
import net.minecraft.item.Item;

import net.minecraft.item.Item.Properties;

public abstract class AbstractItem extends Item {
    protected AbstractItem(final Properties properties) {
        super(properties.tab(API.itemGroup));
    }

    protected AbstractItem() {
        this(new Properties());
    }
}

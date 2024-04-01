package com.cavetale.quidditch;

import com.cavetale.worldmarker.item.ItemMarker;
import java.util.List;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public enum QuidditchBallType {
    BLUDGER {
        // Chase players, 2 per game
        @Override public ItemStack createItemStack() {
            ItemStack result = new ItemStack(Material.SNOWBALL);
            result.editMeta(meta -> {
                    ItemMarker.setId(meta, getItemId());
                    tooltip(meta, List.of(text("Bludger", RED)));
                });
            return result;
        }
    },
    QUAFFLE {
        // Score goals, 1 per game
        @Override public ItemStack createItemStack() {
            ItemStack result = new ItemStack(Material.ENDER_PEARL);
            result.editMeta(meta -> {
                    ItemMarker.setId(meta, getItemId());
                    tooltip(meta, List.of(text("Quaffle", RED)));
                });
            return result;
        }
    },
    SNITCH {
        @Override public ItemStack createItemStack() {
            ItemStack result = new ItemStack(Material.EGG);
            result.editMeta(meta -> {
                    ItemMarker.setId(meta, getItemId());
                    tooltip(meta, List.of(text("Snitch", GOLD)));
                });
            return result;
        }
    },
    ;

    private final String itemId;

    QuidditchBallType() {
        this.itemId = "quidditch:" + name().toLowerCase();
    }

    public static QuidditchBallType ofKey(String key) {
        for (var it : values()) {
            if (it.itemId.equals(key)) return it;
        }
        return null;
    }

    public static QuidditchBallType ofItem(ItemStack item) {
        final String id = ItemMarker.getId(item);
        if (id == null) return null;
        return ofKey(id);
    }

    abstract ItemStack createItemStack();
}

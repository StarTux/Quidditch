package com.cavetale.quidditch;

import com.cavetale.mytems.util.BlockColor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
@RequiredArgsConstructor
public enum QuidditchHouse {
    GRYFFINDOR(text("Gryffindor", RED), RED, Color.RED, BlockColor.RED),
    RAVENCLAW(text("Ravenclaw", BLUE), BLUE, Color.BLUE, BlockColor.LIGHT_BLUE),
    HUFFLEPUFF(text("Hufflepuff", YELLOW), YELLOW, Color.YELLOW, BlockColor.YELLOW),
    SLYTHERIN(text("Slytherin", DARK_GREEN), DARK_GREEN, Color.GREEN, BlockColor.GREEN);

    private final Component displayName;
    private final TextColor textColor;
    private final Color bukkitColor;
    private final BlockColor blockColor;

    public ItemStack makeLeatherArmor(Material material) {
        ItemStack result = new ItemStack(material);
        result.editMeta(m -> {
                if (!(m instanceof LeatherArmorMeta meta)) return;
                meta.setColor(bukkitColor);
            });
        return result;
    }

    public static QuidditchHouse ofBlockColor(BlockColor blockColor) {
        for (var it : values()) {
            if (it.blockColor == blockColor) return it;
        }
        return null;
    }
}

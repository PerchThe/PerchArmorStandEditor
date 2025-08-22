/*
 * ArmorStandEditor: Bukkit plugin to allow editing armor stand attributes
 * Copyright (C) 2016-2023  RypoFalem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.github.rypofalem.armorstandeditor.menu;

import io.github.rypofalem.armorstandeditor.ArmorStandEditorPlugin;
import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.PlayerEditor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SizeMenu extends ASEHolder {

    public ArmorStandEditorPlugin plugin = ArmorStandEditorPlugin.instance();
    private Inventory menuInv;
    private Debug debug;
    private final PlayerEditor pe;
    private final ArmorStand as;
    static String name = "Size Menu";

    public SizeMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.as = as;
        this.debug = new Debug(pe.plugin);
        name = pe.plugin.getLang().getMessage("sizeMenu", "menutitle");
        // 6 rows (54): top filler, two centered rows (1..10), blank row, options row, bottom filler
        menuInv = Bukkit.createInventory(pe.getManager().getSizeMenuHolder(), 54, name);
    }

    // ----- Normalized titles (gold, not underlined) pulled via same path keys as createIcon -----
    private String normalizedTitleForPath(String path) {
        String raw = plugin.getLang().getMessage(path, "iconname", null);
        if (raw == null || raw.isBlank()) raw = defaultTitleForPath(path);
        return toGold(noUnderline(noColors(raw)));
    }

    private final String SCALE1        = normalizedTitleForPath("scale1");
    private final String SCALE2        = normalizedTitleForPath("scale2");
    private final String SCALE3        = normalizedTitleForPath("scale3");
    private final String SCALE4        = normalizedTitleForPath("scale4");
    private final String SCALE5        = normalizedTitleForPath("scale5");
    private final String SCALE6        = normalizedTitleForPath("scale6");
    private final String SCALE7        = normalizedTitleForPath("scale7");
    private final String SCALE8        = normalizedTitleForPath("scale8");
    private final String SCALE9        = normalizedTitleForPath("scale9");
    private final String SCALE10       = normalizedTitleForPath("scale10");
    private final String SCALEPLUS12   = normalizedTitleForPath("scaleadd12");    // +0.5
    private final String SCALEMINUS12  = normalizedTitleForPath("scaleremove12"); // -0.5
    private final String SCALEPLUS110  = normalizedTitleForPath("scaleadd110");   // +0.1
    private final String SCALEMINUS110 = normalizedTitleForPath("scaleremove110");// -0.1

    // Optional (not placed in layout, but handler supports them if added later)
    private final String BACKTOMENU    = normalizedTitleForPath("backtomenu");
    private final String RESET         = normalizedTitleForPath("reset");

    private void fillInventory() {
        menuInv.clear();

        // Filler: gray stained glass pane (no name/lore)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            fm.setLore(new ArrayList<>());
            fm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            filler.setItemMeta(fm);
        }

        // Build exact-scale items (1..10)
        ItemStack base10   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 1),  "scale1");
        ItemStack base20   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 2),  "scale2");
        ItemStack base30   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 3),  "scale3");
        ItemStack base40   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 4),  "scale4");
        ItemStack base50   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 5),  "scale5");
        ItemStack base60   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 6),  "scale6");
        ItemStack base70   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 7),  "scale7");
        ItemStack base80   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 8),  "scale8");
        ItemStack base90   = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 9),  "scale9");
        ItemStack base100  = createIcon(new ItemStack(Material.RED_STAINED_GLASS, 10), "scale10");

        // Deltas
        ItemStack add12toBase       = createIcon(new ItemStack(Material.ORANGE_STAINED_GLASS, 1), "scaleadd12");
        ItemStack remove12fromBase  = createIcon(new ItemStack(Material.GREEN_STAINED_GLASS, 1),  "scaleremove12");
        ItemStack add110fromBase    = createIcon(new ItemStack(Material.ORANGE_STAINED_GLASS, 2), "scaleadd110");
        ItemStack remove110fromBase = createIcon(new ItemStack(Material.GREEN_STAINED_GLASS, 2),  "scaleremove110");

        // Miniature (chibi) toggle — EGG, dynamic ON/OFF title & white lore
        String miniatureTitle = miniatureTitle(as.isSmall());
        ItemStack miniature = createIconCustom(
                new ItemStack(Material.EGG),
                "scalesmall",
                miniatureTitle,
                toWhite("Toggle small size")
        );

        // Back button at top-left
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            String backTitle = plugin.getLang().getMessage("backtomenu", "iconname");
            if (backTitle == null || backTitle.isBlank()) backTitle = "Back";
            bm.setDisplayName(toGold(noUnderline(noColors(backTitle))));
            ArrayList<String> lore = new ArrayList<>();
            String backDesc = plugin.getLang().getMessage("backtomenu.description", "icondescription");
            if (backDesc == null) backDesc = "";
            lore.add(toWhite(noUnderline(stripLeadingColorCodes(backDesc))));
            bm.setLore(lore);
            bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            bm.getPersistentDataContainer().set(ArmorStandEditorPlugin.instance().getIconKey(),
                    PersistentDataType.STRING, "ase back");
            back.setItemMeta(bm);
        }

        // Start with filler everywhere
        ItemStack[] items = new ItemStack[54];
        for (int i = 0; i < items.length; i++) items[i] = filler;

        // Row 1 (0..8): place Back at 0
        items[0] = back;

        // Rows 2–3 centered (exclude first 2 and last 2 columns -> use 11..15 and 20..24)
        items[11] = base10;
        items[12] = base20;
        items[13] = base30;
        items[14] = base40;
        items[15] = base50;

        items[20] = base60;
        items[21] = base70;
        items[22] = base80;
        items[23] = base90;
        items[24] = base100;

        // Row 4 (27..35): filler

        // Row 5 (36..44): deltas + miniature toggle in the center (40)
        items[36] = remove12fromBase;  // -0.5
        items[37] = add12toBase;       // +0.5
        items[40] = miniature;         // Miniature: ON/OFF (EGG)
        items[43] = remove110fromBase; // -0.1
        items[44] = add110fromBase;    // +0.1

        // Row 6 (45..53): filler

        menuInv.setContents(items);
    }

    private ItemStack createIcon(ItemStack icon, String path) {
        return createIcon(icon, path, null);
    }

    private ItemStack createIcon(ItemStack icon, String path, String option) {
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;

        // Command tag (use 'path' when option is null)
        meta.getPersistentDataContainer().set(
                ArmorStandEditorPlugin.instance().getIconKey(),
                PersistentDataType.STRING,
                "ase " + (option != null ? option : path)
        );

        // Name: gold, no underline; Lore: white (with fallbacks)
        String nameCol = normalizedTitleForPath(path);

        String rawDesc = plugin.getLang().getMessage(path + ".description", "icondescription", option);
        if (rawDesc == null) rawDesc = "";
        String descCol = toWhite(noUnderline(stripLeadingColorCodes(rawDesc)));

        meta.setDisplayName(nameCol);
        ArrayList<String> loreList = new ArrayList<>();
        if (!descCol.isBlank()) loreList.add(descCol);
        meta.setLore(loreList);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    // Explicit override for hardcoded title/lore (Miniature mode)
    private ItemStack createIconCustom(ItemStack icon, String command, String title, String lore) {
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;

        meta.getPersistentDataContainer().set(
                ArmorStandEditorPlugin.instance().getIconKey(),
                PersistentDataType.STRING,
                "ase " + command
        );

        meta.setDisplayName(title);
        ArrayList<String> loreList = new ArrayList<>();
        if (lore != null && !lore.isBlank()) loreList.add(lore);
        meta.setLore(loreList);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private String miniatureTitle(boolean isSmall) {
        return toGold("Miniature mode: " + (isSmall ? "ON" : "OFF"));
    }

    private String defaultTitleForPath(String path) {
        // scaleN -> "Scale N"
        Matcher m = Pattern.compile("^scale(\\d{1,2})$").matcher(path);
        if (m.find()) {
            return "Scale " + m.group(1);
        }
        return switch (path) {
            case "scaleadd12"     -> "+0.5";
            case "scaleremove12"  -> "-0.5";
            case "scaleadd110"    -> "+0.1";
            case "scaleremove110" -> "-0.1";
            case "scalesmall"     -> "Miniature mode";
            case "backtomenu"     -> "Back";
            case "reset"          -> "Reset";
            default               -> path;
        };
    }

    // ----- Formatting helpers (defined once) -----
    private String color(String s) { return s == null ? "" : s.replace('&', '§'); }
    private String toGold(String s) { return color("&6" + s); }
    private String toWhite(String s) { return color("&f" + s); }
    private String noUnderline(String s) { return s == null ? "" : s.replaceAll("(?i)[§&]n", ""); }
    private String noColors(String s) { return s == null ? "" : s.replaceAll("(?i)[§&][0-9A-FK-ORX]", ""); }
    private String stripLeadingColorCodes(String s) { return s == null ? "" : s.replaceFirst("(?i)^[§&][0-9A-FK-ORX]", ""); }

    // ----- Click handling -----
    public void handleAttributeScaling(String itemName, Player player) {
        if (itemName == null || player == null) return;

        Map<String, Double> positiveScaleMap = Map.ofEntries(
                Map.entry(SCALE1, 1.0),
                Map.entry(SCALE2, 2.0),
                Map.entry(SCALE3, 3.0),
                Map.entry(SCALE4, 4.0),
                Map.entry(SCALE5, 5.0),
                Map.entry(SCALE6, 6.0),
                Map.entry(SCALE7, 7.0),
                Map.entry(SCALE8, 8.0),
                Map.entry(SCALE9, 9.0),
                Map.entry(SCALE10, 10.0),
                Map.entry(SCALEPLUS12, 0.5),
                Map.entry(SCALEPLUS110, 0.1)
        );

        Map<String, Double> negativeScaleMap = Map.ofEntries(
                Map.entry(SCALEMINUS12, 0.5),
                Map.entry(SCALEMINUS110, 0.1)
        );

        if (positiveScaleMap.containsKey(itemName)) {
            handleScaleChange(player, itemName, positiveScaleMap.get(itemName));
            return;
        }
        if (negativeScaleMap.containsKey(itemName)) {
            handleScaleChange(player, itemName, negativeScaleMap.get(itemName));
            return;
        }

        // Miniature toggle — accept both ON and OFF versions
        String miniOn  = miniatureTitle(true);
        String miniOff = miniatureTitle(false);
        if (itemName.equals(miniOn) || itemName.equals(miniOff)) {
            if (!as.isValid() || !player.hasPermission("asedit.togglesize")) return;
            as.setSmall(!as.isSmall());  // toggle
            playChimeSound(player);
            player.closeInventory();
            return;
        }

        if (itemName.equals(BACKTOMENU)) {
            handleBackToMenu(player);
        } else if (itemName.equals(RESET)) {
            handleReset(player);
        }
    }

    private void handleScaleChange(Player player, String itemName, double scaleValue) {
        setArmorStandScale(player, itemName, scaleValue);
        playChimeSound(player);
        player.closeInventory();
    }

    private void handleBackToMenu(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
        player.closeInventory();
        pe.openMenu();
    }

    private void handleReset(Player player) {
        setArmorStandScale(player, RESET, 1);
        playChimeSound(player);
        player.closeInventory();
    }

    private void playChimeSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
    }

    private void setArmorStandScale(Player player, String itemName, double scaleValue) {
        debug.log("Setting the Scale of the ArmorStand");
        if (!as.isValid()) return;
        if (!player.hasPermission("asedit.togglesize")) return;

        double currentScaleValue;
        double newScaleValue;

        // Direct set to exact scale 1..10
        if (itemName.equals(SCALE1) || itemName.equals(SCALE2) || itemName.equals(SCALE3)
                || itemName.equals(SCALE4) || itemName.equals(SCALE5) || itemName.equals(SCALE6)
                || itemName.equals(SCALE7) || itemName.equals(SCALE8) || itemName.equals(SCALE9)
                || itemName.equals(SCALE10)) {

            currentScaleValue = 0.0;
            newScaleValue = currentScaleValue + scaleValue;

            debug.log("Result of the scale Calculation: " + newScaleValue);

            if (newScaleValue > plugin.getMaxScaleValue()) {
                pe.getPlayer().sendMessage(plugin.getLang().getMessage("scalemaxwarn", "warn"));
            } else if (newScaleValue < plugin.getMinScaleValue()) {
                pe.getPlayer().sendMessage(plugin.getLang().getMessage("scaleminwarn", "warn"));
            } else {
                as.getAttribute(Attribute.SCALE).setBaseValue(newScaleValue);
            }

            // Increment +0.5 / +0.1
        } else if (itemName.equals(SCALEPLUS12) || itemName.equals(SCALEPLUS110)) {
            currentScaleValue = as.getAttribute(Attribute.SCALE).getBaseValue();
            newScaleValue = currentScaleValue + scaleValue;

            debug.log("Result of the scale Calculation: " + newScaleValue);

            if (newScaleValue > plugin.getMaxScaleValue()) {
                pe.getPlayer().sendMessage(plugin.getLang().getMessage("scalemaxwarn", "warn"));
                return;
            }
            as.getAttribute(Attribute.SCALE).setBaseValue(newScaleValue);

            // Decrement -0.5 / -0.1
        } else if (itemName.equals(SCALEMINUS12) || itemName.equals(SCALEMINUS110)) {
            currentScaleValue = as.getAttribute(Attribute.SCALE).getBaseValue();
            newScaleValue = currentScaleValue - scaleValue;

            debug.log("Result of the scale Calculation: " + newScaleValue);

            if (newScaleValue < plugin.getMinScaleValue()) {
                pe.getPlayer().sendMessage(plugin.getLang().getMessage("scaleminwarn", "warn"));
                return;
            }
            as.getAttribute(Attribute.SCALE).setBaseValue(newScaleValue);

            // Reset to 1 (if you later place a reset button wired to RESET)
        } else if (itemName.equals(RESET)) {
            newScaleValue = 1.0;
            as.getAttribute(Attribute.SCALE).setBaseValue(newScaleValue);
        }
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.togglesize")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getDisplayName() + "' has opened the Sizing Attribute Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }
}

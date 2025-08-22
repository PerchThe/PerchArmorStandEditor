/*
 * armorstandeditor: Bukkit plugin to allow editing armor stand attributes
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
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;

public class PresetArmorPosesMenu {

    private Inventory menuInv;
    private Debug debug;
    private final PlayerEditor pe;
    public ArmorStandEditorPlugin plugin = ArmorStandEditorPlugin.instance();
    private final ArmorStand armorStand;
    static String name;

    public PresetArmorPosesMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.armorStand = as;
        this.debug = new Debug(pe.plugin);
        name = plugin.getLang().getMessage("presettitle", "menutitle");
        // 4 rows (36): top row (back + filler), two centered rows of presets (rows 3 & 4), no extras
        menuInv = Bukkit.createInventory(pe.getManager().getPresetHolder(), 36, name);
    }

    // ----- Normalized gold titles (no underline), used for both icons and comparisons -----
    private String normalizedTitleForPath(String path) {
        String raw = plugin.getLang().getMessage(path, "iconname");
        if (raw == null || raw.isBlank()) raw = defaultTitleForPath(path);
        return toGold(noUnderline(noColors(raw)));
    }

    // Preset names (match display names exactly)
    private final String SITTING     = normalizedTitleForPath("sitting");
    private final String WAVING      = normalizedTitleForPath("waving");
    private final String GREETING_1  = normalizedTitleForPath("greeting 1");
    private final String GREETING_2  = normalizedTitleForPath("greeting 2");
    private final String CHEERS      = normalizedTitleForPath("cheers");
    private final String ARCHER      = normalizedTitleForPath("archer");
    private final String DANCING     = normalizedTitleForPath("dancing");
    private final String HANGING     = normalizedTitleForPath("hanging");
    private final String PRESENTING  = normalizedTitleForPath("present");
    private final String FISHING     = normalizedTitleForPath("fishing");

    // Menu utility
    private final String BACKTOMENU  = normalizedTitleForPath("backtomenu");

    private void fillInventory() {
        menuInv.clear();

        // Filler: gray stained glass pane (blank)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            fm.setLore(new ArrayList<>());
            filler.setItemMeta(fm);
        }

        // Accurate/thematic icons for each preset
        ItemStack sitting   = createIcon(new ItemStack(Material.OAK_STAIRS),        "sitting");     // seat vibe
        ItemStack waving    = createIcon(new ItemStack(Material.LIME_BANNER),       "waving");      // waving banner
        ItemStack greet1    = createIcon(new ItemStack(Material.BELL),              "greeting 1");  // greeting bell
        ItemStack greet2    = createIcon(new ItemStack(Material.SUNFLOWER),         "greeting 2");  // friendly flower
        ItemStack cheer     = createIcon(new ItemStack(Material.HONEY_BOTTLE),      "cheers");      // drink (no potion meta)
        ItemStack archer    = createIcon(new ItemStack(Material.BOW),               "archer");      // bow
        ItemStack dancing   = createIcon(new ItemStack(Material.JUKEBOX),           "dancing");     // music/dance
        ItemStack hanging   = createIcon(new ItemStack(Material.CHAIN),             "hanging");     // chain
        ItemStack present   = createIcon(new ItemStack(Material.LECTERN),           "present");     // presenting/lectern
        ItemStack fishing   = createIcon(new ItemStack(Material.FISHING_ROD),       "fishing");     // fishing rod

        // Back button at absolute top-left, as a red stained glass pane
        ItemStack backToMenu = createIcon(new ItemStack(Material.RED_STAINED_GLASS_PANE), "backtomenu");

        // Fill all slots with filler first
        ItemStack[] items = new ItemStack[36];
        for (int i = 0; i < items.length; i++) items[i] = filler;

        // Top row (0..8): place Back at 0; rest remains filler
        items[0] = backToMenu;

        // Presets moved down one row vs. the old 2-row layout:
        // Now on rows 3 & 4 (still centered): slots 20..24 and 29..33

        // Row 3 (slots 18..26) -> use 20..24 for centering
        items[20] = sitting;
        items[21] = waving;
        items[22] = greet1;
        items[23] = greet2;
        items[24] = cheer;

        // Row 4 (slots 27..35) -> use 29..33 for centering
        items[29] = archer;
        items[30] = dancing;
        items[31] = hanging;
        items[32] = present;
        items[33] = fishing;

        menuInv.setContents(items);
    }

    private ItemStack createIcon(ItemStack icon, String path) {
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;

        // Command tag (optional, useful for future click routing)
        meta.getPersistentDataContainer().set(
                ArmorStandEditorPlugin.instance().getIconKey(),
                PersistentDataType.STRING,
                "ase preset " + path
        );

        // Title & lore with forced colors: &6 title, &f lore; strip underline/colors from lang
        String rawName = plugin.getLang().getMessage(path, "iconname");
        if (rawName == null || rawName.isBlank()) rawName = defaultTitleForPath(path);
        String nameCol = toGold(noUnderline(noColors(rawName)));

        String rawDesc = plugin.getLang().getMessage(path + ".description", "icondescription");
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

    private String defaultTitleForPath(String path) {
        if ("greeting 1".equals(path)) return "Greeting 1";
        if ("greeting 2".equals(path)) return "Greeting 2";
        return switch (path) {
            case "sitting"     -> "Sitting";
            case "waving"      -> "Waving";
            case "cheers"      -> "Cheers";
            case "archer"      -> "Archer";
            case "dancing"     -> "Dancing";
            case "hanging"     -> "Hanging";
            case "present"     -> "Presenting";
            case "fishing"     -> "Fishing";
            case "backtomenu"  -> "Back";
            default            -> capitalizeWords(path);
        };
    }

    private String capitalizeWords(String s) {
        if (s == null || s.isBlank()) return "";
        String[] parts = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.basic")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getDisplayName() + "' has opened the armorStand Preset Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    public static String getName() {
        return name;
    }

    public void handlePresetPose(String itemName, Player player) {
        if (itemName == null || player == null) return;

        debug.log("Player '" + player.getDisplayName() + "' chose preset pose '" + itemName + "'");

        if (itemName.equals(SITTING)) {
            setPresetPose(player, 345, 0, 10, 350, 0, 350, 280, 20, 0, 280, 340, 0, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(WAVING)) {
            setPresetPose(player, 220, 20, 0, 350, 0, 350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(GREETING_1)) {
            setPresetPose(player, 260, 20, 0, 260, 340, 0, 340, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(GREETING_2)) {
            setPresetPose(player, 260, 10, 0, 260, 350, 0, 320, 0, 0, 10, 0, 0, 340, 0, 350, 0, 0, 0);
        } else if (itemName.equals(ARCHER)) {
            setPresetPose(player, 270, 350, 0, 280, 50, 0, 340, 0, 10, 20, 0, 350, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(DANCING)) {
            setPresetPose(player, 14, 0, 110, 20, 0, 250, 250, 330, 0, 15, 330, 0, 350, 350, 0, 0, 0, 0);
        } else if (itemName.equals(CHEERS)) {
            setPresetPose(player, 250, 60, 0, 20, 10, 0, 10, 0, 0, 350, 0, 0, 340, 0, 0, 0, 0, 0);
        } else if (itemName.equals(HANGING)) {
            setPresetPose(player, 1, 33, 67, -145, -33, -4, -42, 21, 1, -100, 0, -1, -29, -38, -18, 0, -4, 0);
        } else if (itemName.equals(PRESENTING)) {
            setPresetPose(player, 280, 330, 0, 10, 0, 350, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(FISHING)) {
            setPresetPose(player, 300, 320, 0, 300, 40, 0, 280, 20, 0, 280, 340, 0, 0, 0, 0, 0, 0, 0);
        } else if (itemName.equals(BACKTOMENU)) {
            player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
            player.closeInventory();
            pe.openMenu();
            return;
        } else {
            return;
        }

        // play click & close for pose actions
        player.playSound(player.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 1, 1);
        player.closeInventory();
    }

    public void setPresetPose(Player player,
                              double rightArmRoll, double rightArmYaw, double rightArmPitch,
                              double leftArmRoll,  double leftArmYaw,  double leftArmPitch,
                              double rightLegRoll, double rightLegYaw, double rightLegPitch,
                              double leftLegRoll,  double leftLegYaw,  double leftLegPitch,
                              double headRoll,     double headYaw,     double headPitch,
                              double bodyRoll,     double bodyYaw,     double bodyPitch) {

        if (!armorStand.isValid()) return;
        if (!player.hasPermission("asedit.basic")) return;

        armorStand.setRightArmPose(new EulerAngle(
                Math.toRadians(rightArmRoll),
                Math.toRadians(rightArmYaw),
                Math.toRadians(rightArmPitch)
        ));

        armorStand.setLeftArmPose(new EulerAngle(
                Math.toRadians(leftArmRoll),
                Math.toRadians(leftArmYaw),
                Math.toRadians(leftArmPitch)
        ));

        armorStand.setRightLegPose(new EulerAngle(
                Math.toRadians(rightLegRoll),
                Math.toRadians(rightLegYaw),
                Math.toRadians(rightLegPitch)));

        armorStand.setLeftLegPose(new EulerAngle(
                Math.toRadians(leftLegRoll),
                Math.toRadians(leftLegYaw),
                Math.toRadians(leftLegPitch)));

        armorStand.setBodyPose(new EulerAngle(
                Math.toRadians(bodyRoll),
                Math.toRadians(bodyYaw),
                Math.toRadians(bodyPitch)));

        armorStand.setHeadPose(new EulerAngle(
                Math.toRadians(headRoll),
                Math.toRadians(headYaw),
                Math.toRadians(headPitch)));
    }

    // ----- color helpers (single set) -----
    private String color(String s) { return s == null ? "" : s.replace('&', '§'); }
    private String toGold(String s) { return color("&6" + s); }
    private String toWhite(String s) { return color("&f" + s); }
    private String noUnderline(String s) { return s == null ? "" : s.replaceAll("(?i)[§&]n", ""); }
    private String noColors(String s) { return s == null ? "" : s.replaceAll("(?i)[§&][0-9A-FK-ORX]", ""); }
    private String stripLeadingColorCodes(String s) { return s == null ? "" : s.replaceFirst("(?i)^[§&][0-9A-FK-ORX]", ""); }
}

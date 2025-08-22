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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;

public class Menu {
    private final Inventory menuInv;
    private final PlayerEditor pe;
    private static String name = "Armor Stand Editor Menu";
    private Debug debug;

    public Menu(PlayerEditor pe) {
        this.pe = pe;
        this.debug = new Debug(pe.plugin);
        name = pe.plugin.getLang().getMessage("mainmenutitle", "menutitle");
        menuInv = Bukkit.createInventory(pe.getManager().getMenuHolder(), 54, name);
        fillInventory();
    }

    private void fillInventory() {
        menuInv.clear();

        // -------- Filler (light gray panes, no name/lore) --------
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            fm.setLore(new ArrayList<>());
            filler.setItemMeta(fm);
        }

        // -------- Main icons --------
        // Axis (normal stained glass blocks)
        ItemStack xAxis = createIcon(new ItemStack(Material.RED_STAINED_GLASS),   "xaxis", "axis x");
        ItemStack yAxis = createIcon(new ItemStack(Material.GREEN_STAINED_GLASS), "yaxis", "axis y");
        ItemStack zAxis = createIcon(new ItemStack(Material.BLUE_STAINED_GLASS),  "zaxis", "axis z");

        // Movement speed
        ItemStack coarseAdj = createIcon(new ItemStack(Material.COARSE_DIRT),      "coarseadj", "adj coarse");
        ItemStack fineAdj   = createIcon(new ItemStack(Material.SMOOTH_SANDSTONE), "fineadj",   "adj fine");

        // Reset pose (lava bucket)
        ItemStack reset = createIcon(new ItemStack(Material.LAVA_BUCKET), "reset", "mode reset");

        // Body parts / modes
        ItemStack headPos     = createIcon(new ItemStack(Material.IRON_HELMET),     "head",     "mode head");
        ItemStack bodyPos     = createIcon(new ItemStack(Material.IRON_CHESTPLATE), "body",     "mode body");
        ItemStack leftLegPos  = createIcon(new ItemStack(Material.IRON_LEGGINGS),   "leftleg",  "mode leftleg");
        ItemStack rightLegPos = createIcon(new ItemStack(Material.IRON_LEGGINGS),   "rightleg", "mode rightleg");
        ItemStack leftArmPos  = createIcon(new ItemStack(Material.STICK),           "leftarm",  "mode leftarm");
        ItemStack rightArmPos = createIcon(new ItemStack(Material.STICK),           "rightarm", "mode rightarm");
        ItemStack showArms    = createIcon(new ItemStack(Material.STICK),           "showarms", "mode showarms");

        ItemStack presetItem  = createIcon(new ItemStack(Material.BOOKSHELF), "presetmenu", "mode preset");

        // Visibility: make a plain potion (no base type, no custom effects) so no effect text shows
        ItemStack visibility = null;
        if (pe.getPlayer().hasPermission("asedit.togglearmorstandvisibility") || pe.plugin.getArmorStandVisibility()) {
            ItemStack vis = new ItemStack(Material.POTION);
            PotionMeta pm = (PotionMeta) vis.getItemMeta();
            if (pm != null) {
                pm.clearCustomEffects();             // ensure there's no “No Effect” or effect lines
                vis.setItemMeta(pm);
            }
            visibility = createIcon(vis, "invisible", "mode invisible");
        }

        ItemStack toggleVulnerability = null;
        if (pe.getPlayer().hasPermission("asedit.toggleInvulnerability")) {
            toggleVulnerability = createIcon(new ItemStack(Material.TOTEM_OF_UNDYING), "vulnerability", "mode vulnerability");
        }

        ItemStack size = null;
        if (pe.getPlayer().hasPermission("asedit.togglesize")) {
            size = createIcon(new ItemStack(Material.PUFFERFISH), "size", "mode size");
        }

        ItemStack disableSlots = null;
        if (pe.getPlayer().hasPermission("asedit.disableslots")) {
            disableSlots = createIcon(new ItemStack(Material.BARRIER), "disableslots", "mode disableslots");
        }

        ItemStack plate = null;
        if (pe.getPlayer().hasPermission("asedit.togglebaseplate")) {
            plate = createIcon(new ItemStack(Material.SMOOTH_STONE_SLAB), "baseplate", "mode baseplate");
        }

        ItemStack place = null;
        if (pe.getPlayer().hasPermission("asedit.movement")) {
            place = createIcon(new ItemStack(Material.RAIL), "placement", "mode placement");
        }

        ItemStack rotate = null;
        if (pe.getPlayer().hasPermission("asedit.rotation")) {
            rotate = createIcon(new ItemStack(Material.COMPASS), "rotate", "mode rotate");
        }

        ItemStack equipment = null;
        if (pe.getPlayer().hasPermission("asedit.equipment")) {
            equipment = createIcon(new ItemStack(Material.CHEST), "equipment", "mode equipment");
        }

        // Copy / Paste / Slots (3)
        ItemStack copy, paste, slot1, slot2, slot3;
        if (pe.getPlayer().hasPermission("asedit.copy")) {
            copy  = createIcon(new ItemStack(Material.BUCKET),       "copy",    "mode copy");
            slot1 = createIcon(new ItemStack(Material.BOOK),         "copyslot","slot 1", "1");
            slot2 = createIcon(new ItemStack(Material.BOOK, 2),      "copyslot","slot 2", "2");
            slot3 = createIcon(new ItemStack(Material.BOOK, 3),      "copyslot","slot 3", "3");
        } else {
            copy = slot1 = slot2 = slot3 = filler; // keep column visually filled
        }
        if (pe.getPlayer().hasPermission("asedit.paste")) {
            paste = createIcon(new ItemStack(Material.WATER_BUCKET), "paste", "mode paste");
        } else {
            paste = filler;
        }

        ItemStack glowing = pe.getPlayer().hasPermission("asedit.togglearmorstandglow")
                ? createIcon(new ItemStack(Material.GLOW_INK_SAC), "armorstandglow", "mode armorstandglow")
                : filler;

        // -------- Build layout with filler everywhere first --------
        ItemStack[] items = new ItemStack[54];
        Arrays.fill(items, filler);

        // Left column: Copy → Paste → Slot1 → Slot2 → Slot3 → Preset
        items[0]  = copy;
        items[9]  = paste;
        items[18] = slot1;
        items[27] = slot2;
        items[36] = slot3;
        items[45] = presetItem;

        // Top row: axis centered; placement far right
        items[3] = xAxis;
        items[4] = yAxis;
        items[5] = zAxis;
        items[8] = place;

        // Row 2: head; rotate to the far right (under placement)
        items[13] = headPos;
        items[17] = rotate;

        // Row 3: arms/body; coarse to the far right (under rotate)
        items[21] = rightArmPos;
        items[22] = bodyPos;
        items[23] = leftArmPos;
        items[26] = coarseAdj;

        // Row 4: legs; fine to the far right (under coarse)
        items[30] = rightLegPos;
        items[32] = leftLegPos;
        items[35] = fineAdj;

        // Row 5: disable slots at the far right (under fine)
        items[44] = disableSlots;

        // Bottom row (46..53): glow, visibility, size, equipment (center), show arms, baseplate, invulnerability, reset
        items[46] = glowing;
        items[47] = (visibility != null ? visibility : filler);
        items[48] = (size != null ? size : filler);
        items[49] = (equipment != null ? equipment : filler);
        items[50] = showArms;
        items[51] = (plate != null ? plate : filler);
        items[52] = (toggleVulnerability != null ? toggleVulnerability : filler);
        items[53] = reset; // bottom-right danger

        menuInv.setContents(items);
    }

    private ItemStack createIcon(ItemStack icon, String path, String command) {
        return createIcon(icon, path, command, null);
    }

    private ItemStack createIcon(ItemStack icon, String path, String command, String option) {
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;

        // Attach command for click handling
        meta.getPersistentDataContainer().set(
                ArmorStandEditorPlugin.instance().getIconKey(),
                PersistentDataType.STRING,
                "ase " + command
        );

        // Title & lore: force &6 title, pure white lore; strip underline/legacy color codes from lang
        String rawName = getIconName(path, option);
        String rawDesc = getIconDescription(path, option);

        meta.setDisplayName(color("&6" + stripAllColors(rawName)));
        ArrayList<String> lore = new ArrayList<>();
        String desc = stripUnderline(rawDesc);
        lore.add(color("&f" + stripLeadingColor(desc)));
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private String getIconName(String path, String option) {
        return pe.plugin.getLang().getMessage(path, "iconname", option);
    }

    private String getIconDescription(String path, String option) {
        return pe.plugin.getLang().getMessage(path + ".description", "icondescription", option);
    }

    // ----- color helpers -----
    private String color(String s) {
        return s == null ? "" : s.replace('&', '§');
    }

    private String stripAllColors(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)[§&][0-9A-FK-ORX]", "");
    }

    private String stripUnderline(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)[§&]n", "");
    }

    private String stripLeadingColor(String s) {
        if (s == null) return "";
        return s.replaceFirst("(?i)^[§&][0-9A-FK-ORX]", "");
    }

    public void openMenu() {
        if (pe.getPlayer().hasPermission("asedit.basic")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getDisplayName() + "' has opened the Main ASE Menu");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    public static String getName() {
        return name;
    }
}

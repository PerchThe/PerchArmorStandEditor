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

import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.PlayerEditor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

public class EquipmentMenu {
    private Inventory menuInv;
    private Debug debug;
    private final PlayerEditor pe;
    private final ArmorStand armorstand;
    static String name = "ArmorStand Equipment";

    // --- Layout (45 slots) ---
    // Row 3 icons, centered: 19..22 (armor), 23 gap, 24..25 (hands)
    private static final int ICON_HELM   = 19;
    private static final int ICON_CHEST  = 20;
    private static final int ICON_PANTS  = 21;
    private static final int ICON_BOOTS  = 22;
    private static final int ICON_GAP    = 23;
    private static final int ICON_MAIN   = 24;
    private static final int ICON_OFF    = 25;

    // Row 4 inputs, centered: 28..31 (armor), 32 gap, 33..34 (hands)
    private static final int SLOT_HELMET   = 28;
    private static final int SLOT_CHEST    = 29;
    private static final int SLOT_PANTS    = 30;
    private static final int SLOT_BOOTS    = 31;
    private static final int SLOT_GAP      = 32;
    private static final int SLOT_MAINHAND = 33;
    private static final int SLOT_OFFHAND  = 34;

    public EquipmentMenu(PlayerEditor pe, ArmorStand as) {
        this.pe = pe;
        this.armorstand = as;
        this.debug = new Debug(pe.plugin);
        name = pe.plugin.getLang().getMessage("equiptitle", "menutitle");
        // 45 slots (5 rows)
        menuInv = Bukkit.createInventory(pe.getManager().getEquipmentHolder(), 45, name);
    }

    private void fillInventory() {
        menuInv.clear();

        // Snapshot current equipment
        EntityEquipment eq = armorstand.getEquipment();
        ItemStack curHelmet   = eq.getHelmet();
        ItemStack curChest    = eq.getChestplate();
        ItemStack curPants    = eq.getLeggings();
        ItemStack curBoots    = eq.getBoots();
        ItemStack curMainHand = eq.getItemInMainHand();
        ItemStack curOffHand  = eq.getItemInOffHand();

        // Preserve original behavior: clear stand while editing
        eq.clear();

        // Filler: gray pane, blank and tagged
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName(" ");
            fm.setLore(new ArrayList<>());
            fm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            fm.getPersistentDataContainer().set(pe.plugin.getIconKey(), PersistentDataType.STRING, "ase filler");
            filler.setItemMeta(fm);
        }

        // Back button at slot 0 (red stained glass)
        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            String backTitle = pe.plugin.getLang().getMessage("backtomenu", "iconname");
            if (backTitle == null || backTitle.isBlank()) backTitle = "Back";
            bm.setDisplayName(toGold(noUnderline(noColors(backTitle))));
            ArrayList<String> lore = new ArrayList<>();
            String backDesc = pe.plugin.getLang().getMessage("backtomenu.description", "icondescription");
            if (backDesc == null) backDesc = "";
            lore.add(toWhite(noUnderline(stripLeadingColorCodes(backDesc))));
            bm.setLore(lore);
            bm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            bm.getPersistentDataContainer().set(pe.plugin.getIconKey(), PersistentDataType.STRING, "ase back");
            back.setItemMeta(bm);
        }

        // Build base array with filler
        ItemStack[] items = new ItemStack[45];
        for (int i = 0; i < items.length; i++) items[i] = filler;

        // Row 1: back + filler
        items[0] = back;

        // Row 2: filler (9..17) — already filler

        // Row 3: icons centered with a gap between armor and hands
        items[ICON_HELM]  = createIcon(Material.LEATHER_HELMET,    "helm");
        items[ICON_CHEST] = createIcon(Material.LEATHER_CHESTPLATE, "chest");
        items[ICON_PANTS] = createIcon(Material.LEATHER_LEGGINGS,   "pants");
        items[ICON_BOOTS] = createIcon(Material.LEATHER_BOOTS,      "boots");
        // ICON_GAP stays filler
        items[ICON_MAIN]  = createIcon(Material.WOODEN_SWORD,       "rhand");
        items[ICON_OFF]   = createIcon(Material.SHIELD,             "lhand");

        // Row 4: input slots centered in same pattern
        items[SLOT_HELMET]   = curHelmet;
        items[SLOT_CHEST]    = curChest;
        items[SLOT_PANTS]    = curPants;
        items[SLOT_BOOTS]    = curBoots;
        // SLOT_GAP stays filler
        items[SLOT_MAINHAND] = curMainHand;
        items[SLOT_OFFHAND]  = curOffHand;

        // Row 5: filler (36..44) — already filler

        menuInv.setContents(items);
    }

    private ItemStack createIcon(Material mat, String slotKey) {
        ItemStack icon = new ItemStack(mat);
        ItemMeta meta = icon.getItemMeta();
        assert meta != null;

        // Title (gold, no underline), Lore (white)
        String rawName = pe.plugin.getLang().getMessage("equipslot", "iconname", slotKey);
        if (rawName == null || rawName.isBlank()) rawName = defaultTitleForSlot(slotKey);
        String nameCol = toGold(noUnderline(noColors(rawName)));

        String rawDesc = pe.plugin.getLang().getMessage("equipslot.description", "icondescription", slotKey);
        if (rawDesc == null) rawDesc = "";
        String descCol = toWhite(noUnderline(stripLeadingColorCodes(rawDesc)));

        meta.setDisplayName(nameCol);
        ArrayList<String> loreList = new ArrayList<>();
        if (!descCol.isBlank()) loreList.add(descCol);
        meta.setLore(loreList);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(pe.plugin.getIconKey(), PersistentDataType.STRING, "ase icon " + slotKey);
        icon.setItemMeta(meta);
        return icon;
    }

    private String defaultTitleForSlot(String slotKey) {
        return switch (slotKey) {
            case "helm"  -> "Helmet";
            case "chest" -> "Chestplate";
            case "pants" -> "Leggings";
            case "boots" -> "Boots";
            case "rhand" -> "Main Hand";
            case "lhand" -> "Off Hand";
            default      -> "Slot";
        };
    }

    public void openMenu() {
        pe.getPlayer().closeInventory();
        if (pe.getPlayer().hasPermission("asedit.equipment")) {
            fillInventory();
            debug.log("Player '" + pe.getPlayer().getDisplayName() + "' has opened the Equipment Menu (45).");
            pe.getPlayer().openInventory(menuInv);
        }
    }

    public void equipArmorstand() {
        ItemStack helmet   = menuInv.getItem(SLOT_HELMET);
        ItemStack chest    = menuInv.getItem(SLOT_CHEST);
        ItemStack pants    = menuInv.getItem(SLOT_PANTS);
        ItemStack boots    = menuInv.getItem(SLOT_BOOTS);
        ItemStack mainHand = menuInv.getItem(SLOT_MAINHAND);
        ItemStack offHand  = menuInv.getItem(SLOT_OFFHAND);

        debug.log("Equipping the ArmorStand with:");
        debug.log("Helmet: " + helmet);
        debug.log("Chest: " + chest);
        debug.log("Pants: " + pants);
        debug.log("Boots: " + boots);
        debug.log("Main: " + mainHand);
        debug.log("Off: " + offHand);

        EntityEquipment eq = armorstand.getEquipment();
        eq.setHelmet(helmet);
        eq.setChestplate(chest);
        eq.setLeggings(pants);
        eq.setBoots(boots);
        eq.setItemInMainHand(mainHand);
        eq.setItemInOffHand(offHand);
    }

    public static String getName() {
        return name;
    }

    // ---- formatting helpers (consistent with other menus) ----
    private String color(String s) { return s == null ? "" : s.replace('&', '§'); }
    private String toGold(String s) { return color("&6" + s); }
    private String toWhite(String s) { return color("&f" + s); }
    private String noUnderline(String s) { return s == null ? "" : s.replaceAll("(?i)[§&]n", ""); }
    private String noColors(String s) { return s == null ? "" : s.replaceAll("(?i)[§&][0-9A-FK-ORX]", ""); }
    private String stripLeadingColorCodes(String s) { return s == null ? "" : s.replaceFirst("(?i)^[§&][0-9A-FK-ORX]", ""); }
}

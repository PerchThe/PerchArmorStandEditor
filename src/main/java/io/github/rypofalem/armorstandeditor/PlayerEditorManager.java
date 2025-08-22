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

package io.github.rypofalem.armorstandeditor;

import com.google.common.collect.ImmutableList;

import io.github.rypofalem.armorstandeditor.Debug;
import io.github.rypofalem.armorstandeditor.api.ArmorStandRenameEvent;
import io.github.rypofalem.armorstandeditor.api.ItemFrameGlowEvent;
import io.github.rypofalem.armorstandeditor.menu.ASEHolder;
import io.github.rypofalem.armorstandeditor.protections.*;

import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// Manages PlayerEditors and Player Events related to editing armorstands
public class PlayerEditorManager implements Listener {
    private Debug debug;
    private ArmorStandEditorPlugin plugin;
    private HashMap<UUID, PlayerEditor> players;
    private ASEHolder menuHolder = new ASEHolder(); // Inventory holder that owns the main ase menu inventories for the plugin
    private ASEHolder equipmentHolder = new ASEHolder(); // Inventory holder that owns the equipment menu
    private ASEHolder presetHolder = new ASEHolder(); // Inventory Holder that owns the PresetArmorStand Post Menu
    private ASEHolder sizeMenuHolder = new ASEHolder(); // Inventory Holder that owns the PresetArmorStand Post Menu
    double coarseAdj;
    double fineAdj;
    double coarseMov;
    double fineMov;
    private boolean ignoreNextInteract = false;
    private TickCounter counter;
    private ArrayList<ArmorStand> as = null;
    private ArrayList<ItemFrame> itemF = null;
    private Integer noSize = 0;
    Team team;

    // Debounce to avoid double-processing between damage and swing paths
    private final HashMap<UUID, Long> lastEditTick = new HashMap<>();

    // Instantiate protections used to determine whether a player may edit an armor stand or item frame
    // NOTE: GriefPreventionProtection is Deprecated as of v1.19.3-40
    private final List<Protection> protections = ImmutableList.of(
            new GriefDefenderProtection(),
            new GriefPreventionProtection(),
            new LandsProtection(),
            new PlotSquaredProtection(),
            new SkyblockProtection(),
            new TownyProtection(),
            new WorldGuardProtection(),
            new itemAdderProtection(),
            new BentoBoxProtection()
    );

    PlayerEditorManager(ArmorStandEditorPlugin plugin) {
        this.plugin = plugin;
        this.debug = new Debug(plugin);
        players = new HashMap<>();
        coarseAdj = Util.FULL_CIRCLE / plugin.coarseRot;
        fineAdj = Util.FULL_CIRCLE / plugin.fineRot;
        coarseMov = 1;
        fineMov = .03125; // 1/32
        counter = new TickCounter();
        Scheduler.runTaskTimer(plugin, counter, 1, 1);
    }

    private boolean debounce(UUID id) {
        long now = getTime();
        Long last = lastEditTick.get(id);
        if (last != null && now - last <= 2) return true; // 2 ticks ~ 100ms
        lastEditTick.put(id, now);
        return false;
    }

    // Primary left-click path via damage event (works when damage events still fire)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) return;

        if (!((event.getEntity() instanceof ArmorStand) || event.getEntity() instanceof ItemFrame)) {
            // Left-clicked something else with the tool: open menu
            event.setCancelled(true);
            debug.log("Open Menu Called for Player: " + player.getDisplayName());
            getPlayerEditor(player.getUniqueId()).openMenu();
            return;
        }

        if (debounce(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof ArmorStand) {
            debug.log("Player '" + player.getDisplayName() + "' has LEFT clicked the ArmorStand");
            ArmorStand as = (ArmorStand) event.getEntity();
            getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
            event.setCancelled(true); // Never damage stands while editing
            if (canEdit(player, as)) applyLeftTool(player, as);
        } else if (event.getEntity() instanceof ItemFrame) {
            debug.log("Player '" + player.getDisplayName() + "' has LEFT clicked the ItemFrame");
            ItemFrame itemf = (ItemFrame) event.getEntity();
            getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
            event.setCancelled(true);
            if (canEdit(player, itemf)) applyLeftTool(player, itemf);
        }
    }

    // Paper users: you can add PrePlayerAttackEntityEvent here for even earlier interception.
    // We keep Spigot-compat by not importing Paper-only classes.

    // Swing-based fallback: fires for entity left-clicks even when no damage event is raised.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    void onArmSwing(PlayerAnimationEvent e) {
        // Only care about the main hand swing
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        final Player player = e.getPlayer();
        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) return;
        if (!player.hasPermission("asedit.basic")) return;
        if (plugin.enablePerWorld && (!plugin.allowedWorldList.contains(player.getWorld().getName()))) {
            getPlayerEditor(player.getUniqueId()).sendMessage("notincorrectworld", "warn");
            return;
        }

        // Avoid double-processing if the damage path will also catch this
        if (debounce(player.getUniqueId())) return;

        // Ray-trace like getTargets() to see what they're hitting
        ArrayList<ArmorStand> targets = getTargets(player);
        if (targets != null && !targets.isEmpty()) {
            ArmorStand target = targets.get(0);
            if (canEdit(player, target)) {
                debug.log("Arm swing fallback: applying LEFT tool to ArmorStand for " + player.getDisplayName());
                getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
                applyLeftTool(player, target);
            }
            return;
        }

        ArrayList<ItemFrame> frames = getFrameTargets(player);
        if (frames != null && !frames.isEmpty()) {
            ItemFrame frame = frames.get(0);
            if (canEdit(player, frame)) {
                debug.log("Arm swing fallback: applying LEFT tool to ItemFrame for " + player.getDisplayName());
                getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
                applyLeftTool(player, frame);
            }
        }
    }

    // Right-click (direct entity) path (pose reverse / menus / rename)
    @EventHandler(priority = EventPriority.LOWEST)
    void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (ignoreNextInteract) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!((event.getRightClicked() instanceof ArmorStand) || event.getRightClicked() instanceof ItemFrame)) return;

        if (event.getRightClicked() instanceof ArmorStand) {
            debug.log("Player '" + player.getDisplayName() + "' has RIGHT clicked on an ArmorStand");
            ArmorStand as = (ArmorStand) event.getRightClicked();

            if (!canEdit(player, as)) return;
            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
                event.setCancelled(true);
                applyRightTool(player, as);
                return;
            }

            // Attempt rename via Name Tag
            if (player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG && player.hasPermission("asedit.rename")) {
                ItemStack nameTag = player.getInventory().getItemInMainHand();
                String name;
                if (nameTag.getItemMeta() != null && nameTag.getItemMeta().hasDisplayName()) {
                    name = nameTag.getItemMeta().getDisplayName().replace('&', ChatColor.COLOR_CHAR);
                } else {
                    name = null;
                }

                // API: ArmorStandRenameEvent
                ArmorStandRenameEvent e = new ArmorStandRenameEvent(as, player, name);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                if (name == null) {
                    as.setCustomName(null);
                    as.setCustomNameVisible(false);
                    event.setCancelled(true);
                } else if (name.startsWith("" + ChatColor.COLOR_CHAR) && !player.hasPermission("asedit.rename.color")) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLang().getMessage("renamestopped"));
                } else if (!name.equals("")) { // Name tag is not blank
                    event.setCancelled(true);
                    // Delay 1 tick so vanilla rename doesn't overwrite formatting
                    Scheduler.runTaskLater(plugin, () -> {
                        as.setCustomName(name);
                        as.setCustomNameVisible(true);
                    }, 1);
                }
            }
        } else if (event.getRightClicked() instanceof ItemFrame) {
            ItemFrame itemFrame = (ItemFrame) event.getRightClicked();

            if (!canEdit(player, itemFrame)) return;
            if (plugin.isEditTool(player.getInventory().getItemInMainHand())) {
                getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
                if (!itemFrame.getItem().getType().equals(Material.AIR)) {
                    event.setCancelled(true);
                }
                applyRightTool(player, itemFrame);
                return;
            }

            // Glow item frame conversion (shift + glow ink sac)
            if (player.getInventory().getItemInMainHand().getType().equals(Material.GLOW_INK_SAC)
                    && player.hasPermission("asedit.basic")
                    && plugin.glowItemFrames && player.isSneaking()) {

                ItemFrameGlowEvent e = new ItemFrameGlowEvent(itemFrame, player);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                ItemStack glowSacs = player.getInventory().getItemInMainHand();
                ItemStack contents = null;
                Rotation rotation = null;
                if (itemFrame.getItem().getType() != Material.AIR) {
                    contents = itemFrame.getItem(); // save item
                    rotation = itemFrame.getRotation(); // save rotation
                }
                Location itemFrameLocation = itemFrame.getLocation();
                BlockFace facing = itemFrame.getFacing();

                if (player.getGameMode() != GameMode.CREATIVE) {
                    if (glowSacs.getAmount() > 1) {
                        glowSacs.setAmount(glowSacs.getAmount() - 1);
                    } else glowSacs = new ItemStack(Material.AIR);
                }

                itemFrame.remove();
                GlowItemFrame glowFrame = (GlowItemFrame) player.getWorld().spawnEntity(itemFrameLocation, EntityType.GLOW_ITEM_FRAME);
                glowFrame.setFacingDirection(facing);
                if (contents != null) {
                    glowFrame.setItem(contents);
                    glowFrame.setRotation(rotation);
                }
            }
        }
    }

    // Prevent creative players from breaking invulnerable stands (issue #309) and cleanup names
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    void onArmorStandBreak(EntityDamageByEntityEvent event) { // Fixes issue #309
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof ArmorStand)) return;

        if (event.getEntity() instanceof ArmorStand entityAS) {
            if (entityAS.isInvulnerable() && event.getDamager() instanceof Player p) {
                if (p.getGameMode() == GameMode.CREATIVE) {
                    p.sendMessage(plugin.getLang().getMessage("unabledestroycreative"));
                    event.setCancelled(true);
                }
            }
        }

        if (event.getEntity() instanceof ArmorStand entityAS && entityAS.isDead()) {
            // TEMP workaround for name lingering after destruction
            event.getEntity().setCustomName(null);
            event.getEntity().setCustomNameVisible(false);
            event.setCancelled(false);
        }
    }

    // Menu open with the tool: RIGHT clicks only (avoid stealing left-click edits)
    @EventHandler(priority = EventPriority.LOWEST)
    void onRightClickTool(PlayerInteractEvent e) {
        if (!(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
        debug.log("Ran on Right Click Tool Event.");
        Player player = e.getPlayer();
        if (!plugin.isEditTool(player.getInventory().getItemInMainHand())) return;
        if (plugin.requireSneaking && !player.isSneaking()) return;
        if (!player.hasPermission("asedit.basic")) return;
        if (plugin.enablePerWorld && (!plugin.allowedWorldList.contains(player.getWorld().getName()))) {
            // Implementation for Per World ASE
            getPlayerEditor(player.getUniqueId()).sendMessage("notincorrectworld", "warn");
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        debug.log("Open Menu Called for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).openMenu();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSwitchHands(PlayerSwapHandItemsEvent event) {
        debug.log("PlayerSwapHandItemsEvent trigger for Player: " + event.getPlayer().getDisplayName());
        if (!plugin.isEditTool(event.getOffHandItem())) return; // event assumes they are already switched
        event.setCancelled(true);
        Player player = event.getPlayer();

        as = getTargets(player);   // Get ArmorStands closest to player view
        itemF = getFrameTargets(player); // Get ItemFrames closest to player view

        // Check for null and empty lists
        if (as != null && itemF != null && !as.isEmpty() && !itemF.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).sendMessage("doubletarget", "warn");
        } else if (as != null && !as.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).setTarget(as);
        } else if (itemF != null && !itemF.isEmpty()) {
            getPlayerEditor(player.getUniqueId()).setFrameTarget(itemF);
        } else {
            getPlayerEditor(player.getUniqueId()).sendMessage("nodoubletarget", "warn");
        }
    }

    private ArrayList<ArmorStand> getTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        ArrayList<ArmorStand> armorStands = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return null;

        for (double i = 0; i < RANGE; i += STEPSIZE) {
            List<Entity> nearby = (List<Entity>) player.getWorld().getNearbyEntities(eyeLaser, LASERRADIUS, LASERRADIUS, LASERRADIUS);
            if (!nearby.isEmpty()) {
                boolean endLaser = false;
                for (Entity e : nearby) {
                    if (e instanceof ArmorStand stand) {
                        armorStands.add(stand);
                        endLaser = true;
                    }
                }
                if (endLaser) break;
            }
            if (eyeLaser.getBlock().getType().isSolid()) break;
            eyeLaser.add(STEP);
        }
        return armorStands;
    }

    private ArrayList<ItemFrame> getFrameTargets(Player player) {
        Location eyeLaser = player.getEyeLocation();
        Vector direction = player.getLocation().getDirection();
        ArrayList<ItemFrame> itemFrames = new ArrayList<>();

        double STEPSIZE = .5;
        Vector STEP = direction.multiply(STEPSIZE);
        double RANGE = 10;
        double LASERRADIUS = .3;
        List<Entity> nearbyEntities = player.getNearbyEntities(RANGE, RANGE, RANGE);
        if (nearbyEntities.isEmpty()) return null;

        for (double i = 0; i < RANGE; i += STEPSIZE) {
            List<Entity> nearby = (List<Entity>) player.getWorld().getNearbyEntities(eyeLaser, LASERRADIUS, LASERRADIUS, LASERRADIUS);
            if (!nearby.isEmpty()) {
                boolean endLaser = false;
                for (Entity e : nearby) {
                    if (e instanceof ItemFrame frame) {
                        itemFrames.add(frame);
                        endLaser = true;
                    }
                }
                if (endLaser) break;
            }
            if (eyeLaser.getBlock().getType().isSolid()) break;
            eyeLaser.add(STEP);
        }
        return itemFrames;
    }

    boolean canEdit(Player player, Entity entity) {
        // Get the Entity being checked for editing
        Block block = entity.getLocation().getBlock();

        // Check if all protections allow this edit, if one fails, don't allow edit
        return protections.stream().allMatch(protection -> protection.checkPermission(block, player));
    }

    void applyLeftTool(Player player, ArmorStand as) {
        debug.log("Applying Left Tool on ArmorStand for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
        getPlayerEditor(player.getUniqueId()).editArmorStand(as);
    }

    void applyLeftTool(Player player, ItemFrame itemf) {
        debug.log("Applying Left Tool on ItemFrame for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ItemFrame itemf) {
        debug.log("Applying Right Tool on ItemFrame for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
        getPlayerEditor(player.getUniqueId()).editItemFrame(itemf);
    }

    void applyRightTool(Player player, ArmorStand as) {
        debug.log("Applying Right Tool on ArmorStand for Player: " + player.getDisplayName());
        getPlayerEditor(player.getUniqueId()).cancelOpenMenu();
        getPlayerEditor(player.getUniqueId()).reverseEditArmorStand(as);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerMenuSelect(InventoryClickEvent e) {
        final InventoryHolder holder = PaperLib.getHolder(e.getInventory(), false).getHolder();

        if (holder == null) return;
        if (!(holder instanceof ASEHolder)) return;
        if (holder == menuHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                Player player = (Player) e.getWhoClicked();
                String command = item.getItemMeta().getPersistentDataContainer().get(plugin.getIconKey(), PersistentDataType.STRING);
                if (command == null || command.equals("ase ")) { // Therefore user has clicked a black pane
                    getPlayerEditor(player.getUniqueId()).sendMessage("blackGlassClick", "");
                    return;
                } else {
                    player.performCommand(command);
                    return;
                }
            }
        }
        if (holder == equipmentHolder) {
            ItemStack item = e.getCurrentItem();
            if (item == null) return;
            if (item.getItemMeta() == null) return;
            if (item.getItemMeta().getPersistentDataContainer().has(plugin.getIconKey(), PersistentDataType.STRING)) {
                e.setCancelled(true);
            }
        }

        if (holder == presetHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                Player player = (Player) e.getWhoClicked();
                String itemName = item.getItemMeta().getDisplayName();
                PlayerEditor pe = players.get(player.getUniqueId());
                pe.presetPoseMenu.handlePresetPose(itemName, player);
            }
        }

        if (holder == sizeMenuHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                Player player = (Player) e.getWhoClicked();
                String itemName = item.getItemMeta().getDisplayName();
                PlayerEditor pe = players.get(player.getUniqueId());
                pe.sizeModificationMenu.handleAttributeScaling(itemName, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onPlayerMenuClose(InventoryCloseEvent e) {
        final InventoryHolder holder = PaperLib.getHolder(e.getInventory(), false).getHolder();

        if (holder == null) return;
        if (!(holder instanceof ASEHolder)) return;
        if (holder == equipmentHolder) {
            PlayerEditor pe = players.get(e.getPlayer().getUniqueId());
            pe.equipMenu.equipArmorstand();

            // Remove the In Use Lock
            if (!Scheduler.isFolia()) {
                team = plugin.scoreboard.getTeam(plugin.inUseTeam);
                if (team != null) {
                    team.removeEntry(pe.armorStandInUseId.toString());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerLogOut(PlayerQuitEvent e) {
        removePlayerEditor(e.getPlayer().getUniqueId());
    }

    public PlayerEditor getPlayerEditor(UUID uuid) {
        return players.containsKey(uuid) ? players.get(uuid) : addPlayerEditor(uuid);
    }

    PlayerEditor addPlayerEditor(UUID uuid) {
        PlayerEditor pe = new PlayerEditor(uuid, plugin);
        players.put(uuid, pe);
        return pe;
    }

    private void removePlayerEditor(UUID uuid) {
        players.remove(uuid);
    }

    public ASEHolder getMenuHolder() {
        return menuHolder;
    }

    public ASEHolder getEquipmentHolder() {
        return equipmentHolder;
    }

    public ASEHolder getSizeMenuHolder() {
        return sizeMenuHolder;
    }

    public ASEHolder getPresetHolder() {
        return presetHolder;
    }

    long getTime() {
        return counter.ticks;
    }

    class TickCounter implements Runnable {
        long ticks = 0; // I am optimistic

        @Override
        public void run() {
            ticks++;
        }

        public long getTime() {
            return ticks;
        }
    }
}

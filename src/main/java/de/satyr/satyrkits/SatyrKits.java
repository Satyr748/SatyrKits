package de.satyr.satyrkits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SatyrKits extends JavaPlugin implements Listener {
    private static final String MAIN_TITLE = ChatColor.DARK_GRAY + "SatyrKits";
    private static final String CLAIM_TITLE = ChatColor.DARK_GRAY + "SatyrKits » Claim Kit";
    private static final String GIVE_TITLE = ChatColor.DARK_GRAY + "SatyrKits » Give Kit All";
    private static final String DELETE_TITLE = ChatColor.DARK_GRAY + "SatyrKits » Delete Kit";
    private final Map<UUID, Boolean> awaitingKitName = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("kit").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used in-game.");
                return true;
            }
            if (!player.isOp()) {
                player.sendMessage(color("&cYou must be OP to use SatyrKits."));
                return true;
            }
            if (args.length == 0) {
                openMainMenu(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
                saveKit(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                return true;
            }
            if (args[0].equalsIgnoreCase("claim") && args.length >= 2) {
                giveKit(player, args[1]);
                return true;
            }
            if (args[0].equalsIgnoreCase("giveall") && args.length >= 2) {
                giveKitToAll(args[1], player);
                return true;
            }
            player.sendMessage(color("&eUsage: /kit, /kit create <name>, /kit claim <name>, /kit giveall <name>"));
            return true;
        });
        getLogger().info("SatyrKits has been enabled.");
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        inv.setItem(10, menuItem(Material.WRITABLE_BOOK, "&aCreate Kit", "&7Save your current inventory as a kit."));
        inv.setItem(12, menuItem(Material.CHEST, "&eClaim Kit", "&7Choose a kit to receive."));
        inv.setItem(14, menuItem(Material.DIAMOND_SWORD, "&cGive Kit All", "&7Give a kit to every online player."));
        inv.setItem(16, menuItem(Material.BARRIER, "&4Delete Kit", "&7Permanently delete a saved kit."));
        player.openInventory(inv);
    }

    private void openKitMenu(Player player, boolean giveAll) {
        String title = giveAll ? GIVE_TITLE : CLAIM_TITLE;
        Inventory inv = Bukkit.createInventory(null, 54, title);
        ConfigurationSection section = getConfig().getConfigurationSection("kits");
        if (section != null) {
            int slot = 0;
            for (String kitName : section.getKeys(false)) {
                if (slot >= inv.getSize() - 1) break;
                inv.setItem(slot++, menuItem(Material.CHEST, "&b" + kitName, "&7Click to " + (giveAll ? "give this kit to everyone online." : "claim this kit.")));
            }
        }
        inv.setItem(53, menuItem(Material.ARROW, "&cBack", "&7Return to the main menu."));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(MAIN_TITLE) && !title.equals(CLAIM_TITLE) && !title.equals(GIVE_TITLE) && !title.equals(DELETE_TITLE)) return;
        event.setCancelled(true);
        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage(color("&cYou must be OP to use SatyrKits."));
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
        String display = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (title.equals(MAIN_TITLE)) {
            switch (display) {
                case "Create Kit" -> {
                    player.closeInventory();
                    awaitingKitName.put(player.getUniqueId(), true);
                    player.sendMessage(color("&aType the kit name in chat. Type &ccancel &ato cancel."));
                    player.sendMessage(color("&7Your current inventory and armor will be saved."));
                }
                case "Claim Kit" -> openKitMenu(player, false);
                case "Give Kit All" -> openKitMenu(player, true);
                case "Delete Kit" -> openDeleteMenu(player);
            }
            return;
        }
        if (display.equals("Back")) {
            openMainMenu(player);
            return;
        }
        if (title.equals(CLAIM_TITLE)) giveKit(player, display);
        else if (title.equals(GIVE_TITLE)) giveKitToAll(display, player);
        else if (title.equals(DELETE_TITLE)) deleteKit(player, display);
        player.closeInventory();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingKitName.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
        String name = event.getMessage().trim();
        awaitingKitName.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(this, () -> {
            if (name.equalsIgnoreCase("cancel")) {
                player.sendMessage(color("&cKit creation cancelled."));
                return;
            }
            if (name.isBlank() || name.length() > 32 || name.contains(".")) {
                player.sendMessage(color("&cInvalid name. Use 1-32 characters and no dots."));
                return;
            }
            saveKit(player, name);
        });
    }


    private void openDeleteMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, DELETE_TITLE);
        ConfigurationSection section = getConfig().getConfigurationSection("kits");
        if (section != null) {
            int slot = 0;
            for (String kitName : section.getKeys(false)) {
                if (slot >= inv.getSize() - 1) break;
                inv.setItem(slot++, menuItem(Material.BARRIER, "&c" + kitName, "&7Click to permanently delete this kit."));
            }
        }
        inv.setItem(53, menuItem(Material.ARROW, "&cBack", "&7Return to the main menu."));
        player.openInventory(inv);
    }

    private void deleteKit(Player player, String name) {
        if (!getConfig().isConfigurationSection("kits." + name)) {
            player.sendMessage(color("&cKit not found: &e" + name));
            return;
        }
        getConfig().set("kits." + name, null);
        saveConfig();
        player.sendMessage(color("&aKit &e" + name + " &ahas been deleted."));
    }

    private void saveKit(Player player, String rawName) {
        String name = rawName.trim();
        if (name.isEmpty() || name.length() > 32 || name.contains(".")) {
            player.sendMessage(color("&cInvalid kit name. Use 1-32 characters and no dots."));
            return;
        }
        getConfig().set("kits." + name + ".items", player.getInventory().getStorageContents());
        getConfig().set("kits." + name + ".armor", player.getInventory().getArmorContents());
        getConfig().set("kits." + name + ".offhand", player.getInventory().getItemInOffHand());
        saveConfig();
        player.sendMessage(color("&aKit &e" + name + " &asaved!"));
    }

    private void giveKit(Player player, String name) {
        ConfigurationSection section = getConfig().getConfigurationSection("kits." + name);
        if (section == null) {
            player.sendMessage(color("&cKit not found: &e" + name));
            return;
        }
        ItemStack[] contents = section.getList("items", new ArrayList<>()).toArray(new ItemStack[0]);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(contents);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        List<?> armorList = section.getList("armor");
        if (armorList != null) {
            ItemStack[] armor = armorList.stream().map(o -> o instanceof ItemStack item ? item : null).toArray(ItemStack[]::new);
            player.getInventory().setArmorContents(armor);
        }
        Object offhand = section.get("offhand");
        if (offhand instanceof ItemStack item) player.getInventory().setItemInOffHand(item.clone());
        player.sendMessage(color("&aYou received kit &e" + name + "&a!"));
    }

    private void giveKitToAll(String name, Player sender) {
        if (!getConfig().isConfigurationSection("kits." + name)) {
            sender.sendMessage(color("&cKit not found: &e" + name));
            return;
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            giveKit(online, name);
            count++;
        }
        sender.sendMessage(color("&aKit &e" + name + " &agiven to &e" + count + " &aonline players."));
    }

    private ItemStack menuItem(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(displayName));
        meta.setLore(Arrays.stream(lore).map(SatyrKits::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

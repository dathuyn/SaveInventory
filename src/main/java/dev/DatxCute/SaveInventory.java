package dev.DatxCute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveInventory extends JavaPlugin {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getConsoleSender().sendMessage(addcolor(config.getString("messages.enable")));
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(addcolor(config.getString("messages.disable")));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("save-inventory")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                saveInventory(player);
                player.sendMessage(addcolor(config.getString("messages.inventory_saved")));
                return true;
            } else {
                sender.sendMessage(addcolor(config.getString("messages.player_only_command")));
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("save-inventory-load")) {
            if (args.length != 1) {
                sender.sendMessage(addcolor(config.getString("messages.usage_save_inventory_load")));
                return false;
            }
            if (sender.hasPermission("saveinventory.load")) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    loadInventory(targetPlayer);
                    sender.sendMessage(addcolor(config.getString("messages.inventory_loaded").replace("{player}", targetPlayer.getName())));
                    targetPlayer.sendMessage(addcolor(config.getString("messages.inventory_loaded_target")));
                    return true;
                } else {
                    sender.sendMessage(addcolor(config.getString("messages.player_not_found")));
                    return false;
                }
            } else {
                sender.sendMessage(addcolor(config.getString("messages.no_permission")));
                return false;
            }
        }
        return false;
    }

    private void saveInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        List<ItemData> items = new ArrayList<>();

        for (ItemStack item : contents) {
            if (item != null) {
                items.add(new ItemData(item));
            } else {
                items.add(null);
            }
        }
        File configDir = getDataFolder();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File saveFile = new File(configDir, player.getName() + ".json");
        try (FileWriter writer = new FileWriter(saveFile)) {
            gson.toJson(items, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadInventory(Player player) {
        File saveFile = new File(getDataFolder(), player.getName() + ".json");
        if (saveFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(saveFile))) {
                ItemData[] items = gson.fromJson(reader, ItemData[].class);
                ItemStack[] contents = new ItemStack[items.length];

                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null) {
                        contents[i] = items[i].toItemStack();
                    } else {
                        contents[i] = null;
                    }
                }
                player.getInventory().setContents(contents);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        } else {
            player.sendMessage(addcolor(config.getString("messages.no_saved_inventory")));
        }
    }

    private static class ItemData {
        private final String type;
        private final int amount;
        private final String displayName;
        private final List<String> lore;
        private final Map<String, Integer> enchantments;

        public ItemData(ItemStack item) {
            this.type = item.getType().name();
            this.amount = item.getAmount();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                this.displayName = meta.hasDisplayName() ? meta.getDisplayName() : null;
                this.lore = meta.hasLore() ? meta.getLore() : null;
                if (meta.hasEnchants()) {
                    this.enchantments = new HashMap<>();
                    meta.getEnchants().forEach((enchant, level) ->
                            this.enchantments.put(enchant.getKey().getKey(), level));
                } else {
                    this.enchantments = null;
                }
            } else {
                this.displayName = null;
                this.lore = null;
                this.enchantments = null;
            }
        }

        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(Material.valueOf(this.type), this.amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (this.displayName != null) {
                    meta.setDisplayName(this.displayName);
                }
                if (this.lore != null) {
                    meta.setLore(this.lore);
                }
                if (this.enchantments != null) {
                    this.enchantments.forEach((enchant, level) -> {
                        Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchant));
                        if (enchantment != null) {
                            meta.addEnchant(enchantment, level, true);
                        }
                    });
                }
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    public static String addcolor(String a) {
        return ChatColor.translateAlternateColorCodes('&', a);
    }
}

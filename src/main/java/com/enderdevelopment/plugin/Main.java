package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin implements Listener {
    private final Map<UUID, Long> lastJoin = new HashMap<>();
    private final Map<UUID, Integer> streaks = new HashMap<>();
    private final Map<UUID, Long> lastKit = new HashMap<>();
    private NamespacedKey streakKey;
    private BukkitTask announcementTask;
    private int announcementIndex;
    private final String[] announcements = {
            "Bring a friend and build something unforgettable.",
            "Daily streaks grow your rewards. Come back tomorrow!",
            "Use /viral to find your next challenge."
    };

    @Override
    public void onEnable() {
        streakKey = new NamespacedKey(this, "login_streak");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ViralServer enabled. No proxy required.");
        announcementTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Component.text("✦ ", NamedTextColor.GOLD)
                        .append(Component.text(announcements[announcementIndex], NamedTextColor.YELLOW)));
            }
            announcementIndex = (announcementIndex + 1) % announcements.length;
        }, 20L * 60, 20L * 60 * 8);
    }

    @Override
    public void onDisable() {
        if (announcementTask != null) announcementTask.cancel();
        saveConfig();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long previous = getConfig().getLong("players." + player.getUniqueId() + ".lastJoin", 0L);
        int streak = getConfig().getInt("players." + player.getUniqueId() + ".streak", 0);
        if (previous == 0L || now - previous >= 48L * 60 * 60 * 1000) streak = 1;
        else if (now - previous >= 20L * 60 * 60 * 1000) streak++;
        getConfig().set("players." + player.getUniqueId() + ".lastJoin", now);
        getConfig().set("players." + player.getUniqueId() + ".streak", streak);
        saveConfig();
        streaks.put(player.getUniqueId(), streak);
        lastJoin.put(player.getUniqueId(), now);
        event.joinMessage(Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(player.getName(), NamedTextColor.AQUA))
                .append(Component.text(" joined the adventure! Streak: " + streak, NamedTextColor.YELLOW)));
        Bukkit.broadcast(Component.text("Welcome back, " + player.getName() + "!", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(Component.text(event.getPlayer().getName() + " left the adventure.", NamedTextColor.GRAY));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(Component.text(event.getEntity().getName() + " met their match. Who's next?", NamedTextColor.RED));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if (name.equals("viral")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("announce")) {
                if (!sender.hasPermission("viral.admin")) {
                    sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                    return true;
                }
                Bukkit.broadcast(Component.text("✦ Server event starting now! Invite your friends and join in.", NamedTextColor.GOLD));
                return true;
            }
            sender.sendMessage(Component.text("✦ VIRAL SERVER", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/spawn  - Return to the community spawn", NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/kit    - Claim your starter kit (once per day)", NamedTextColor.AQUA));
            sender.sendMessage(Component.text("/streak - Check your daily login streak", NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Bring friends. Build legends. Keep the streak alive.", NamedTextColor.YELLOW));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        switch (name) {
            case "spawn" -> teleportToSpawn(player);
            case "kit" -> giveKit(player);
            case "streak" -> player.sendMessage(Component.text("Your login streak: " + streaks.getOrDefault(player.getUniqueId(), getConfig().getInt("players." + player.getUniqueId() + ".streak", 0)) + " days.", NamedTextColor.GOLD));
            default -> { return false; }
        }
        return true;
    }

    private void teleportToSpawn(Player player) {
        Location spawn = getConfig().getLocation("spawn");
        if (spawn == null) {
            World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (world == null) {
                player.sendMessage(Component.text("No world is loaded yet.", NamedTextColor.RED));
                return;
            }
            spawn = world.getSpawnLocation();
        }
        player.teleport(spawn);
        player.sendMessage(Component.text("Back to the hub. Your next story starts here.", NamedTextColor.GREEN));
    }

    private void giveKit(Player player) {
        long now = System.currentTimeMillis();
        long last = getConfig().getLong("players." + player.getUniqueId() + ".lastKit", 0L);
        long cooldown = 24L * 60 * 60 * 1000;
        if (now - last < cooldown) {
            long remaining = cooldown - (now - last);
            player.sendMessage(Component.text("Your next kit is ready in " + remaining / 3600000 + "h " + (remaining / 60000) % 60 + "m.", NamedTextColor.RED));
            return;
        }
        giveTagged(player, Material.IRON_SWORD, 1, "Starter sword");
        giveTagged(player, Material.BREAD, 16, "Trail rations");
        giveTagged(player, Material.TORCH, 16, "Camp lights");
        getConfig().set("players." + player.getUniqueId() + ".lastKit", now);
        saveConfig();
        player.sendMessage(Component.text("Starter kit claimed. Make today count!", NamedTextColor.GREEN));
    }

    private void giveTagged(Player player, Material material, int amount, String title) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title, NamedTextColor.GOLD));
            meta.getPersistentDataContainer().set(streakKey, PersistentDataType.STRING, "viral-starter");
            item.setItemMeta(meta);
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }
}

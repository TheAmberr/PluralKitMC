package com.omnipico.pluralkitmc;
import com.omnipico.pluralkitmc.database.CacheManager;
import com.omnipico.pluralkitmc.database.KeyManager;

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.chat.Chat;
import org.apache.commons.lang3.concurrent.TimedSemaphore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PluralKitMC extends JavaPlugin {
    private BukkitAudiences adventure;
    Chat chat;
    PluralKitData data;
    ProxyListener proxyListener;
    DiscordSRV discord;
    TimedSemaphore apiSemaphore;
    boolean havePlaceholderAPI = false;
    private static PluralKitMC plugin;
    public static PluralKitMC getInstance() {return plugin;}
    private BukkitAudiences audiences;

    @Override
    public void onEnable() {
        plugin = this;
        this.saveDefaultConfig();
        apiSemaphore = new TimedSemaphore(1, TimeUnit.SECONDS, 1);
        KeyManager.setKey();
        CacheManager.connect();
        CacheManager.loadFromCache();

        // Soft deps
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") != null){
            chat = getServer().getServicesManager().load(Chat.class);
        } else { getLogger().warning("Vault plugin not found!"); }
        if (Bukkit.getServer().getPluginManager().getPlugin("DiscordSRV") != null){
            discord = DiscordSRV.getPlugin();
        } else { getLogger().warning("DiscordSRV plugin not found!"); }
        havePlaceholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (!havePlaceholderAPI) {
            getLogger().warning("PlaceholderAPI plugin not found!");
        }
        this.data = new PluralKitData(this);

        CommandPK commandPK = new CommandPK(data, this);
        Objects.requireNonNull(this.getCommand("pk")).setExecutor(commandPK);
        Objects.requireNonNull(this.getCommand("pk")).setTabCompleter(commandPK);
        audiences = BukkitAudiences.create(this);
        proxyListener = new ProxyListener(data, chat, discord, havePlaceholderAPI, audiences);
        proxyListener.setConfig(this.getConfig());
        getServer().getPluginManager().registerEvents(proxyListener, this);
        this.adventure = BukkitAudiences.create(this);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) this.adventure.close();
        CacheManager.disconnect();
    }

    public BukkitAudiences adventure() {
        return this.adventure;
    }

    public void reloadConfigData() {
        this.reloadConfig();
        FileConfiguration config = this.getConfig();
        proxyListener.setConfig(config);
        data.setConfig(config);
    }
}

package com.omnipico.pluralkitmc;


import github.scarsz.discordsrv.DiscordSRV;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ProxyListener implements Listener {
    //TODO: Make this a config option
    FileConfiguration config;
    String format;
    PluralKitData data;
    String defaultNameColor;
    Chat chat;
    DiscordSRV discord;
    boolean usePlaceholderAPI;
    BukkitAudiences audiences;
    MiniMessage miniMessage = MiniMessage.miniMessage();

    public ProxyListener(PluralKitData data, FileConfiguration config, Chat chat,
                         DiscordSRV discord, boolean usePlaceholderAPI, BukkitAudiences audiences) {
        this.data = data;
        this.chat = chat;
        this.discord = discord;
        this.usePlaceholderAPI = usePlaceholderAPI;
        this.audiences = audiences;
        setConfig(config);
    }


    public void setConfig(FileConfiguration config) {
        this.config = config;
        defaultNameColor = ChatUtils.replaceColor(config.getString("default_name_color","&b"));
        if (config.contains("message_format")) {
            format = Objects.requireNonNull(config.getString("message_format")).replace("%message%","%2$s");
        } else {
            format = "<white>[PK] <aqua>%member% <gray>> <white>%2$s";
        }
    }

    private BaseComponent[] getOutputComponent(BaseComponent[] resultComponents, Player player, PluralKitSystem system, String memberName) {
        return getOutputComponent(resultComponents, player, system, memberName, false);
    }

    private BaseComponent[] getOutputComponent(BaseComponent[] resultComponents, Player player, PluralKitSystem system, String memberName, Boolean logUsername) {
        ArrayList<BaseComponent> components = new ArrayList<>();
        boolean convertedMember = false;
        if (logUsername) {
            components.add(new TextComponent("(" + player.getName() + ") "));
        }
        for (BaseComponent component : resultComponents) {
            if (!convertedMember && component.toPlainText().startsWith("%member%")) {
                convertedMember = true;
                ComponentBuilder hoverTextBuilder = new ComponentBuilder("User: ").color(ChatColor.GREEN)
                        .append(TextComponent.fromLegacyText(player.getDisplayName())).append("\nSystem: ").color(ChatColor.GREEN);
                if (system.getName() != null && system.getName().length() > 0) {
                    hoverTextBuilder.append(system.getName()).color(ChatColor.AQUA);
                } else {
                    hoverTextBuilder.append(system.getId()).color(ChatColor.GRAY);
                }
                Text hoverText = new Text(hoverTextBuilder.create());
                components.addAll(Arrays.asList(new ComponentBuilder(memberName)
                        .color(component.getColor())
                        .bold(component.isBold())
                        .italic(component.isItalic())
                        .obfuscated(component.isObfuscated())
                        .strikethrough(component.isStrikethrough())
                        .underlined(component.isUnderlined())
                        .font(component.getFont())
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                hoverText
                        ))
                        .create()));
                if (component.toPlainText().length() > 8) {
                    BaseComponent[] post = new ComponentBuilder(component.toPlainText().substring(8))
                            .color(component.getColor())
                            .bold(component.isBold())
                            .italic(component.isItalic())
                            .obfuscated(component.isObfuscated())
                            .strikethrough(component.isStrikethrough())
                            .underlined(component.isUnderlined())
                            .font(component.getFont())
                            .create();
                    components.addAll(Arrays.asList(post));
                }
            } else {
                components.add(component);
            }
        }
        return components.toArray(new BaseComponent[0]);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalFormat = event.getFormat();
        String message = event.getMessage();
        PluralKitSystem system = data.getSystem(player.getUniqueId());
        PluralKitMember proxiedMember = data.getProxiedUser(player.getUniqueId(), message.toLowerCase());
        PluralKitProxy pluralKitProxy = data.getProxy(player.getUniqueId(), message.toLowerCase());

        if (proxiedMember != null && system != null) {
            String systemTag = data.getSystemTag(player.getUniqueId());
            String memberName = (proxiedMember.display_name != null && !proxiedMember.display_name.isEmpty()) ? proxiedMember.display_name : proxiedMember.name;
            String fullMemberName = (systemTag != null && !systemTag.isEmpty()) ? memberName + " " + systemTag : memberName;

            int prefixLength = pluralKitProxy.prefix != null ? pluralKitProxy.prefix.length() : 0;
            int suffixLength = pluralKitProxy.suffix != null ? pluralKitProxy.suffix.length() : 0;
            message = message.substring(prefixLength, message.length() - suffixLength);
            event.setMessage(message);

            String ourFormat = format;
            String colorTag;
            if (proxiedMember.color != null && !proxiedMember.color.isEmpty()) {
                colorTag = "<#" + proxiedMember.color + ">";
            } else {
                colorTag = "<aqua>";
            }
            ourFormat = ourFormat.replace("%member%", colorTag + "%member%");

            String prefix = chat != null ? ChatUtils.replaceColor(chat.getPlayerPrefix(player)) : "";
            String suffix = chat != null ? ChatUtils.replaceColor(chat.getPlayerSuffix(player)) : "";

            ourFormat = ourFormat.replace("%prefix%", prefix).replace("%suffix%", suffix);

            if (!config.getBoolean("hover_text", false)) {
                ourFormat = ourFormat.replace("%member%", fullMemberName);
            }

            if (usePlaceholderAPI) {
                ourFormat = PlaceholderAPI.setPlaceholders(player, ourFormat);
            }

            String finalFormatted = String.format(ourFormat.replace("%", "%%").replace("%%2$s", "%2$s"), player.getDisplayName(), message);
            finalFormatted = finalFormatted.replace("%member%", fullMemberName);
            Component parsed = miniMessage.deserialize(finalFormatted);

            for (Player p : event.getRecipients()) {
                audiences.player(p).sendMessage(parsed);
            }

            if (discord != null && config.getBoolean("discordsrv_compatibility", true)) {
                if (config.getBoolean("discordsrv_use_member_names", true)) {
                    String oldDisplayName = player.getDisplayName();
                    player.setDisplayName(fullMemberName);
                    discord.processChatMessage(player, message, "global", false);
                    player.setDisplayName(oldDisplayName);
                } else {
                    discord.processChatMessage(player, message, "global", false);
                }
            }

            if (config.getBoolean("keep_original_message_event", false)) {
                try {
                    event.setFormat(originalFormat);
                    event.getRecipients().clear();
                } catch (UnsupportedOperationException e) {
                    event.setCancelled(true);
                }
            } else {
                audiences.console().sendMessage(parsed);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        data.getCacheOrCreate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        data.clearCache(event.getPlayer().getUniqueId());
    }

}
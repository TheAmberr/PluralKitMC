package com.omnipico.pluralkitmc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CommandPK implements CommandExecutor, TabCompleter {
    PluralKitData data;
    PluralKitMC plugin;

    public CommandPK(PluralKitData data, PluralKitMC plugin) {
        this.data = data;
        this.plugin = plugin;
    }

    public List<String> getMemberList(CommandSender sender) {
        List<String> memberNames = new ArrayList<>();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            List<PluralKitMember> members = data.getMembers(player.getUniqueId());
            for (PluralKitMember member : members) {
                memberNames.add(member.name);
            }
        }
        return memberNames;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length >= 1) {
                String arg0 = args[0].toLowerCase();
                switch (arg0) {
                    case "help":
                    case "h":
                        plugin.adventure().player(player).sendMessage(ChatUtils.helpMessage);
                        break;
                    case "update":
                    case "u":
                        if (player.hasPermission("pluralkitmc.update") || player.hasPermission("pluralkitmc.*") || player.hasPermission("*")) {
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                data.updateCache(player.getUniqueId(), true);
                            });
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Your system is being updated and will be active momentarily.").color(NamedTextColor.GREEN)
                            ));
                        } else {
                            plugin.adventure().player(player).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                        }
                        break;
                    case "load":
                    case "l":
                        if (args.length == 2) {
                            if (args[1].length() == 5 || args[1].length() == 6) {
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    data.setSystemId(player.getUniqueId(), args[1].toLowerCase());
                                });
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Set system id to ", NamedTextColor.GREEN)
                                ).append(
                                        Component.text(args[1], NamedTextColor.AQUA)
                                ).append(
                                        Component.text(", it will be active momentarily", NamedTextColor.GREEN)
                                ));
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" System ids must be 5/6 characters long.", NamedTextColor.RED)
                                ));
                            }
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Usage: /pk load <system id>", NamedTextColor.RED)
                            ));
                        }
                        break;
                    case "link":
                    case "token":
                    case "t":
                        if (player.hasPermission("pluralkitmc.update") || player.hasPermission("pluralkitmc.*") || player.hasPermission("*")) {
                            if (args.length == 2) {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Linking pluralkit...", NamedTextColor.GREEN)
                                ));
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    if (data.setToken(player.getUniqueId(), args[1])) {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                                Component.text(" Your pluralkit has been linked.", NamedTextColor.GREEN)
                                        ));
                                    } else {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                                Component.text(" Your token was invalid, try refreshing it with pk;token refresh.", NamedTextColor.RED)
                                        ));
                                    }
                                });
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Usage: /pk link <token from pk;token>", NamedTextColor.RED)
                                ));
                            }
                        } else {
                            plugin.adventure().player(player).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                        }
                        break;
                    case "system":
                    case "s":
                        if (args.length >= 2) {
                            String arg1 = args[1].toLowerCase();
                            if (arg1.equals("list") || arg1.equals("l")) {
                                if (args.length == 3 && (args[2].toLowerCase().equals("full") || args[2].toLowerCase().equals("f"))) {
                                    PluralKitSystem system = data.getSystem(player.getUniqueId());
                                    List<PluralKitMember> members = data.getMembers(player.getUniqueId());
                                    if (system != null && members.size() > 0) {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.displayMemberList(members, system));
                                    } else {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                                Component.text(" System/Members not found :(", NamedTextColor.RED)
                                        ));
                                    }
                                } else {
                                    PluralKitSystem system = data.getSystem(player.getUniqueId());
                                    List<PluralKitMember> members = data.getMembers(player.getUniqueId());
                                    if (system != null && members.size() > 0) {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.displayMemberList(members, system));
                                    } else {
                                        plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                                Component.text(" System/Members not found :(", NamedTextColor.RED)
                                        ));
                                    }
                                }
                            } else if (arg1.equals("f")) {
                                // TODO: Show fronter
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Usage: /pk system [list] [full]", NamedTextColor.RED)
                                ));
                            }
                        } else {
                            PluralKitSystem system = data.getSystem(player.getUniqueId());
                            if (system != null) {
                                plugin.adventure().player(player).sendMessage(ChatUtils.displaySystemInfo(system));
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" System not found :(", NamedTextColor.RED)
                                ));
                            }
                        }
                        break;
                    case "find":
                    case "f":
                        if (args.length == 2) {
                            PluralKitSystem system = data.getSystem(player.getUniqueId());
                            List<PluralKitMember> members = data.searchMembers(player.getUniqueId(), args[1]);
                            if (system != null && members.size() > 0) {
                                plugin.adventure().player(player).sendMessage(ChatUtils.displayMemberSearch(members, system, args[1]));
                            } else if (system != null) {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" No members found :(", NamedTextColor.RED)
                                ));
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Your system is not currently loaded!", NamedTextColor.RED)
                                ));
                            }
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Usage: /pk find <search term>", NamedTextColor.RED)
                            ));
                        }
                        break;
                    case "autoproxy":
                    case "ap":
                        if (args.length == 2) {
                            String apMode = args[1].toLowerCase();
                            if (apMode.equals("off") || apMode.equals("front") || apMode.equals("latch")) {
                                data.updateAutoProxyMode(player.getUniqueId(), apMode);
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Auto proxy mode set to ", NamedTextColor.GREEN)
                                ).append(
                                        Component.text(apMode, NamedTextColor.AQUA)
                                ));
                            } else {
                                apMode = "member";
                                data.updateAutoProxyMode(player.getUniqueId(), apMode);
                                if (data.updateAutoProxyMember(player.getUniqueId(), args[1].toLowerCase())) {
                                    plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                            Component.text(" Auto proxy set to ", NamedTextColor.GREEN)
                                    ).append(
                                            Component.text(args[1], NamedTextColor.AQUA)
                                    ));
                                } else {
                                    plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                            Component.text(" Could not find system member ", NamedTextColor.RED)
                                    ).append(
                                            Component.text(apMode, NamedTextColor.AQUA)
                                    ));
                                }
                            }
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Usage: /pk autoproxy <off/front/latch/member>", NamedTextColor.RED)
                            ));
                        }
                        break;
                    case "member":
                    case "m":
                        if (args.length == 2) {
                            PluralKitSystem system = data.getSystem(player.getUniqueId());
                            PluralKitMember member = data.getMemberByName(player.getUniqueId(), args[1]);
                            if (system != null && member != null) {
                                plugin.adventure().player(player).sendMessage(ChatUtils.displayMemberInfo(member, system));
                            } else {
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Member not found :(", NamedTextColor.RED)
                                ));
                            }
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Usage: /pk member <member>", NamedTextColor.RED)
                            ));
                        }
                        break;
                    case "random":
                    case "r":
                        PluralKitSystem system = data.getSystem(player.getUniqueId());
                        PluralKitMember member = data.getRandomMember(player.getUniqueId());
                        if (system != null && member != null) {
                            plugin.adventure().player(player).sendMessage(ChatUtils.displayMemberInfo(member, system));
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Could not find any members :(", NamedTextColor.RED)
                            ));
                        }
                        break;
                    case "reload":
                        if (player.hasPermission("pluralkitmc.reload") || player.hasPermission("pluralkitmc.*") || player.hasPermission("*")) {
                            plugin.reloadConfigData();
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Config reloaded.", NamedTextColor.GREEN)
                            ));
                        } else {
                            plugin.adventure().player(player).sendMessage(Component.text("You do not have permission for this command.").color(NamedTextColor.RED));
                        }
                        break;
                    case "switch":
                    case "sw":
                        if (args.length >= 2) {
                            List<String> newFronters = new ArrayList<>();
                            for (int i = 1; i < args.length; i++) {
                                String memberName = args[i].toLowerCase();
                                if (!memberName.equals("out")) {
                                    newFronters.add(memberName);
                                }
                            }
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                data.updateFronters(player.getUniqueId(), newFronters);
                                plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                        Component.text(" Updated fronters.", NamedTextColor.GREEN)
                                ));
                            });
                        } else {
                            plugin.adventure().player(player).sendMessage(ChatUtils.pluginTag.append(
                                    Component.text(" Usage: /pk switch [out/member...]", NamedTextColor.RED)
                            ));
                        }
                        break;
                    default:
                        plugin.adventure().player(player).sendMessage(ChatUtils.helpMessage);
                        break;
                }
            } else {
                plugin.adventure().player(player).sendMessage(ChatUtils.helpMessage);
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subCommands = new ArrayList<>();
        subCommands.add("help");
        subCommands.add("update");
        subCommands.add("load");
        subCommands.add("link");
        subCommands.add("unlink");
        subCommands.add("system");
        subCommands.add("find");
        subCommands.add("autoproxy");
        subCommands.add("member");
        subCommands.add("random");
        subCommands.add("switch");
        subCommands.add("reload");
        if (args.length == 1) {
            return subCommands;
        } else {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("system") || subCommand.equals("s")) {
                List<String> specificSystemCommands = new ArrayList<>();
                specificSystemCommands.add("list");
                specificSystemCommands.add("fronter");
                if (args.length == 2) {
                    List<String> systemCommands = new ArrayList<>();
                    systemCommands.addAll(specificSystemCommands);
                    return systemCommands;
                } else {
                    String systemSub = args[1].toLowerCase();
                    if (args.length == 3 && systemSub.equals("proxy")) {
                        List<String> proxyCommands = new ArrayList<>();
                        proxyCommands.add("on");
                        proxyCommands.add("off");
                        return proxyCommands;
                    } else if (args.length == 3 && systemSub.length() == 5) {
                        return specificSystemCommands;
                    }
                }
            } else if (subCommand.equals("autoproxy") || subCommand.equals("ap")) {
                if (args.length == 2) {
                    List<String> autoProxyCommands = new ArrayList<>();
                    autoProxyCommands.add("off");
                    autoProxyCommands.add("front");
                    autoProxyCommands.add("latch");
                    autoProxyCommands.addAll(getMemberList(sender));
                    return autoProxyCommands;
                }
            } else if (subCommand.equals("member") || subCommand.equals("m")) {
                if (args.length == 2) {
                    List<String> subMember = getMemberList(sender);
                    return subMember;
                }
            } else if (subCommand.equals("switch") || subCommand.equals("sw")) {
                List<String> subMember = getMemberList(sender);
                subMember.add("out");
                return subMember;
            }
        }
        return null;
    }
}
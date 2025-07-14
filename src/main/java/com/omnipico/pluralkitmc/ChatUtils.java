package com.omnipico.pluralkitmc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ChatUtils {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    static final Component pluginComponent = mm.deserialize("<white>Plural<gray>Kit<aqua>MC");
    public static final Component pluginTag = mm.deserialize("<light_purple>[PluralKitMC]</light_purple> ");
    static final Component helpMessage = mm.deserialize(
            mm.serialize(pluginTag) +
                    "<green> Help\nCommands: " +
                    "\n<aqua>/pk help <green>-- Lists the " + mm.serialize(pluginComponent) + "<green> commands" +
                    "\n<aqua>/pk load <system id> <green>-- Links you to the given system id" +
                    "\n<aqua>/pk update <green>-- Forces your system information to refresh" +
                    "\n<aqua>/pk link <token> <green>-- Links your account to the token from pk;token" +
                    "\n<aqua>/pk unlink <green>-- Removes the attached token" +
                    "\n<aqua>/pk autoproxy <off/front/latch/member><green>-- Configures your autoproxy settings" +
                    "\n<aqua>/pk switch [out/member...]<green>-- Switch out or to one or more member" +
                    "\n<aqua>/pk find <search term><green>-- Searches for a member by name" +
                    "\n<aqua>/pk random<green>-- Lists a random member from your system" +
                    "\n<aqua>/pk member <member><green>-- Display information regarding a user in your system" +
                    "\n<aqua>/pk system [list] [full]<green>-- Display information regarding your system, or list its members"
    );

    static Pattern REPLACE_ALL_RGB_PATTERN = Pattern.compile("(&)?&(#[0-9a-fA-F]{6})");
    private static String formatCreated(String created) {
        try {
            Instant instant = Instant.parse(created);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        } catch (Exception e) {
            return created; // :c no worky
        }
    }
    static String replaceColor(String input) {
        // Not needed with MiniMessage, but if you want to support legacy codes:
        input = input.replace("&", "§");
        return input;
    }

    public static Component displayMemberInfo(PluralKitMember member, PluralKitSystem system) {
        String color = member.color != null ? "<#" + member.color + ">" : "<aqua>";
        StringBuilder sb = new StringBuilder();
        sb.append(mm.serialize(pluginTag));
        sb.append("<green> Member information for ");
        if (system.name != null && !system.name.isEmpty()) {
            sb.append(color).append(member.name).append(" <gray>(").append(system.name).append(")");
        } else {
            sb.append(color).append(member.name);
        }
        sb.append("<green>\nDisplay Name: ").append(color).append(member.name);
        if (member.getBirthday() != null) {
            sb.append("<green>\nBirthday: <aqua>").append(member.getBirthday());
        }
        if (member.getPronouns() != null) {
            sb.append("<green>\nPronouns: <aqua>").append(member.getPronouns());
        }
        if (member.getColor() != null) {
            sb.append("<green>\nColor: ").append(color).append("#").append(member.getColor());
        }
        if (member.getProxy_tags() != null && !member.getProxy_tags().isEmpty()) {
            sb.append("<green>\nProxy Tags: ");
            for (int i = 0; i < member.getProxy_tags().size(); i++) {
                if (i > 0) sb.append("<green>, ");
                PluralKitProxy proxyTag = member.getProxy_tags().get(i);
                sb.append("<gray>").append(proxyTag.getPrefix()).append("text").append(proxyTag.getSuffix());
            }
        }
        if (member.getDescription() != null) {
            sb.append("<green>\nDescription: <gray>").append(member.getDescription());
        }
        sb.append("<green>\nCreated: <aqua>").append(formatCreated(member.getCreated()));
        sb.append("<green>\nSystem ID: <gray>").append(system.getId());
        sb.append("<green>\nMember ID: <gray>").append(member.getId());
        return mm.deserialize(sb.toString());
    }

    public static Component displaySystemInfo(PluralKitSystem system) {
        StringBuilder sb = new StringBuilder();
        sb.append(mm.serialize(pluginTag));
        sb.append("<green> System information for ");
        if (system.name != null && !system.name.isEmpty()) {
            sb.append("<aqua>").append(system.name)
                    .append("<green> (<gray>").append(system.id).append("<green>)");
        } else {
            sb.append("<gray>").append(system.id);
        }
        if (system.tag != null) {
            sb.append("<green>\nTag: <aqua>").append(system.tag);
        }
        if (system.description != null && !system.description.isEmpty()) {
            sb.append("<green>\nDescription: <gray>").append(system.description);
        }
        return mm.deserialize(sb.toString());
    }

    public static Component displayMemberListForm(PluralKitMember member) {
        String color = member.color != null ? "<#" + member.color + ">" : "<aqua>";
        StringBuilder sb = new StringBuilder();
        sb.append("<white>[<gray>").append(member.id).append("<white>] ").append(color).append(member.name);
        if (member.getProxy_tags() != null && !member.getProxy_tags().isEmpty()) {
            sb.append("<white> (");
            for (int i = 0; i < member.getProxy_tags().size(); i++) {
                if (i > 0) sb.append("<white>, ");
                PluralKitProxy proxyTag = member.getProxy_tags().get(i);
                sb.append("<gray>").append(proxyTag.getPrefix()).append("text").append(proxyTag.getSuffix());
            }
            sb.append("<white>)");
        }
        return mm.deserialize(sb.toString());
    }

    public static Component displayMemberList(List<PluralKitMember> members, PluralKitSystem system) {
        StringBuilder sb = new StringBuilder();
        sb.append(mm.serialize(pluginTag));
        sb.append("<green> Members of ");
        if (system.name != null && !system.name.isEmpty()) {
            sb.append("<aqua>").append(system.name)
                    .append("<green> (<gray>").append(system.id).append("<green>)");
        } else {
            sb.append("<gray>").append(system.id);
        }
        for (PluralKitMember member : members) {
            sb.append("\n").append(mm.serialize(displayMemberListForm(member)));
        }
        return mm.deserialize(sb.toString());
    }

    public static Component displayMemberSearch(List<PluralKitMember> members, PluralKitSystem system, String search) {
        StringBuilder sb = new StringBuilder();
        sb.append(mm.serialize(pluginTag));
        sb.append("<green> Members of ");
        if (system.name != null && !system.name.isEmpty()) {
            sb.append("<aqua>").append(system.name)
                    .append("<green> (<gray>").append(system.id).append("<green>)");
        } else {
            sb.append("<gray>").append(system.id);
        }
        sb.append("<green> matching <aqua>").append(search);
        for (PluralKitMember member : members) {
            sb.append("\n").append(mm.serialize(displayMemberListForm(member)));
        }
        return mm.deserialize(sb.toString());
    }
}

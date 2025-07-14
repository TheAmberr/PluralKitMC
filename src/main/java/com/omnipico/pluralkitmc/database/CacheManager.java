package com.omnipico.pluralkitmc.database;

import com.google.gson.Gson;
import com.omnipico.pluralkitmc.PluralKitMember;
import com.omnipico.pluralkitmc.PluralKitSystem;
import com.omnipico.pluralkitmc.UserCache;
import com.omnipico.pluralkitmc.PluralKitMC;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.omnipico.pluralkitmc.PluralKitMC.getInstance;
import static com.omnipico.pluralkitmc.database.KeyManager.getKey;
import static com.omnipico.pluralkitmc.database.KeyManager.encrypt;
import static com.omnipico.pluralkitmc.database.KeyManager.decrypt;
import static java.sql.DriverManager.getConnection;

public class CacheManager {
    public static Connection conn;
    private static byte[] key;
    private static final Map<UUID, UserCache> memUserCache = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    public static void connect() {
        try {
            conn = getConnection("jdbc:sqlite:" + getInstance().getDataFolder() + "/cache/cache.db");
            Statement state = conn.createStatement();
            state.executeUpdate("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                system TEXT,
                system_cache TEXT,
                members_cache TEXT,
                token TEXT,
                ap_mode TEXT,
                last_updated INTEGER
            )
            """);
            key = getKey();
        } catch (SQLException e) {
            getInstance().getLogger().severe("Could not connect to the database, error: " + e.getMessage());
        }
    }

    public static void disconnect() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            getInstance().getLogger().severe("Could not disconnect from the database, error: " + e.getMessage());
        }
    }

    public static void loadFromCache() {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM players");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String systemId = decrypt(key, rs.getString("system"));
                String token = decrypt(key, rs.getString("token"));
                UserCache cache = new UserCache(uuid, systemId, token, getInstance(), true);

                // Optionally, load system and members from JSON cache
                String systemCache = rs.getString("system_cache");
                if (systemCache != null && !systemCache.isEmpty()) {
                    cache.system = gson.fromJson(systemCache, PluralKitSystem.class);
                }
                String membersCache = rs.getString("members_cache");
                if (membersCache != null && !membersCache.isEmpty()) {
                    PluralKitMember[] members = gson.fromJson(membersCache, PluralKitMember[].class);
                    cache.members = Arrays.asList(members);
                }
                cache.autoProxyMode = rs.getString("ap_mode");
                cache.lastUpdated = rs.getLong("last_updated");
                memUserCache.put(uuid, cache);
            }
        } catch (Exception e) {
            getInstance().getLogger().severe("Could not load from cache, error: " + e.getMessage());
        }
    }

    public static void saveToDatabase(UserCache u, PluralKitMC plugin) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, system, system_cache, members_cache, token, ap_mode, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, u.uuid.toString());
            stmt.setString(2, encrypt(key, u.systemId));
            stmt.setString(3, u.system != null ? gson.toJson(u.system) : "");
            stmt.setString(4, u.members != null ? gson.toJson(u.members) : "");
            stmt.setString(5, encrypt(key, u.token));
            stmt.setString(6, u.autoProxyMode);
            stmt.setLong(7, u.lastUpdated);
            stmt.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save to cache, error: " + e.getMessage());
        }
    }
    public static void addToMemCache(UserCache u) {
        memUserCache.put(u.uuid, u);
    }

    public static UserCache getFromMemCache(UUID uuid) {
        return memUserCache.get(uuid);
    }

    public static UserCache loadFromDatabase(UUID uuid, PluralKitMC plugin) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String systemId = decrypt(key, rs.getString("system"));
                String token = decrypt(key, rs.getString("token"));
                UserCache cache = new UserCache(uuid, systemId, token, plugin, true);

                String systemCache = rs.getString("system_cache");
                if (systemCache != null && !systemCache.isEmpty()) {
                    cache.system = gson.fromJson(systemCache, PluralKitSystem.class);
                }
                String membersCache = rs.getString("members_cache");
                if (membersCache != null && !membersCache.isEmpty()) {
                    PluralKitMember[] members = gson.fromJson(membersCache, PluralKitMember[].class);
                    cache.members = Arrays.asList(members);
                }
                cache.autoProxyMode = rs.getString("ap_mode");
                cache.lastUpdated = rs.getLong("last_updated");
                return cache;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load user from database, error: " + e.getMessage());
        }
        return null;
    }
}

package com.omnipico.pluralkitmc;

import com.omnipico.pluralkitmc.database.CacheManager;

import java.util.*;

public class PluralKitData {
    PluralKitMC plugin;
    long cacheUpdateFrequency;

    public PluralKitData(PluralKitMC plugin) {
        this.plugin = plugin;
        this.cacheUpdateFrequency = plugin.getConfig().getLong("cache-update-frequency");
    }

    public void setConfig(org.bukkit.configuration.file.FileConfiguration config) {}

    String getSystemId(UUID uuid) {
        UserCache user = CacheManager.getFromMemCache(uuid);
        return user != null ? user.systemId : null;
    }

    String getToken(UUID uuid) {
        UserCache user = CacheManager.getFromMemCache(uuid);
        return user != null ? user.token : null;
    }

    void setSystemId(UUID uuid, String systemId) {
        String token = null;
        UserCache user = CacheManager.getFromMemCache(uuid);
        if (user != null) {
            token = user.getToken();
        }
        UserCache newUser = new UserCache(uuid, systemId, token, plugin, true);
        CacheManager.addToMemCache(newUser);
        CacheManager.saveToDatabase(newUser, plugin);
    }

    void setSystemId(UUID uuid, String systemId, String token) {
        UserCache newUser = new UserCache(uuid, systemId, token, plugin, true);
        CacheManager.addToMemCache(newUser);
        CacheManager.saveToDatabase(newUser, plugin);
    }

    UserCache getCacheOrCreate(UUID uuid, boolean blocking) {
        UserCache user = CacheManager.getFromMemCache(uuid);
        if (user != null) {
            user.updateIfNeeded(cacheUpdateFrequency, blocking);
            return user;
        } else {
            String systemId = getSystemId(uuid);
            String token = getToken(uuid);
            if (systemId != null) {
                UserCache newUser = new UserCache(uuid, systemId, token, plugin, blocking);
                CacheManager.addToMemCache(newUser);
                return newUser;
            } else {
                return null;
            }
        }
    }

    UserCache getCacheOrCreate(UUID uuid) {
        return getCacheOrCreate(uuid, false);
    }

    boolean setToken(UUID uuid, String token) {
        UserCache user = getCacheOrCreate(uuid);
        String systemId = UserCache.verifyToken(plugin, token);
        if (systemId != null) {
            if (user == null) {
                setSystemId(uuid, systemId, token);
            } else {
                user.setToken(token);
                user.update(true);
                CacheManager.saveToDatabase(user, plugin);
            }
            return true;
        }
        return false;
    }

    void updateAutoProxyMode(UUID uuid, String mode) {
        UserCache userCache = getCacheOrCreate(uuid);
        if (userCache != null) {
            userCache.setAutoProxyMode(mode);
            CacheManager.saveToDatabase(userCache, plugin);
        }
    }

    boolean updateAutoProxyMember(UUID uuid, String memberName) {
        UserCache userCache = getCacheOrCreate(uuid);
        memberName = memberName.toLowerCase();
        if (userCache != null) {
            for (PluralKitMember member : userCache.members) {
                if (member.id.equals(memberName) || member.name.toLowerCase().equals(memberName)) {
                    userCache.setAutoProxyMember(member.id);
                    CacheManager.saveToDatabase(userCache, plugin);
                    return true;
                }
            }
        }
        return false;
    }

    void updateFronters(UUID uuid, List<String> fronterNames) {
        UserCache userCache = getCacheOrCreate(uuid);
        if (userCache != null) {
            List<PluralKitMember> members = new ArrayList<>();
            for (String fronterName : fronterNames) {
                PluralKitMember member = userCache.getMemberByIdOrName(fronterName);
                if (member != null) {
                    members.add(member);
                }
            }
            userCache.setFronters(members);
            CacheManager.saveToDatabase(userCache, plugin);
        }
    }

    void updateCache(UUID uuid, boolean blocking) {
        UserCache user = getCacheOrCreate(uuid, blocking);
        if (user != null) {
            user.update(blocking);
            CacheManager.saveToDatabase(user, plugin);
        }
    }

    void clearCache(UUID uuid) {
        CacheManager.getFromMemCache(uuid);
    }

    String getSystemTag(UUID uuid) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null && user.system != null) {
            return user.system.tag;
        } else {
            return null;
        }
    }

    List<PluralKitMember> getMembers(UUID uuid) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null) {
            return user.members;
        } else {
            return new ArrayList<>();
        }
    }

    PluralKitSystem getSystem(UUID uuid) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null) {
            return user.system;
        } else {
            return null;
        }
    }

    PluralKitMember getRandomMember(UUID uuid) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null) {
            return user.getRandomMember();
        } else {
            return null;
        }
    }

    List<PluralKitMember> searchMembers(UUID uuid, String search) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null) {
            return user.searchMembers(search);
        } else {
            return new ArrayList<>();
        }
    }

    PluralKitMember getMemberByName(UUID uuid, String search) {
        UserCache user = getCacheOrCreate(uuid);
        if (user != null) {
            return user.getMemberByIdOrName(search);
        } else {
            return null;
        }
    }

    PluralKitMember getProxiedUser(UUID uuid, String message) {
        UserCache user = getCacheOrCreate(uuid);
        List<PluralKitMember> members = getMembers(uuid);
        int fitStrength = 0;
        PluralKitMember bestFit = null;
        for (PluralKitMember member : members) {
            for (PluralKitProxy proxy : member.proxy_tags) {
                int proxyLength = 0;
                if (proxy.getPrefix() != null) {
                    proxyLength += proxy.getPrefix().length();
                }
                if (proxy.getSuffix() != null) {
                    proxyLength += proxy.getSuffix().length();
                }
                if (message.length() > proxyLength && (proxy.getPrefix() == null || message.startsWith(proxy.getPrefix()))
                        && (proxy.getSuffix() == null || message.endsWith(proxy.getSuffix()))
                        && (proxy.getPrefix() != null || proxy.getSuffix() != null)) {
                    if (proxyLength > fitStrength) {
                        fitStrength = proxyLength;
                        bestFit = member;
                    }
                }
            }
        }
        if (bestFit == null && user != null) {
            String apMode = user.getAutoProxyMode();
            if (apMode.equals("member")) {
                bestFit = user.getMemberById(user.getAutoProxyMember());
            } else if (apMode.equals("latch") && user.getLastProxied() != null) {
                bestFit = user.getMemberById(user.getLastProxied());
            } else if (apMode.equals("front")) {
                bestFit = user.getFirstFronter();
            }
        }
        if (bestFit != null && user != null) {
            user.setLastProxied(bestFit.id);
            CacheManager.saveToDatabase(user, plugin);
        }
        return bestFit;
    }

    PluralKitProxy getProxy(UUID uuid, String message) {
        UserCache user = getCacheOrCreate(uuid);
        List<PluralKitMember> members = getMembers(uuid);
        int fitStrength = 0;
        PluralKitProxy bestFit = null;
        for (PluralKitMember member : members) {
            for (PluralKitProxy proxy : member.proxy_tags) {
                int proxyLength = 0;
                if (proxy.getPrefix() != null) {
                    proxyLength += proxy.getPrefix().length();
                }
                if (proxy.getSuffix() != null) {
                    proxyLength += proxy.getSuffix().length();
                }
                if (message.length() > proxyLength && (proxy.getPrefix() == null || message.startsWith(proxy.getPrefix()))
                        && (proxy.getSuffix() == null || message.endsWith(proxy.getSuffix()))
                        && (proxy.getPrefix() != null || proxy.getSuffix() != null)) {
                    if (proxyLength > fitStrength) {
                        fitStrength = proxyLength;
                        bestFit = proxy;
                    }
                }
            }
        }
        if (bestFit == null) {
            bestFit = new PluralKitProxy("","");
        }
        return bestFit;
    }
}

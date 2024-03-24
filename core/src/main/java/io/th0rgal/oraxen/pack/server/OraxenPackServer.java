package io.th0rgal.oraxen.pack.server;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface OraxenPackServer {

    @Nullable
    static OraxenPackServer initializeServer() {
        return switch (PackServerType.fromSetting()) {
            case SELFHOST -> new SelfHostServer();
            case POLYMATH -> new PolymathServer();
            case NONE -> null;
        };
    }

    default boolean uploadPack() {
        return true;
    }

    void sendPack(Player player);

    default void start() {
    }

    default void stop() {
    }

    static byte[] hashArray(String hash) {
        int len = hash.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hash.charAt(i), 16) << 4)
                    + Character.digit(hash.charAt(i + 1), 16));
        }
        return data;
    }
}
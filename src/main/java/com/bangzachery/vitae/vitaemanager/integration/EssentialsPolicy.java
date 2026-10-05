package com.bangzachery.vitae.vitaemanager.integration;

import java.util.Locale;
import java.util.Set;

public final class EssentialsPolicy {
    private EssentialsPolicy() { }
    private static final Set<String> ECONOMY = Set.of("money", "bal", "balance", "pay", "eco", "economy",
            "baltop", "balancetop", "emoney", "ebal", "ebalance", "epay", "eeco", "eeconomy", "ebaltop", "ebalancetop");

    public static String label(String commandLine) {
        if (commandLine == null || !commandLine.startsWith("/")) return "";
        return commandLine.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    }

    public static boolean blocked(String label, String canonical, String owner) {
        if (!"essentials".equalsIgnoreCase(owner)) return false;
        String key = label.toLowerCase(Locale.ROOT);
        if (key.startsWith("essentials:")) key = key.substring(11);
        return ECONOMY.contains(key) || ECONOMY.contains(canonical.toLowerCase(Locale.ROOT));
    }
}
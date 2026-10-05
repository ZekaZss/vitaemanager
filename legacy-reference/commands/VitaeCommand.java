package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VitaeCommand implements CommandExecutor, TabCompleter {

    private final ConfigurationService configuration;
    private final MessageService messages;
    private final Logger logger;

    public VitaeCommand(
            ConfigurationService configuration,
            MessageService messages,
            Logger logger
    ) {
        this.configuration = configuration;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("vitae.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0
                || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            messages.send(sender, "help");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vitae.admin.reload")) {
                messages.send(sender, "no-permission");
                return true;
            }

            try {
                configuration.reload();
                messages.send(sender, "reloaded");
            } catch (IOException
                     | InvalidConfigurationException
                     | IllegalArgumentException exception) {
                logger.log(
                        Level.WARNING,
                        "Reload config.yml gagal; konfigurasi aktif dipertahankan.",
                        exception
                );
                messages.send(sender, "reload-failed");
            }

            return true;
        }

        messages.send(sender, "usage");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("vitae.admin") || args.length != 1) {
            return List.of();
        }

        List<String> choices = sender.hasPermission("vitae.admin.reload")
                ? List.of("help", "reload")
                : List.of("help");

        String prefix = args[0].toLowerCase(Locale.ROOT);

        return choices.stream()
                .filter(choice -> choice.startsWith(prefix))
                .toList();
    }
}
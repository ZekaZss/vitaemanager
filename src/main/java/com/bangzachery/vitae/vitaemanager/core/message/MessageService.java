package com.bangzachery.vitae.vitaemanager.core.message;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Objects;

public final class MessageService {
    private final ConfigurationService configuration;

    public MessageService(ConfigurationService configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Component.empty());
    }

    public void send(
            CommandSender sender, String key, Component detail) {
        ConfigurationService.Snapshot snapshot =
                configuration.current();

        Component prefix = snapshot.messages().get("prefix");
        Component message = snapshot.messages().get(key);

        if (prefix == null || message == null) {
            throw new IllegalArgumentException(
                    "Message key tidak terdaftar: " + key);
        }

        sender.sendMessage(
                prefix.append(message).append(detail));
    }

    public Component component(String key) {
        Component message =
                configuration.current().messages().get(key);

        if (message == null) {
            throw new IllegalArgumentException(
                    "Message key tidak terdaftar: " + key);
        }

        return message;
    }
}
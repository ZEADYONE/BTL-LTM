package com.bomberman.clientfx.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * TCP endpoint of the game server. Each field is resolved independently from, in order:
 * system property, environment variable, the address saved by the player, resource default.
 */
public record ClientNetworkConfig(String host, int port) {

    static final String HOST_PROPERTY = "bomberman.server.host";
    static final String PORT_PROPERTY = "bomberman.tcp.port";
    static final String HOST_ENVIRONMENT = "BOMBERMAN_SERVER_HOST";
    static final String PORT_ENVIRONMENT = "BOMBERMAN_TCP_PORT";
    private static final String CONFIG_RESOURCE = "/client.properties";

    public ClientNetworkConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535");
        }
    }

    /** @param saved address chosen by the player, or {@code null} when none was saved */
    public static ClientNetworkConfig load(ClientNetworkConfig saved) {
        return load(System::getProperty, System::getenv, saved);
    }

    static ClientNetworkConfig load(
            UnaryOperator<String> systemProperties,
            UnaryOperator<String> environment,
            ClientNetworkConfig saved
    ) {
        Properties defaults = loadDefaults();
        String host = firstNonBlank(
                systemProperties.apply(HOST_PROPERTY),
                environment.apply(HOST_ENVIRONMENT),
                saved == null ? null : saved.host(),
                defaults.getProperty("server.host")
        );
        String rawPort = firstNonBlank(
                systemProperties.apply(PORT_PROPERTY),
                environment.apply(PORT_ENVIRONMENT),
                saved == null ? null : Integer.toString(saved.port()),
                defaults.getProperty("server.port")
        );
        if (host == null || rawPort == null) {
            throw new IllegalStateException(CONFIG_RESOURCE + " must define server.host and server.port");
        }
        try {
            return new ClientNetworkConfig(host.trim(), Integer.parseInt(rawPort.trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid TCP port: " + rawPort, exception);
        }
    }

    /** Human-readable {@code host:port}, as shown in the UI and in connection errors. */
    public String displayAddress() {
        return host + ":" + port;
    }

    private static Properties loadDefaults() {
        Properties defaults = new Properties();
        try (InputStream input = ClientNetworkConfig.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + CONFIG_RESOURCE);
            }
            defaults.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + CONFIG_RESOURCE, exception);
        }
        return defaults;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

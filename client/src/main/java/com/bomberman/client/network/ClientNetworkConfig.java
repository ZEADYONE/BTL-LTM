package com.bomberman.client.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads the TCP endpoint from system properties/environment with resource defaults. */
public record ClientNetworkConfig(String host, int port) {

    private static final String CONFIG_RESOURCE = "/client.properties";

    public ClientNetworkConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Server port must be between 1 and 65535");
        }
    }

    public static ClientNetworkConfig load() {
        Properties defaults = new Properties();
        try (InputStream input = ClientNetworkConfig.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + CONFIG_RESOURCE);
            }
            defaults.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load " + CONFIG_RESOURCE, exception);
        }

        String host = configuredValue(
                "bomberman.server.host",
                "BOMBERMAN_SERVER_HOST",
                defaults.getProperty("server.host")
        );
        String rawPort = configuredValue(
                "bomberman.tcp.port",
                "BOMBERMAN_TCP_PORT",
                defaults.getProperty("server.port")
        );
        try {
            return new ClientNetworkConfig(host, Integer.parseInt(rawPort));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid TCP port: " + rawPort, exception);
        }
    }

    private static String configuredValue(
            String systemProperty,
            String environmentVariable,
            String defaultValue
    ) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentVariable);
        }
        if (value == null || value.isBlank()) {
            value = defaultValue;
        }
        return value;
    }
}

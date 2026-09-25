package com.bomberman.client.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientNetworkConfigTest {

    @Test
    void systemPropertiesOverrideResourceDefaults() {
        String previousHost = System.getProperty("bomberman.server.host");
        String previousPort = System.getProperty("bomberman.tcp.port");
        try {
            System.setProperty("bomberman.server.host", "game.example.test");
            System.setProperty("bomberman.tcp.port", "19081");

            ClientNetworkConfig config = ClientNetworkConfig.load();

            assertEquals("game.example.test", config.host());
            assertEquals(19081, config.port());
        } finally {
            restore("bomberman.server.host", previousHost);
            restore("bomberman.tcp.port", previousPort);
        }
    }

    private void restore(String property, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, previousValue);
        }
    }
}

package com.bomberman.clientfx.network;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientNetworkConfigTest {

    private static final UnaryOperator<String> NONE = key -> null;

    @Test
    void systemPropertiesOverrideResourceDefaults() {
        String previousHost = System.getProperty(ClientNetworkConfig.HOST_PROPERTY);
        String previousPort = System.getProperty(ClientNetworkConfig.PORT_PROPERTY);
        try {
            System.setProperty(ClientNetworkConfig.HOST_PROPERTY, "game.example.test");
            System.setProperty(ClientNetworkConfig.PORT_PROPERTY, "19081");

            ClientNetworkConfig config = ClientNetworkConfig.load(null);

            assertEquals("game.example.test", config.host());
            assertEquals(19081, config.port());
        } finally {
            restore(ClientNetworkConfig.HOST_PROPERTY, previousHost);
            restore(ClientNetworkConfig.PORT_PROPERTY, previousPort);
        }
    }

    @Test
    void resourceDefaultsApplyWhenNothingElseIsSet() {
        ClientNetworkConfig config = ClientNetworkConfig.load(NONE, NONE, null);

        assertEquals(new ClientNetworkConfig("127.0.0.1", 8081), config);
    }

    @Test
    void savedAddressOverridesResourceDefaults() {
        ClientNetworkConfig saved = new ClientNetworkConfig("192.168.1.20", 9000);

        assertEquals(saved, ClientNetworkConfig.load(NONE, NONE, saved));
    }

    @Test
    void launchOverridesWinOverSavedAddressFieldByField() {
        Map<String, String> environment = Map.of(ClientNetworkConfig.PORT_ENVIRONMENT, "7000");
        Map<String, String> properties = Map.of(ClientNetworkConfig.HOST_PROPERTY, "lan-server");

        ClientNetworkConfig config = ClientNetworkConfig.load(
                properties::get,
                environment::get,
                new ClientNetworkConfig("192.168.1.20", 9000)
        );

        assertEquals(new ClientNetworkConfig("lan-server", 7000), config);
    }

    @Test
    void invalidPortIsRejected() {
        Map<String, String> properties = Map.of(ClientNetworkConfig.PORT_PROPERTY, "not-a-port");

        assertThrows(IllegalStateException.class, () -> ClientNetworkConfig.load(properties::get, NONE, null));
        assertThrows(IllegalArgumentException.class, () -> new ClientNetworkConfig("host", 65_536));
    }

    @Test
    void displayAddressJoinsHostAndPort() {
        assertEquals("10.0.0.5:8081", new ClientNetworkConfig("10.0.0.5", 8081).displayAddress());
    }

    private void restore(String property, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, previousValue);
        }
    }
}

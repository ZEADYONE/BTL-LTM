package com.bomberman.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "bomberman.tcp.port=0")
class BombermanServerApplicationTests {

    @Test
    void contextLoads() {
    }
}

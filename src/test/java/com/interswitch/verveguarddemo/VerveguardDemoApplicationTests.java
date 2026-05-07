package com.interswitch.verveguarddemo;

import com.interswitch.verveguarddemo.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class VerveguardDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}

package com.itmao.aispringboot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedLocalSecretSurvivesRestart() {
        Path secretFile = tempDir.resolve("jwt-secret.key");
        JwtConfig firstStart = config(secretFile);
        firstStart.init();

        JwtConfig secondStart = config(secretFile);
        secondStart.init();

        assertTrue(firstStart.getSecret().length() >= 32);
        assertEquals(firstStart.getSecret(), secondStart.getSecret());
    }

    private static JwtConfig config(Path secretFile) {
        JwtConfig config = new JwtConfig();
        config.setSecret("");
        config.setLocalSecretFile(secretFile.toString());
        return config;
    }
}

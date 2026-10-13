package com.udea.demo.usuarios.application.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokensSegurosTest {
    @Test
    void elHashContinuaCompatibleConLaApiAnterior() {
        assertEquals(AutenticacionService.hash("ejemplo"), TokensSeguros.hash("ejemplo"));
        assertEquals("_ZhLrTY-mjACFGAJFyZGe2W7HojDF1LZPGit6H5JRuQ", TokensSeguros.hash("ejemplo"));
    }

    @Test
    void losTokensSonDiferentesYConFormatoSeguroParaUrls() {
        String primero = TokensSeguros.generarToken();
        String segundo = TokensSeguros.generarToken();
        assertNotEquals(primero, segundo);
        assertTrue(primero.matches("[A-Za-z0-9_-]+"));
        assertEquals(64, primero.length());
    }
}

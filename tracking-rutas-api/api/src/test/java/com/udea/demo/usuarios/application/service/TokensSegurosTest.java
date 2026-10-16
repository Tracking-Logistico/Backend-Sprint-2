package com.udea.demo.usuarios.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TokensSeguros - generación de tokens criptográficos y hashing")
class TokensSegurosTest {

    @Test
    @DisplayName("El hash SHA-256 en Base64 URL es determinista y coincide con AutenticacionService")
    void elHashEsDeterministaYCompatibleConAutenticacionService() throws NoSuchAlgorithmException {
        String valor = "PasswordSegura123!";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        String esperado = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

        String hash = TokensSeguros.hash(valor);

        assertEquals(esperado, hash);
        assertEquals(hash, TokensSeguros.hash(valor));
        assertEquals(hash, AutenticacionService.hash(valor));
        assertNotEquals(hash, TokensSeguros.hash("OtraClave456@"));
        assertTrue(hash.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    @DisplayName("Los tokens generados son aleatorios, distintos y en formato Base64 URL sin padding")
    void losTokensSonDiferentesYConFormatoSeguroParaUrls() {
        String primero = TokensSeguros.generarToken();
        String segundo = TokensSeguros.generarToken();

        assertNotEquals(primero, segundo);
        assertTrue(primero.matches("[A-Za-z0-9_-]+"));
        assertTrue(segundo.matches("[A-Za-z0-9_-]+"));
        assertEquals(64, primero.length());
        assertEquals(64, segundo.length());
    }
}

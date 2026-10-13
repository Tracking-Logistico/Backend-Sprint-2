package com.udea.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"app.api.role=usuarios", "app.messaging.rabbitmq.enabled=false", "spring.flyway.enabled=false"})
@ActiveProfiles("test")
class LimitesApiTest {
    @Autowired RequestMappingHandlerMapping rutas;

    @Test
    void publicaUnicamenteControladoresPropios() {
        Set<String> paquetes = rutas.getHandlerMethods().values().stream()
                .map(handler -> handler.getBeanType().getPackageName())
                .filter(p -> p.startsWith("com.udea.demo.") && p.contains(".infrastructure.controller"))
                .collect(Collectors.toSet());
        assertFalse(paquetes.isEmpty());
        for (String paquete : paquetes) {
            assertTrue(paquete.startsWith("com.udea.demo.usuarios."), "Controlador ajeno: " + paquete);
        }
    }
}

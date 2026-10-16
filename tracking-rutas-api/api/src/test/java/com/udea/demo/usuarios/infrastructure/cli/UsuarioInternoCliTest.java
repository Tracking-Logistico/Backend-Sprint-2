package com.udea.demo.usuarios.infrastructure.cli;

import com.udea.demo.usuarios.application.dto.CrearUsuarioInternoCommand;
import com.udea.demo.usuarios.application.dto.EditarUsuarioInternoCommand;
import com.udea.demo.usuarios.application.dto.ResultadoCreacionUsuarioInterno;
import com.udea.demo.usuarios.domain.model.Rol;
import com.udea.demo.usuarios.interfaces.services.UsuarioInternoServiceI;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioInternoCli - interfaz de línea de comandos para gestión de usuarios internos")
class UsuarioInternoCliTest {

    @Mock private UsuarioInternoServiceI service;
    @Mock private Validator validator;
    @Mock private ApplicationArguments args;

    @InjectMocks
    private UsuarioInternoCli cli;

    @Test
    @DisplayName("Sin argumentos no ejecuta ningún comando")
    void run_sinArgs_noFalla() {
        when(args.getNonOptionArgs()).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> cli.run(args));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Comando desconocido muestra mensaje y no falla")
    void run_comandoDesconocido_noFalla() {
        when(args.getNonOptionArgs()).thenReturn(List.of("desconocido"));
        when(args.getOptionNames()).thenReturn(Set.of());

        assertThatNoException().isThrownBy(() -> cli.run(args));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Comando crear invoca al servicio con datos mapeados")
    void run_crear_exitoso() {
        when(args.getNonOptionArgs()).thenReturn(List.of("crear"));
        when(args.getOptionNames()).thenReturn(Set.of("nombre", "email", "telefono", "rol", "direccion", "licencia"));
        when(args.getOptionValues("nombre")).thenReturn(List.of("Conductor Uno"));
        when(args.getOptionValues("email")).thenReturn(List.of("cond@test.com"));
        when(args.getOptionValues("telefono")).thenReturn(List.of("300123"));
        when(args.getOptionValues("rol")).thenReturn(List.of("CONDUCTOR"));
        when(args.getOptionValues("direccion")).thenReturn(List.of("Calle 10"));
        when(args.getOptionValues("licencia")).thenReturn(List.of("LIC-123"));
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        ResultadoCreacionUsuarioInterno res = new ResultadoCreacionUsuarioInterno(1L, "cond@test.com", Rol.CONDUCTOR, "Temp123!");
        when(service.crear(any(CrearUsuarioInternoCommand.class))).thenReturn(res);

        cli.run(args);

        verify(service).crear(any(CrearUsuarioInternoCommand.class));
    }

    @Test
    @DisplayName("Comando editar invoca al servicio")
    void run_editar_exitoso() {
        when(args.getNonOptionArgs()).thenReturn(List.of("editar"));
        when(args.getOptionNames()).thenReturn(Set.of("id", "nombre", "rol"));
        when(args.getOptionValues("id")).thenReturn(List.of("5"));
        when(args.getOptionValues("nombre")).thenReturn(List.of("Nuevo Nombre"));
        when(args.getOptionValues("rol")).thenReturn(List.of("OPERADOR"));
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        cli.run(args);

        verify(service).editar(eq(5L), any(EditarUsuarioInternoCommand.class));
    }

    @Test
    @DisplayName("Comando desactivar invoca al servicio")
    void run_desactivar_exitoso() {
        when(args.getNonOptionArgs()).thenReturn(List.of("desactivar"));
        when(args.getOptionNames()).thenReturn(Set.of("id"));
        when(args.getOptionValues("id")).thenReturn(List.of("7"));

        cli.run(args);

        verify(service).desactivar(7L);
    }
}

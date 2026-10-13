package com.udea.orquestador.infraestructura;

import com.udea.orquestador.aplicacion.TransporteHttp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class TransporteHttpCliente implements TransporteHttp {
    private static final Set<String> NO_REENVIAR = Set.of(
            "host", "connection", "content-length", "transfer-encoding", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailer", "upgrade", "expect",
            "origin");
    private static final Set<String> NO_RESPONDER = Set.of(
            "connection", "content-length", "transfer-encoding", "keep-alive", "trailer",
            "upgrade", "access-control-allow-origin", "access-control-allow-credentials",
            "access-control-allow-methods", "access-control-allow-headers", "access-control-expose-headers",
            "access-control-max-age");
    private final HttpClient cliente;
    private final Duration tiempoMaximo;

    public TransporteHttpCliente(@Value("${gateway.connect-timeout-ms:5000}") long conexion,
                                @Value("${gateway.request-timeout-ms:60000}") long respuesta) {
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(conexion))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.tiempoMaximo = Duration.ofMillis(respuesta);
    }

    @Override
    public void reenviar(URI base, HttpServletRequest entrada, HttpServletResponse salida) throws IOException {
        String ruta = entrada.getRequestURI();
        String consulta = entrada.getQueryString();
        URI destino = URI.create(base.toString() + ruta + (consulta == null ? "" : "?" + consulta));
        HttpRequest.BodyPublisher cuerpo = cuerpo(entrada);
        HttpRequest.Builder peticion = HttpRequest.newBuilder(destino)
                .timeout(tiempoMaximo)
                .method(entrada.getMethod(), cuerpo);

        Enumeration<String> nombres = entrada.getHeaderNames();
        while (nombres != null && nombres.hasMoreElements()) {
            String nombre = nombres.nextElement();
            if (NO_REENVIAR.contains(nombre.toLowerCase(Locale.ROOT))
                    || nombre.toLowerCase(Locale.ROOT).startsWith("x-forwarded-")) continue;
            Enumeration<String> valores = entrada.getHeaders(nombre);
            while (valores.hasMoreElements()) peticion.header(nombre, valores.nextElement());
        }
        peticion.header("X-Forwarded-For", entrada.getRemoteAddr());
        peticion.header("X-Forwarded-Proto", entrada.getScheme());
        peticion.header("X-Forwarded-Host", entrada.getHeader("Host") == null ? "" : entrada.getHeader("Host"));

        try {
            HttpResponse<InputStream> respuesta = cliente.send(
                    peticion.build(), HttpResponse.BodyHandlers.ofInputStream());
            salida.setStatus(respuesta.statusCode());
            respuesta.headers().map().forEach((nombre, valores) -> {
                if (!NO_RESPONDER.contains(nombre.toLowerCase(Locale.ROOT))) {
                    valores.forEach(valor -> salida.addHeader(nombre, valor));
                }
            });
            try (InputStream flujo = respuesta.body()) {
                if (!"HEAD".equalsIgnoreCase(entrada.getMethod())) {
                    flujo.transferTo(salida.getOutputStream());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            errorTemporal(salida, 504, "TIMEOUT_SERVICIO");
        } catch (java.net.http.HttpTimeoutException ex) {
            errorTemporal(salida, 504, "TIMEOUT_SERVICIO");
        } catch (IOException ex) {
            if (!salida.isCommitted()) {
                errorTemporal(salida, 502, "SERVICIO_NO_DISPONIBLE");
            } else {
                throw ex;
            }
        } catch (IllegalArgumentException ex) {
            errorTemporal(salida, 400, "PETICION_INVALIDA");
        }
    }

    private HttpRequest.BodyPublisher cuerpo(HttpServletRequest entrada) throws IOException {
        if ("GET".equalsIgnoreCase(entrada.getMethod()) || "HEAD".equalsIgnoreCase(entrada.getMethod())
                || "DELETE".equalsIgnoreCase(entrada.getMethod()) && entrada.getContentLengthLong() == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        InputStream datos = entrada.getInputStream();
        return HttpRequest.BodyPublishers.ofInputStream(() -> datos);
    }

    private void errorTemporal(HttpServletResponse salida, int codigo, String causa) throws IOException {
        if (salida.isCommitted()) return;
        salida.reset();
        salida.setStatus(codigo);
        salida.setContentType("application/json;charset=UTF-8");
        salida.getWriter().write("{\"code\":\"" + causa + "\",\"error\":\"No fue posible contactar el servicio\"}");
    }
}

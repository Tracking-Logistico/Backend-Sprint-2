-- Idempotencia durable de los resultados de entrega (mismo idEventoCliente => un solo registro).
CREATE TABLE IF NOT EXISTS eventos_resultado_entrega (
    id_evento_cliente VARCHAR(36) PRIMARY KEY,
    id_pedido BIGINT NOT NULL REFERENCES pedidos(id_pedido),
    id_usuario BIGINT NOT NULL,
    resultado VARCHAR(40) NOT NULL,
    codigo_novedad VARCHAR(40),
    motivo VARCHAR(500),
    fecha_evento TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_resultados_entrega_pedido ON eventos_resultado_entrega(id_pedido);

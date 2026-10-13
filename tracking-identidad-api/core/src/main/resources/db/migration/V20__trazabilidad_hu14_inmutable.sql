ALTER TABLE historial_pedidos ADD COLUMN evento_referenciado_id BIGINT;
ALTER TABLE historial_pedidos ADD CONSTRAINT fk_historial_correccion
    FOREIGN KEY (evento_referenciado_id) REFERENCES historial_pedidos(id_historial) ON DELETE RESTRICT;
ALTER TABLE historial_pedidos ADD CONSTRAINT chk_historial_tipo_correccion_hu14 CHECK (
    (tipo_evento = 'CORRECCION_EVENTO' AND evento_referenciado_id IS NOT NULL)
    OR (tipo_evento <> 'CORRECCION_EVENTO' AND evento_referenciado_id IS NULL)
);
CREATE INDEX idx_historial_pedido_fecha_id ON historial_pedidos(id_pedido, fecha, id_historial);

CREATE OR REPLACE FUNCTION impedir_mutacion_historial_hu14() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Historial inmutable: registre un nuevo evento de corrección';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historial_append_only_hu14
    BEFORE UPDATE OR DELETE ON historial_pedidos
    FOR EACH ROW EXECUTE FUNCTION impedir_mutacion_historial_hu14();

CREATE OR REPLACE FUNCTION validar_referencia_historial_hu14() RETURNS trigger AS $$
BEGIN
    IF NEW.evento_referenciado_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM historial_pedidos original
        WHERE original.id_historial = NEW.evento_referenciado_id
          AND original.id_pedido = NEW.id_pedido
    ) THEN
        RAISE EXCEPTION 'La corrección debe referenciar un evento del mismo envío';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_referencia_historial_hu14
    BEFORE INSERT ON historial_pedidos
    FOR EACH ROW EXECUTE FUNCTION validar_referencia_historial_hu14();

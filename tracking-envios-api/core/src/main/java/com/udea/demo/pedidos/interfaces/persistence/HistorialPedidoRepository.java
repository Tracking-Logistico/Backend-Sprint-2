package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.HistorialPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface HistorialPedidoRepository extends JpaRepository<HistorialPedido, Long>, JpaSpecificationExecutor<HistorialPedido> {
    List<HistorialPedido> findByPedidoIdOrderByFechaAsc(Long pedidoId);
    java.util.Optional<HistorialPedido> findFirstByPedidoIdOrderByFechaDescIdDesc(Long pedidoId);

    Page<HistorialPedido> findByTipoEventoInOrderByFechaDescIdDesc(List<String> tiposEvento, Pageable pageable);
}

package com.udea.demo.pedidos.interfaces.persistence;

import com.udea.demo.pedidos.domain.model.CheckpointPedido;
import com.udea.demo.pedidos.domain.model.EstadoRegistroCheckpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CheckpointPedidoRepository extends JpaRepository<CheckpointPedido, Long> {
    Optional<CheckpointPedido> findByIdEventoCliente(String idEventoCliente);

    Optional<CheckpointPedido> findFirstByPedidoIdOrderByFechaEventoDescIdDesc(Long pedidoId);

    List<CheckpointPedido> findByPedidoIdAndFechaEventoIn(Long pedidoId, java.util.Collection<java.time.LocalDateTime> fechas);

    List<CheckpointPedido> findByUsuarioIdAndEstadoRegistroOrderByFechaEventoAsc(Long usuarioId,
                                                                                 EstadoRegistroCheckpoint estado);

    Page<CheckpointPedido> findByEstadoRegistroOrderByFechaRegistroDesc(EstadoRegistroCheckpoint estado,
                                                                       Pageable pageable);
}

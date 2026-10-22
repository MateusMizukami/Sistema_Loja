package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.NotaFiscal;
import com.mizukami.vendas.models.StatusNotaFiscal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    @EntityGraph(attributePaths = "itens", type = EntityGraph.EntityGraphType.LOAD)
    Optional<NotaFiscal> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusNotaFiscal status);
}

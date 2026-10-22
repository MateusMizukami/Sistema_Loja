package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByDataCriacaoDesc(Long clienteId);

    Optional<Pedido> findByClienteIdAndStatus(Long clienteId, StatusPedido status);

    List<Pedido> findByStatus(StatusPedido status);

    //Área do cliente.
    @EntityGraph(attributePaths = {"itens", "itens.produto"}, type = EntityGraph.EntityGraphType.LOAD)
    List<Pedido> findAllByClienteIdAndStatusOrderByIdAsc(Long clienteId, StatusPedido status);

    @EntityGraph(attributePaths = {"itens", "itens.produto"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    @EntityGraph(attributePaths = {"itens", "itens.produto"}, type = EntityGraph.EntityGraphType.LOAD)
    List<Pedido> findByClienteIdAndStatusNotOrderByDataCriacaoDesc(Long clienteId, StatusPedido status);

    //Painel do gerente.
    Page<Pedido> findByStatusNot(StatusPedido status, Pageable pageable);

    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    @EntityGraph(attributePaths = {"itens", "itens.produto"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Pedido> findDetalhadoById(Long id);

    long countByStatus(StatusPedido status);

    long countByStatusNot(StatusPedido status);

    long countByStatusInAndDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(
            Collection<StatusPedido> status, LocalDateTime inicio, LocalDateTime fim);

    /**
     * Troca de status: só altera se o pedido ainda estiver no status esperado.
     * Retorna 0 se outra pessoa já mudou o pedido (evita, por exemplo, devolver o estoque duas vezes).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Pedido p set p.status = :novo where p.id = :id and p.status = :atual")
    int atualizarStatus(@Param("id") Long id,
                        @Param("atual") StatusPedido atual,
                        @Param("novo") StatusPedido novo);

    /** Ids dos pedidos num status criados antes do limite (usado para expirar pedidos sem pagamento). */
    @Query("select p.id from Pedido p where p.status = :status and p.dataCriacao < :limite")
    List<Long> idsPorStatusCriadosAntesDe(@Param("status") StatusPedido status,
                                          @Param("limite") LocalDateTime limite);
}

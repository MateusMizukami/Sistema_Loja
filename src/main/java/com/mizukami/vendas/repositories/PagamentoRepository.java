package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.Pagamento;
import com.mizukami.vendas.models.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByAsaasId(String asaasId);

    Optional<Pagamento> findFirstByPedidoIdOrderByIdDesc(Long pedidoId);

    Optional<Pagamento> findFirstByPedidoIdAndStatusOrderByIdDesc(Long pedidoId, StatusPagamento status);

    List<Pagamento> findByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);

    boolean existsByPedidoId(Long pedidoId);

    boolean existsByPedidoIdAndStatus(Long pedidoId, StatusPagamento status);

    List<Pagamento> findTop50ByStatusOrderByIdAsc(StatusPagamento status);
}

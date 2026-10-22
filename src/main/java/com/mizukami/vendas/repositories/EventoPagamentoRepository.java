package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.EventoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPagamentoRepository extends JpaRepository<EventoPagamento, String> {
}

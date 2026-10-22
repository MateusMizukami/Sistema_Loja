package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.MetodoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetodoPagamentoRepository extends JpaRepository<MetodoPagamento, Long> {
    List<MetodoPagamento> findByUsuarioId(Long usuarioId);
}

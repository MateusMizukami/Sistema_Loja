package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /** Histórico do cliente, sem o carrinho. */
    public List<Pedido> historico(Long usuarioId) {
        return pedidoRepository.findByClienteIdAndStatusNotOrderByDataCriacaoDesc(usuarioId, StatusPedido.CARRINHO);
    }

    /** Pedido de outro cliente (ou o carrinho) responde como "não encontrado". */
    public Pedido buscarDoCliente(Long usuarioId, Long pedidoId) {
        return pedidoRepository.findByIdAndClienteId(pedidoId, usuarioId)
                .filter(pedido -> pedido.getStatus() != StatusPedido.CARRINHO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }
}

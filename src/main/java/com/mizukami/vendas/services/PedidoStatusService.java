package com.mizukami.vendas.services;

import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único lugar que muda o status de um pedido. Usado tanto pelo gerente quanto pelo sistema
 * (confirmação de pagamento e expiração).
 */
@Service
public class PedidoStatusService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoStatusService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    /**
     * Tenta mudar o status. Devolve false se a transição não é permitida ou se
     * outra operação mudou o pedido antes. O UPDATE é condicional ao status atual, então o estoque de
     * um cancelamento nunca é devolvido duas vezes.
     */
    @Transactional
    public boolean tentarAplicar(Long pedidoId, StatusPedido novo) {
        Pedido pedido = pedidoRepository.findDetalhadoById(pedidoId).orElse(null);
        if (pedido == null || pedido.getStatus() == StatusPedido.CARRINHO) {
            return false;
        }

        StatusPedido atual = pedido.getStatus();
        if (!atual.podeTransitarPara(novo)) {
            return false;
        }
        if (pedidoRepository.atualizarStatus(pedidoId, atual, novo) == 0) {
            return false;
        }

        if (novo == StatusPedido.CANCELADO) {
            for (ItemPedido item : pedido.getItens()) {
                produtoRepository.devolverEstoque(item.getProduto().getId(), item.getQuantidade());
            }
        }
        return true;
    }
}

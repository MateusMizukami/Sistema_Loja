package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.EstoqueInsuficienteException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Endereco;
import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.EnderecoRepository;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CheckoutService {

    private final CarrinhoService carrinhoService;
    private final EnderecoRepository enderecoRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;

    public CheckoutService(CarrinhoService carrinhoService,
                           EnderecoRepository enderecoRepository,
                           ProdutoRepository produtoRepository,
                           PedidoRepository pedidoRepository) {
        this.carrinhoService = carrinhoService;
        this.enderecoRepository = enderecoRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Transforma o carrinho em pedido. Tudo acontece em uma única transação: se qualquer item
     * estiver sem estoque, a exceção desfaz as baixas já feitas e o carrinho permanece intacto.
     */
    @Transactional
    public Pedido finalizar(Long usuarioId, Long enderecoId) {
        Pedido carrinho = carrinhoService.buscarCarrinho(usuarioId)
                .filter(c -> !c.getItens().isEmpty())
                .orElseThrow(() -> new RegraDeNegocioException("Seu carrinho está vazio."));

        // Só aceita endereço do próprio usuário
        Endereco endereco = enderecoRepository.findByIdAndUsuarioId(enderecoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado"));

        // Ordem fixa por produto, para que compras simultâneas travem as linhas na mesma ordem
        List<ItemPedido> itens = new ArrayList<>(carrinho.getItens());
        itens.sort(Comparator.comparing((ItemPedido item) -> item.getProduto().getId()));

        for (ItemPedido item : itens) {
            int alteradas = produtoRepository.baixarEstoque(item.getProduto().getId(), item.getQuantidade());
            if (alteradas == 0) {
                throw new EstoqueInsuficienteException("“" + item.getProduto().getNome()
                        + "” não tem estoque suficiente ou não está mais disponível. "
                        + "Ajuste o carrinho e tente novamente.");
            }
        }

        //mudanças futuras no perfil não alteram este pedido
        carrinho.setEnderecoEntrega(endereco.formatado());
        carrinho.setDataCriacao(LocalDateTime.now());
        carrinho.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        return pedidoRepository.save(carrinho);
    }
}

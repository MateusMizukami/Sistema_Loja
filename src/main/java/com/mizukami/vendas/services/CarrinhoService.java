package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.EstoqueInsuficienteException;
import com.mizukami.vendas.exceptions.ProdutoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * O carrinho é um Pedido com status CARRINHO. Todas as operações partem do id do usuário
 * logado, então um cliente nunca consegue mexer no carrinho de outro.
 */
@Service
public class CarrinhoService {

    public static final int QUANTIDADE_MAXIMA = 99;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarrinhoService(PedidoRepository pedidoRepository,
                           ProdutoRepository produtoRepository,
                           UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarCarrinho(Long usuarioId) {
        List<Pedido> carrinhos = pedidoRepository
                .findAllByClienteIdAndStatusOrderByIdAsc(usuarioId, StatusPedido.CARRINHO);
        return carrinhos.isEmpty() ? Optional.empty() : Optional.of(carrinhos.get(0));
    }

    @Transactional
    public void adicionar(Long usuarioId, Long produtoId, int quantidade) {
        validarQuantidade(quantidade);

        Produto produto = produtoRepository.findByIdAndAtivoTrue(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));

        Pedido carrinho = buscarCarrinho(usuarioId).orElseGet(() -> novoCarrinho(usuarioId));

        ItemPedido existente = carrinho.getItens().stream()
                .filter(item -> item.getProduto().getId().equals(produtoId))
                .findFirst()
                .orElse(null);

        int novaQuantidade = (existente == null ? 0 : existente.getQuantidade()) + quantidade;
        validarLimites(produto, novaQuantidade);

        if (existente != null) {
            existente.setQuantidade(novaQuantidade);
        } else {
            // O construtor de ItemPedido congela o preço atual do produto
            carrinho.adicionarItem(new ItemPedido(carrinho, produto, quantidade));
        }
        pedidoRepository.save(carrinho);
    }

    @Transactional
    public void alterarQuantidade(Long usuarioId, Long itemId, int quantidade) {
        validarQuantidade(quantidade);

        Pedido carrinho = buscarCarrinho(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carrinho não encontrado"));
        ItemPedido item = carrinho.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no carrinho"));

        validarLimites(item.getProduto(), quantidade);
        item.setQuantidade(quantidade);
    }

    @Transactional
    public void remover(Long usuarioId, Long itemId) {
        Pedido carrinho = buscarCarrinho(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carrinho não encontrado"));

        boolean removido = carrinho.getItens().removeIf(item -> item.getId().equals(itemId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Item não encontrado no carrinho");
        }
    }

    @Transactional
    public void esvaziar(Long usuarioId) {
        buscarCarrinho(usuarioId).ifPresent(carrinho -> carrinho.getItens().clear());
    }

    private Pedido novoCarrinho(Long usuarioId) {
        return new Pedido(usuarioRepository.getReferenceById(usuarioId), StatusPedido.CARRINHO);
    }

    private void validarQuantidade(int quantidade) {
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA) {
            throw new RegraDeNegocioException(
                    "Quantidade inválida. Informe um valor entre 1 e " + QUANTIDADE_MAXIMA + ".");
        }
    }

    private void validarLimites(Produto produto, int quantidadeTotal) {
        if (!produto.isAtivo()) {
            throw new RegraDeNegocioException("O produto “" + produto.getNome() + "” não está mais disponível.");
        }
        if (quantidadeTotal > QUANTIDADE_MAXIMA) {
            throw new RegraDeNegocioException("A quantidade máxima por produto é " + QUANTIDADE_MAXIMA + ".");
        }
        if (quantidadeTotal > produto.getEstoque()) {
            throw new EstoqueInsuficienteException("Estoque insuficiente de “" + produto.getNome()
                    + "”. Disponível: " + produto.getEstoque() + ".");
        }
    }
}

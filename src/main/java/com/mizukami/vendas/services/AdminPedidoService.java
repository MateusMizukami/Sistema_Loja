package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.PedidoResumoDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPedidoService {

    public static final int TAMANHO_PAGINA = 15;

    private final PedidoRepository pedidoRepository;
    private final PedidoStatusService pedidoStatusService;
    private final PagamentoService pagamentoService;
    private final NotaFiscalService notaFiscalService;

    public AdminPedidoService(PedidoRepository pedidoRepository,
                              PedidoStatusService pedidoStatusService,
                              PagamentoService pagamentoService,
                              NotaFiscalService notaFiscalService) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoStatusService = pedidoStatusService;
        this.pagamentoService = pagamentoService;
        this.notaFiscalService = notaFiscalService;
    }

    /** Pedidos de todos os clientes (carrinhos não aparecem), com filtro opcional por status. */
    @Transactional(readOnly = true)
    public Page<PedidoResumoDTO> listar(StatusPedido filtro, int pagina) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA, Sort.by("dataCriacao").descending());
        Page<Pedido> resultado = (filtro == null || filtro == StatusPedido.CARRINHO)
                ? pedidoRepository.findByStatusNot(StatusPedido.CARRINHO, pageable)
                : pedidoRepository.findByStatus(filtro, pageable);
        return resultado.map(PedidoResumoDTO::de);
    }

    @Transactional(readOnly = true)
    public Pedido buscar(Long id) {
        return pedidoRepository.findDetalhadoById(id)
                .filter(p -> p.getStatus() != StatusPedido.CARRINHO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    //Muda o status respeitando o fluxo e as regras de pagamento.
    public void alterarStatus(Long id, StatusPedido novo) {
        Pedido pedido = buscar(id);
        StatusPedido atual = pedido.getStatus();

        if (!atual.podeTransitarPara(novo)) {
            throw new RegraDeNegocioException("Não é possível mudar o pedido de “" + atual.getDescricao()
                    + "” para “" + novo.getDescricao() + "”.");
        }

        if (novo == StatusPedido.PAGAMENTO_APROVADO && pagamentoService.temCobranca(id)) {
            throw new RegraDeNegocioException("Este pedido tem uma cobrança no Asaas. O pagamento é "
                    + "confirmado automaticamente quando for recebido.");
        }

        if (novo == StatusPedido.CANCELADO) {
            if (notaFiscalService.temNotaAtiva(id)) {
                throw new RegraDeNegocioException("Este pedido tem nota fiscal autorizada. "
                        + "Cancele a nota fiscal antes de cancelar o pedido.");
            }
            pagamentoService.sincronizarPedido(id);
            if (pagamentoService.temPagamentoConfirmado(id)) {
                throw new RegraDeNegocioException("Este pedido já foi pago. Faça o estorno no painel do "
                        + "Asaas e, depois que o estorno for confirmado, cancele o pedido.");
            }
            pagamentoService.cancelarCobrancasPendentes(id);
        }

        if (!pedidoStatusService.tentarAplicar(id, novo)) {
            throw new RegraDeNegocioException(
                    "Este pedido foi alterado por outra pessoa. Atualize a página e tente de novo.");
        }
    }
}

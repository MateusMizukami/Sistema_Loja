package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.GatewayPagamentoException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.FormaPagamento;
import com.mizukami.vendas.models.Pagamento;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPagamento;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.PagamentoRepository;
import com.mizukami.vendas.repositories.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoGateway gateway;
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoStatusService pedidoStatusService;
    private final long expiracaoMinutos;

    public PagamentoService(PagamentoGateway gateway,
                            PagamentoRepository pagamentoRepository,
                            PedidoRepository pedidoRepository,
                            PedidoStatusService pedidoStatusService,
                            @Value("${app.pagamento.expiracao-minutos:30}") long expiracaoMinutos) {
        this.gateway = gateway;
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoStatusService = pedidoStatusService;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public long getExpiracaoMinutos() {
        return expiracaoMinutos;
    }

    public boolean disponivel() {
        return gateway.disponivel();
    }


    @Transactional(readOnly = true)
    public Optional<Pagamento> ultimoDoPedido(Long pedidoId) {
        return pagamentoRepository.findFirstByPedidoIdOrderByIdDesc(pedidoId);
    }

    @Transactional(readOnly = true)
    public boolean temCobranca(Long pedidoId) {
        return pagamentoRepository.existsByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public boolean temPagamentoConfirmado(Long pedidoId) {
        return pagamentoRepository.existsByPedidoIdAndStatus(pedidoId, StatusPagamento.CONFIRMADO);
    }

    /**
     * Cria a cobrança no Asaas para um pedido que aguarda pagamento. Se já existir uma cobrança
     * pendente da mesma forma, devolve a existente; se for de outra forma, cancela e cria a nova.
     */
    @Transactional
    public Pagamento iniciar(Long usuarioId, Long pedidoId, FormaPagamento forma) {
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraDeNegocioException("Este pedido não está aguardando pagamento.");
        }

        Usuario cliente = pedido.getCliente();
        String cpf = apenasDigitos(cliente.getCpf());
        if (cpf.length() != 11) {
            throw new RegraDeNegocioException("Cadastre seu CPF em “Minha conta” antes de pagar.");
        }
        if (!gateway.disponivel()) {
            throw new GatewayPagamentoException("O pagamento online está indisponível no momento.");
        }

        Optional<Pagamento> pendente = pagamentoRepository
                .findFirstByPedidoIdAndStatusOrderByIdDesc(pedidoId, StatusPagamento.PENDENTE);
        if (pendente.isPresent()) {
            if (pendente.get().getForma() == forma) {
                return pendente.get();
            }
            cancelarCobrancasPendentes(pedidoId);
        }

        if (cliente.getAsaasClienteId() == null || cliente.getAsaasClienteId().isBlank()) {
            cliente.setAsaasClienteId(gateway.criarCliente(
                    cliente.getNome(), cpf, cliente.getEmail(), "USUARIO-" + cliente.getId()));
        }

        BigDecimal valor = pedido.getTotal();
        PagamentoGateway.CobrancaCriada cobranca = gateway.criarCobranca(
                cliente.getAsaasClienteId(), forma, valor, LocalDate.now(),
                "Pedido #" + pedido.getId() + " - Loja Vendas", "PEDIDO-" + pedido.getId());

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setForma(forma);
        pagamento.setAsaasId(cobranca.id());
        pagamento.setValor(valor);
        pagamento.setInvoiceUrl(cobranca.invoiceUrl());
        pagamento.setExpiraEm(pedido.getDataCriacao().plusMinutes(expiracaoMinutos));

        if (forma == FormaPagamento.PIX) {
            try {
                PagamentoGateway.QrCodePix qr = gateway.obterQrCodePix(cobranca.id());
                pagamento.setPixCopiaECola(qr.copiaECola());
                pagamento.setPixQrCodeBase64(qr.imagemBase64());
            } catch (GatewayPagamentoException e) {
                cancelarSemFalhar(cobranca.id());
                throw e;
            }
        }
        return pagamentoRepository.save(pagamento);
    }

    // Consulta o Asaas e atualiza as cobranças pendentes de um pedido (botão "Já paguei").
    @Transactional
    public void verificar(Long usuarioId, Long pedidoId) {
        pedidoRepository.findByIdAndClienteId(pedidoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        sincronizarPedido(pedidoId);
    }

    @Transactional
    public void sincronizarPedido(Long pedidoId) {
        for (Pagamento pagamento : pagamentoRepository.findByPedidoIdAndStatus(pedidoId, StatusPagamento.PENDENTE)) {
            sincronizar(pagamento, gateway.consultar(pagamento.getAsaasId()));
        }
    }

    @Transactional
    public void verificarPagamento(Long pagamentoId) {
        pagamentoRepository.findById(pagamentoId)
                .filter(p -> p.getStatus() == StatusPagamento.PENDENTE)
                .ifPresent(p -> sincronizar(p, gateway.consultar(p.getAsaasId())));
    }

     //Aplica a situação informada pelo Asaas.
    @Transactional
    public void sincronizar(Pagamento pagamento, PagamentoGateway.StatusCobranca status) {
        switch (status) {
            case PAGO -> {
                if (pagamento.getStatus() == StatusPagamento.CONFIRMADO) {
                    return;
                }
                pagamento.setStatus(StatusPagamento.CONFIRMADO);
                pagamento.setConfirmadoEm(LocalDateTime.now());
                pagamentoRepository.save(pagamento);

                Long pedidoId = pagamento.getPedido().getId();
                if (!pedidoStatusService.tentarAplicar(pedidoId, StatusPedido.PAGAMENTO_APROVADO)) {
                    log.warn("Pagamento {} confirmado, mas o pedido {} não pôde ser aprovado "
                            + "(provavelmente já cancelado ou expirado). Verifique se é preciso estornar.",
                            pagamento.getAsaasId(), pedidoId);
                }
            }
            case ESTORNADO -> {
                if (pagamento.getStatus() != StatusPagamento.ESTORNADO) {
                    pagamento.setStatus(StatusPagamento.ESTORNADO);
                    pagamentoRepository.save(pagamento);
                }
            }
            case CANCELADO -> {
                if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
                    pagamento.setStatus(StatusPagamento.CANCELADO);
                    pagamentoRepository.save(pagamento);
                }
            }
            default -> {
                // PENDENTE: nada a fazer
            }
        }
    }

    /**
     * Cancela no Asaas as cobranças pendentes do pedido. Se uma delas já tiver sido paga, lança
     * exceção para o pedido não ser cancelado com dinheiro recebido.
     */
    @Transactional
    public void cancelarCobrancasPendentes(Long pedidoId) {
        for (Pagamento pagamento : pagamentoRepository.findByPedidoIdAndStatus(pedidoId, StatusPagamento.PENDENTE)) {
            if (gateway.consultar(pagamento.getAsaasId()) == PagamentoGateway.StatusCobranca.PAGO) {
                throw new RegraDeNegocioException(
                        "O pagamento deste pedido acabou de ser confirmado. Atualize a página.");
            }
            gateway.cancelar(pagamento.getAsaasId());
            pagamento.setStatus(StatusPagamento.CANCELADO);
            pagamentoRepository.save(pagamento);
        }
    }

    /**
     * Expira um pedido que não foi pago a tempo: cancela as cobranças e devolve o estoque.
     * Se descobrir que o pagamento foi feito, confirma o pedido em vez de expirá-lo.
     */
    @Transactional
    public void expirarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            return;
        }

        for (Pagamento pagamento : pagamentoRepository.findByPedidoIdAndStatus(pedidoId, StatusPagamento.PENDENTE)) {
            if (gateway.consultar(pagamento.getAsaasId()) == PagamentoGateway.StatusCobranca.PAGO) {
                sincronizar(pagamento, PagamentoGateway.StatusCobranca.PAGO);
                return;
            }
            gateway.cancelar(pagamento.getAsaasId());
            pagamento.setStatus(StatusPagamento.EXPIRADO);
            pagamentoRepository.save(pagamento);
        }

        if (pedidoStatusService.tentarAplicar(pedidoId, StatusPedido.CANCELADO)) {
            log.info("Pedido {} expirou sem pagamento e foi cancelado (estoque devolvido).", pedidoId);
        }
    }


    private void cancelarSemFalhar(String cobrancaId) {
        try {
            gateway.cancelar(cobrancaId);
        } catch (RuntimeException e) {
            log.warn("Não foi possível cancelar a cobrança {} no Asaas: {}", cobrancaId, e.getMessage());
        }
    }

    private static String apenasDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    @Transactional(readOnly = true)
    public List<Long> idsDePagamentosPendentes() {
        return pagamentoRepository.findTop50ByStatusOrderByIdAsc(StatusPagamento.PENDENTE)
                .stream().map(Pagamento::getId).toList();
    }
}

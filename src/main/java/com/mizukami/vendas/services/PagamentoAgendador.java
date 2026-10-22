package com.mizukami.vendas.services;

import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Rede de segurança do webhook: em desenvolvimento (localhost) o Asaas não consegue chamar o
 * sistema, e em qualquer ambiente um webhook pode falhar. Esta tarefa consulta periodicamente as
 * cobranças pendentes e expira os pedidos que não foram pagos a tempo.
 */
@Component
public class PagamentoAgendador {

    private static final Logger log = LoggerFactory.getLogger(PagamentoAgendador.class);

    private final PagamentoService pagamentoService;
    private final PedidoRepository pedidoRepository;

    public PagamentoAgendador(PagamentoService pagamentoService, PedidoRepository pedidoRepository) {
        this.pagamentoService = pagamentoService;
        this.pedidoRepository = pedidoRepository;
    }

    @Scheduled(initialDelayString = "${app.pagamento.intervalo-ms:30000}",
               fixedDelayString = "${app.pagamento.intervalo-ms:30000}")
    public void executar() {
        if (!pagamentoService.disponivel()) {
            return;
        }
        verificarPendentes();
        expirarPedidos();
    }

    private void verificarPendentes() {
        for (Long pagamentoId : pagamentoService.idsDePagamentosPendentes()) {
            try {
                pagamentoService.verificarPagamento(pagamentoId);
            } catch (RuntimeException e) {
                log.warn("Não foi possível verificar o pagamento {}: {}", pagamentoId, e.getMessage());
            }
        }
    }

    private void expirarPedidos() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(pagamentoService.getExpiracaoMinutos());
        for (Long pedidoId : pedidoRepository.idsPorStatusCriadosAntesDe(StatusPedido.AGUARDANDO_PAGAMENTO, limite)) {
            try {
                pagamentoService.expirarPedido(pedidoId);
            } catch (RuntimeException e) {
                log.warn("Não foi possível expirar o pedido {}: {}", pedidoId, e.getMessage());
            }
        }
    }
}

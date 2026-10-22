package com.mizukami.vendas.services;

import com.mizukami.vendas.models.EventoPagamento;
import com.mizukami.vendas.repositories.EventoPagamentoRepository;
import com.mizukami.vendas.repositories.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PagamentoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoWebhookService.class);

    private final PagamentoRepository pagamentoRepository;
    private final EventoPagamentoRepository eventoRepository;
    private final PagamentoService pagamentoService;

    public PagamentoWebhookService(PagamentoRepository pagamentoRepository,
                                   EventoPagamentoRepository eventoRepository,
                                   PagamentoService pagamentoService) {
        this.pagamentoRepository = pagamentoRepository;
        this.eventoRepository = eventoRepository;
        this.pagamentoService = pagamentoService;
    }

    /**
     * Processa um evento do Asaas. Se algo falhar, a transação é desfeita e o controller devolve erro,
     * para o Asaas reenviar o evento. O id do evento garante que repetições não sejam reprocessadas.
     */
    @Transactional
    public void processar(Map<String, Object> corpo) {
        String eventoId = texto(corpo.get("id"));
        String tipo = texto(corpo.get("event"));

        if (tipo == null || !(corpo.get("payment") instanceof Map<?, ?> cobranca)) {
            log.info("Webhook do Asaas ignorado: formato sem evento ou sem cobrança.");
            return;
        }
        if (eventoId != null && eventoRepository.existsById(eventoId)) {
            log.debug("Evento {} do Asaas já processado.", eventoId);
            return;
        }

        PagamentoGateway.StatusCobranca novo = switch (tipo) {
            case "PAYMENT_CONFIRMED", "PAYMENT_RECEIVED" -> PagamentoGateway.StatusCobranca.PAGO;
            case "PAYMENT_REFUNDED" -> PagamentoGateway.StatusCobranca.ESTORNADO;
            case "PAYMENT_DELETED" -> PagamentoGateway.StatusCobranca.CANCELADO;
            default -> null;
        };

        String asaasId = texto(cobranca.get("id"));
        if (novo != null && asaasId != null) {
            pagamentoRepository.findByAsaasId(asaasId).ifPresentOrElse(
                    pagamento -> pagamentoService.sincronizar(pagamento, novo),
                    () -> log.warn("Evento {} para cobrança desconhecida {}.", tipo, asaasId));
        }

        if (eventoId != null) {
            eventoRepository.save(new EventoPagamento(eventoId, tipo));
        }
    }

    private static String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}

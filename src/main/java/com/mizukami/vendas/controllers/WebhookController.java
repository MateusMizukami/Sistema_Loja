package com.mizukami.vendas.controllers;

import com.mizukami.vendas.services.PagamentoWebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Recebe os eventos de pagamento do Asaas. A rota é pública, então a autenticação é feita aqui:
 * o header asaas-access-token precisa ser igual ao token configurado (comparação em tempo constante).
 * Sem token configurado, nenhuma requisição é aceita.
 */
@RestController
public class WebhookController {

    private final PagamentoWebhookService webhookService;
    private final byte[] tokenEsperado;

    public WebhookController(PagamentoWebhookService webhookService,
                             @Value("${app.asaas.webhook-token:}") String token) {
        this.webhookService = webhookService;
        this.tokenEsperado = token == null ? new byte[0] : token.trim().getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/webhooks/asaas")
    public ResponseEntity<Map<String, Object>> receber(
            @RequestHeader(name = "asaas-access-token", required = false) String token,
            @RequestBody Map<String, Object> corpo) {

        if (tokenEsperado.length == 0 || token == null
                || !MessageDigest.isEqual(tokenEsperado, token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }

        // Se der erro, a exceção vira HTTP 500 e o Asaas reenvia o evento
        webhookService.processar(corpo);
        return ResponseEntity.ok(Map.of("received", true));
    }
}

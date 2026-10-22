package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.GatewayPagamentoException;
import com.mizukami.vendas.models.FormaPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integração com a API v3 do Asaas.
 * A chave vai no header access_token e nunca é registrada em log.
 */
@Component
public class AsaasGateway implements PagamentoGateway {

    private static final Logger log = LoggerFactory.getLogger(AsaasGateway.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {};

    private final RestClient client;
    private final boolean configurado;

    public AsaasGateway(@Value("${app.asaas.api-key:}") String chave,
                        @Value("${app.asaas.base-url:https://api-sandbox.asaas.com/v3}") String urlBase) {
        this.configurado = chave != null && !chave.isBlank();

        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(15));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.USER_AGENT, "LojaVendas/1.0 (Java; Spring)")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (configurado) {
            builder.defaultHeader("access_token", chave.trim());
        } else {
            log.warn("ASAAS_API_KEY não configurada: o pagamento online ficará indisponível.");
        }
        this.client = builder.build();
    }

    @Override
    public boolean disponivel() {
        return configurado;
    }

    @Override
    public String criarCliente(String nome, String cpfCnpj, String email, String referencia) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("name", nome);
        corpo.put("cpfCnpj", cpfCnpj);
        corpo.put("email", email);
        corpo.put("externalReference", referencia);
        corpo.put("notificationDisabled", true);

        String id = texto(chamar(HttpMethod.POST, "/customers", corpo).get("id"));
        return exigir(id, "o identificador do cliente");
    }

    @Override
    public CobrancaCriada criarCobranca(String clienteId, FormaPagamento forma, BigDecimal valor,
                                        LocalDate vencimento, String descricao, String referencia) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("customer", clienteId);
        corpo.put("billingType", forma.getTipoAsaas());
        corpo.put("value", valor);
        corpo.put("dueDate", vencimento.toString());
        corpo.put("description", descricao);
        corpo.put("externalReference", referencia);

        Map<String, Object> resposta = chamar(HttpMethod.POST, "/payments", corpo);
        String id = exigir(texto(resposta.get("id")), "o identificador da cobrança");
        return new CobrancaCriada(id, texto(resposta.get("invoiceUrl")));
    }

    @Override
    public QrCodePix obterQrCodePix(String cobrancaId) {
        Map<String, Object> resposta = chamar(HttpMethod.GET, "/payments/" + cobrancaId + "/pixQrCode", null);
        String copiaECola = exigir(texto(resposta.get("payload")), "o código PIX");
        String imagem = exigir(texto(resposta.get("encodedImage")), "a imagem do QR Code");
        int virgula = imagem.indexOf("base64,");
        if (virgula >= 0) {
            imagem = imagem.substring(virgula + "base64,".length());
        }
        return new QrCodePix(copiaECola, imagem);
    }

    @Override
    public StatusCobranca consultar(String cobrancaId) {
        Map<String, Object> resposta = chamar(HttpMethod.GET, "/payments/" + cobrancaId, null);
        if (Boolean.TRUE.equals(resposta.get("deleted"))) {
            return StatusCobranca.CANCELADO;
        }
        String status = texto(resposta.get("status"));
        return switch (status == null ? "" : status) {
            case "RECEIVED", "CONFIRMED", "RECEIVED_IN_CASH" -> StatusCobranca.PAGO;
            case "REFUNDED", "REFUND_REQUESTED", "REFUND_IN_PROGRESS" -> StatusCobranca.ESTORNADO;
            default -> StatusCobranca.PENDENTE;
        };
    }

    @Override
    public void cancelar(String cobrancaId) {
        try {
            chamar(HttpMethod.DELETE, "/payments/" + cobrancaId, null);
        } catch (GatewayPagamentoException e) {
            if (e.getStatus() != 404) {
                throw e;
            }
        }
    }

    private Map<String, Object> chamar(HttpMethod metodo, String caminho, Map<String, Object> corpo) {
        if (!configurado) {
            throw new GatewayPagamentoException("O pagamento online está indisponível no momento.");
        }
        try {
            RestClient.RequestBodySpec requisicao = client.method(metodo).uri(caminho);
            if (corpo != null) {
                requisicao = requisicao.contentType(MediaType.APPLICATION_JSON).body(corpo);
            }
            Map<String, Object> resposta = requisicao.retrieve().body(MAPA);
            return resposta == null ? Map.of() : resposta;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String descricao = descricaoDoErro(e);
            log.warn("Asaas respondeu HTTP {} em {} {}: {}", status, metodo, caminho, descricao);

            if (status == 401 || status == 403) {
                throw new GatewayPagamentoException(
                        "O pagamento online está indisponível (credenciais do Asaas inválidas).", status, e);
            }
            if (status == 400 && descricao != null) {
                throw new GatewayPagamentoException("O Asaas recusou a operação: " + descricao, status, e);
            }
            throw new GatewayPagamentoException(
                    "Não foi possível processar o pagamento agora. Tente novamente em instantes.", status, e);

        } catch (RestClientException e) {
            log.warn("Falha de comunicação com o Asaas em {} {}: {}", metodo, caminho, e.getMessage());
            throw new GatewayPagamentoException(
                    "Não foi possível falar com o Asaas agora. Tente novamente em instantes.", 0, e);
        }
    }

    private String descricaoDoErro(RestClientResponseException e) {
        try {
            Map<String, Object> corpo = e.getResponseBodyAs(MAPA);
            if (corpo != null && corpo.get("errors") instanceof List<?> lista && !lista.isEmpty()
                    && lista.get(0) instanceof Map<?, ?> primeiro) {
                return texto(primeiro.get("description"));
            }
        } catch (RuntimeException ignorada) {
        }
        return null;
    }

    private String exigir(String valor, String o_que) {
        if (valor == null || valor.isBlank()) {
            throw new GatewayPagamentoException("Resposta inesperada do Asaas: faltou " + o_que + ".");
        }
        return valor;
    }

    private static String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}

package com.mizukami.vendas.services;

import com.mizukami.vendas.models.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Porta de entrada para um provedor de pagamento.*/
public interface PagamentoGateway {

    boolean disponivel();

    // Cria o cliente no provedor e devolve o identificador dele.
    String criarCliente(String nome, String cpfCnpj, String email, String referencia);

    CobrancaCriada criarCobranca(String clienteId, FormaPagamento forma, BigDecimal valor,
                                 LocalDate vencimento, String descricao, String referencia);

    QrCodePix obterQrCodePix(String cobrancaId);

    StatusCobranca consultar(String cobrancaId);

    // Cancela a cobrança. Se ela já não existir.
    void cancelar(String cobrancaId);

    record CobrancaCriada(String id, String invoiceUrl) {}

    record QrCodePix(String copiaECola, String imagemBase64) {}

    // Situação da cobrança
    enum StatusCobranca { PENDENTE, PAGO, CANCELADO, ESTORNADO }
}

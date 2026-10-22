package com.mizukami.vendas.exceptions;

/** Falha na comunicação com o provedor de pagamento.*/
public class GatewayPagamentoException extends RuntimeException {

    private final int status;

    public GatewayPagamentoException(String mensagem) {
        this(mensagem, 0, null);
    }

    public GatewayPagamentoException(String mensagem, int status, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
    }

    /** Código HTTP devolvido pelo provedor (0 quando não houve resposta). */
    public int getStatus() { return status; }
}

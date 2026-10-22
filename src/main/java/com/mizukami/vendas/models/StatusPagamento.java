package com.mizukami.vendas.models;

public enum StatusPagamento {
    PENDENTE("Aguardando pagamento"),
    CONFIRMADO("Pago"),
    CANCELADO("Cancelado"),
    EXPIRADO("Expirado"),
    ESTORNADO("Estornado");

    private final String descricao;

    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
}

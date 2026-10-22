package com.mizukami.vendas.models;

public enum FormaPagamento {
    PIX("PIX", "PIX"),
    CARTAO_CREDITO("Cartão de crédito", "CREDIT_CARD");

    private final String descricao;
    private final String tipoAsaas;

    FormaPagamento(String descricao, String tipoAsaas) {
        this.descricao = descricao;
        this.tipoAsaas = tipoAsaas;
    }

    public String getDescricao() { return descricao; }

    /** Valor do campo billingType da API do Asaas. */
    public String getTipoAsaas() { return tipoAsaas; }

    /** Converte o texto do formulário. Qualquer valor desconhecido vira null. */
    public static FormaPagamento deTexto(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(texto.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

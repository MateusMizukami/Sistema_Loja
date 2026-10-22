package com.mizukami.vendas.models;

public enum StatusNotaFiscal {
    AUTORIZADA("Autorizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusNotaFiscal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
}

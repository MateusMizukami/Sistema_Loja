package com.mizukami.vendas.dto;

/** Link de paginação já montado, preservando termo, filtros e ordenação. */
public class PaginaLink {

    private final int numero;
    private final String url;
    private final boolean atual;

    public PaginaLink(int numero, String url, boolean atual) {
        this.numero = numero;
        this.url = url;
        this.atual = atual;
    }

    public int getNumero() { return numero; }
    public String getUrl() { return url; }
    public boolean isAtual() { return atual; }
}

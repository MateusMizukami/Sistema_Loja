package com.mizukami.vendas.dto;

import com.mizukami.vendas.models.Produto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTO {

    private final long totalPedidos;
    private final long aguardandoPagamento;
    private final long pedidosMes;
    private final BigDecimal vendasMes;
    private final String mesReferencia;
    private final long totalClientes;
    private final long produtosAtivos;
    private final int limiteEstoqueBaixo;
    private final List<Produto> estoqueBaixo;

    public DashboardDTO(long totalPedidos, long aguardandoPagamento, long pedidosMes, BigDecimal vendasMes,
                        String mesReferencia, long totalClientes, long produtosAtivos,
                        int limiteEstoqueBaixo, List<Produto> estoqueBaixo) {
        this.totalPedidos = totalPedidos;
        this.aguardandoPagamento = aguardandoPagamento;
        this.pedidosMes = pedidosMes;
        this.vendasMes = vendasMes;
        this.mesReferencia = mesReferencia;
        this.totalClientes = totalClientes;
        this.produtosAtivos = produtosAtivos;
        this.limiteEstoqueBaixo = limiteEstoqueBaixo;
        this.estoqueBaixo = estoqueBaixo;
    }

    public long getTotalPedidos() { return totalPedidos; }
    public long getAguardandoPagamento() { return aguardandoPagamento; }
    public long getPedidosMes() { return pedidosMes; }
    public BigDecimal getVendasMes() { return vendasMes; }
    public String getMesReferencia() { return mesReferencia; }
    public long getTotalClientes() { return totalClientes; }
    public long getProdutosAtivos() { return produtosAtivos; }
    public int getLimiteEstoqueBaixo() { return limiteEstoqueBaixo; }
    public List<Produto> getEstoqueBaixo() { return estoqueBaixo; }
}

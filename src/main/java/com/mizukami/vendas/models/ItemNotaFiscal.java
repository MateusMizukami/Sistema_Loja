package com.mizukami.vendas.models;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Item da nota. É uma cópia dos dados do pedido no momento da emissão e nunca muda depois. */
@Entity
@Table(name = "tb_itens_nota_fiscal")
public class ItemNotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_id", nullable = false)
    private NotaFiscal nota;

    @Column(nullable = false)
    private int numeroItem;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    public ItemNotaFiscal() {}

    public Long getId() { return id; }
    public NotaFiscal getNota() { return nota; }
    public void setNota(NotaFiscal nota) { this.nota = nota; }
    public int getNumeroItem() { return numeroItem; }
    public void setNumeroItem(int numeroItem) { this.numeroItem = numeroItem; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
}

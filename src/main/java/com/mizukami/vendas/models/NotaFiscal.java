package com.mizukami.vendas.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nota fiscal simulado de um pedido. Os dados de emitente, destinatário, endereço e itens são uma cópia
 * (snapshot) do momento da emissão: se o cliente mudar o cadastro depois, a nota não muda.
 */
@Entity
@Table(name = "tb_notas_fiscais",
        uniqueConstraints = @UniqueConstraint(name = "uk_nota_serie_numero", columnNames = {"serie", "numero"}))
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Um pedido tem no máximo uma nota.
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(nullable = false)
    private int serie;

    @Column(nullable = false)
    private long numero;

    @Column(nullable = false, unique = true, length = 40)
    private String codigoVerificacao;

    @Column(nullable = false, length = 60)
    private String protocolo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusNotaFiscal status = StatusNotaFiscal.AUTORIZADA;

    @Column(nullable = false)
    private boolean simulada = true;

    @Column(nullable = false)
    private LocalDateTime emitidaEm = LocalDateTime.now();

    private LocalDateTime canceladaEm;

    @Column(length = 255)
    private String motivoCancelamento;

    //cópia dos dados no momento da emissão
    @Column(nullable = false)
    private String emitenteNome;

    @Column(nullable = false, length = 30)
    private String emitenteCnpj;

    @Column(nullable = false, length = 100)
    private String naturezaOperacao;

    @Column(nullable = false, length = 100)
    private String destinatarioNome;

    @Column(nullable = false, length = 20)
    private String destinatarioCpf;

    @Column(nullable = false, length = 100)
    private String destinatarioEmail;

    @Column(nullable = false, length = 1000)
    private String enderecoEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @OneToMany(mappedBy = "nota", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemNotaFiscal> itens = new ArrayList<>();

    public NotaFiscal() {}

    public void adicionarItem(ItemNotaFiscal item) {
        item.setNota(this);
        this.itens.add(item);
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public int getSerie() { return serie; }
    public void setSerie(int serie) { this.serie = serie; }
    public long getNumero() { return numero; }
    public void setNumero(long numero) { this.numero = numero; }
    public String getCodigoVerificacao() { return codigoVerificacao; }
    public void setCodigoVerificacao(String codigoVerificacao) { this.codigoVerificacao = codigoVerificacao; }
    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }
    public StatusNotaFiscal getStatus() { return status; }
    public void setStatus(StatusNotaFiscal status) { this.status = status; }
    public boolean isSimulada() { return simulada; }
    public void setSimulada(boolean simulada) { this.simulada = simulada; }
    public LocalDateTime getEmitidaEm() { return emitidaEm; }
    public void setEmitidaEm(LocalDateTime emitidaEm) { this.emitidaEm = emitidaEm; }
    public LocalDateTime getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(LocalDateTime canceladaEm) { this.canceladaEm = canceladaEm; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
    public String getEmitenteNome() { return emitenteNome; }
    public void setEmitenteNome(String emitenteNome) { this.emitenteNome = emitenteNome; }
    public String getEmitenteCnpj() { return emitenteCnpj; }
    public void setEmitenteCnpj(String emitenteCnpj) { this.emitenteCnpj = emitenteCnpj; }
    public String getNaturezaOperacao() { return naturezaOperacao; }
    public void setNaturezaOperacao(String naturezaOperacao) { this.naturezaOperacao = naturezaOperacao; }
    public String getDestinatarioNome() { return destinatarioNome; }
    public void setDestinatarioNome(String destinatarioNome) { this.destinatarioNome = destinatarioNome; }
    public String getDestinatarioCpf() { return destinatarioCpf; }
    public void setDestinatarioCpf(String destinatarioCpf) { this.destinatarioCpf = destinatarioCpf; }
    public String getDestinatarioEmail() { return destinatarioEmail; }
    public void setDestinatarioEmail(String destinatarioEmail) { this.destinatarioEmail = destinatarioEmail; }
    public String getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(String enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public List<ItemNotaFiscal> getItens() { return itens; }
    public void setItens(List<ItemNotaFiscal> itens) { this.itens = itens; }
}

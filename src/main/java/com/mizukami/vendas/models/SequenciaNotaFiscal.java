package com.mizukami.vendas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Controla a numeração sequencial das notas de uma série.
@Entity
@Table(name = "tb_sequencias_nota_fiscal")
public class SequenciaNotaFiscal {

    @Id
    private Integer serie;

    @Column(nullable = false)
    private long ultimoNumero;

    protected SequenciaNotaFiscal() {}

    public SequenciaNotaFiscal(Integer serie) {
        this.serie = serie;
        this.ultimoNumero = 0;
    }

    // Reserva o próximo número.
    public long proximo() {
        ultimoNumero++;
        return ultimoNumero;
    }

    public Integer getSerie() { return serie; }
    public long getUltimoNumero() { return ultimoNumero; }
}

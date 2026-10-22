package com.mizukami.vendas.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Evento de webhook já processado. O id é o identificador do evento enviado pelo Asaas e
 * serve de chave de idempotência: o mesmo evento pode chegar mais de uma vez.
 */
@Entity
@Table(name = "tb_eventos_pagamento")
public class EventoPagamento {

    @Id
    @Column(length = 150)
    private String id;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime recebidoEm = LocalDateTime.now();

    public EventoPagamento() {}

    public EventoPagamento(String id, String tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public LocalDateTime getRecebidoEm() { return recebidoEm; }
}

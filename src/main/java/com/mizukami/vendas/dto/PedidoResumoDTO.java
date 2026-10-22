package com.mizukami.vendas.dto;

import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Linha das listagens do gerente. Montada dentro da transação do service porque o total
 * depende dos itens e as telas não conseguem carregá-los depois.
 */
public class PedidoResumoDTO {

    private final Long id;
    private final LocalDateTime data;
    private final Long clienteId;
    private final String clienteNome;
    private final StatusPedido status;
    private final BigDecimal total;
    private final int quantidadeItens;

    public PedidoResumoDTO(Long id, LocalDateTime data, Long clienteId, String clienteNome,
                           StatusPedido status, BigDecimal total, int quantidadeItens) {
        this.id = id;
        this.data = data;
        this.clienteId = clienteId;
        this.clienteNome = clienteNome;
        this.status = status;
        this.total = total;
        this.quantidadeItens = quantidadeItens;
    }

    public static PedidoResumoDTO de(Pedido p) {
        int unidades = 0;
        for (ItemPedido item : p.getItens()) {
            unidades += item.getQuantidade();
        }
        return new PedidoResumoDTO(p.getId(), p.getDataCriacao(), p.getCliente().getId(),
                p.getCliente().getNome(), p.getStatus(), p.getTotal(), unidades);
    }

    public Long getId() { return id; }
    public LocalDateTime getData() { return data; }
    public Long getClienteId() { return clienteId; }
    public String getClienteNome() { return clienteNome; }
    public StatusPedido getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public int getQuantidadeItens() { return quantidadeItens; }
}

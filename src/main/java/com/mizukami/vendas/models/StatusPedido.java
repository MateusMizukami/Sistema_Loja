package com.mizukami.vendas.models;

import java.util.List;

public enum StatusPedido {
    CARRINHO("Carrinho"),
    AGUARDANDO_PAGAMENTO("Aguardando pagamento"),
    PAGAMENTO_APROVADO("Pagamento aprovado"),
    EM_PREPARACAO("Em preparação"),
    ENVIADO("Enviado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Fluxo do pedido. Depois de enviado, só pode ser finalizado; finalizado e cancelado são finais. */
    public List<StatusPedido> transicoesPermitidas() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> List.of(PAGAMENTO_APROVADO, CANCELADO);
            case PAGAMENTO_APROVADO -> List.of(EM_PREPARACAO, CANCELADO);
            case EM_PREPARACAO -> List.of(ENVIADO, CANCELADO);
            case ENVIADO -> List.of(FINALIZADO);
            default -> List.of();
        };
    }

    public boolean podeTransitarPara(StatusPedido destino) {
        return transicoesPermitidas().contains(destino);
    }
}

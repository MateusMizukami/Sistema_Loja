package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    /** Soma dos itens dos pedidos nos status informados e no intervalo de datas. Pode retornar null. */
    @Query("select sum(i.precoUnitario * i.quantidade) from ItemPedido i "
            + "where i.pedido.status in :status "
            + "and i.pedido.dataCriacao >= :inicio and i.pedido.dataCriacao < :fim")
    BigDecimal somarVendas(@Param("status") Collection<StatusPedido> status,
                           @Param("inicio") LocalDateTime inicio,
                           @Param("fim") LocalDateTime fim);
}

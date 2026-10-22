package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.DashboardDTO;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.repositories.ItemPedidoRepository;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    public static final int LIMITE_ESTOQUE_BAIXO = 5;

    /** Pedidos com pagamento confirmado: contam como venda. */
    private static final List<StatusPedido> VENDAS_CONFIRMADAS = List.of(
            StatusPedido.PAGAMENTO_APROVADO, StatusPedido.EM_PREPARACAO,
            StatusPedido.ENVIADO, StatusPedido.FINALIZADO);

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(PedidoRepository pedidoRepository,
                            ItemPedidoRepository itemPedidoRepository,
                            ProdutoRepository produtoRepository,
                            UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public DashboardDTO montar() {
        YearMonth mes = YearMonth.now();
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fim = mes.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal vendas = itemPedidoRepository.somarVendas(VENDAS_CONFIRMADAS, inicio, fim);

        return new DashboardDTO(
                pedidoRepository.countByStatusNot(StatusPedido.CARRINHO),
                pedidoRepository.countByStatus(StatusPedido.AGUARDANDO_PAGAMENTO),
                pedidoRepository.countByStatusInAndDataCriacaoGreaterThanEqualAndDataCriacaoLessThan(
                        VENDAS_CONFIRMADAS, inicio, fim),
                vendas == null ? BigDecimal.ZERO : vendas,
                mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))),
                usuarioRepository.countByPerfil(Perfil.ROLE_USER),
                produtoRepository.count(ProdutoSpecifications.ativo()),
                LIMITE_ESTOQUE_BAIXO,
                produtoRepository.findByAtivoTrueAndEstoqueLessThanEqualOrderByEstoqueAscNomeAsc(LIMITE_ESTOQUE_BAIXO));
    }
}

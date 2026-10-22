package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.PedidoResumoDTO;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.services.AdminPedidoService;
import com.mizukami.vendas.services.NotaFiscalService;
import com.mizukami.vendas.services.PagamentoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/pedidos")
public class AdminPedidoController {

    private final AdminPedidoService pedidoService;
    private final PagamentoService pagamentoService;
    private final NotaFiscalService notaFiscalService;

    public AdminPedidoController(AdminPedidoService pedidoService, PagamentoService pagamentoService,
                                 NotaFiscalService notaFiscalService) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
        this.notaFiscalService = notaFiscalService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "status", required = false) String status,
                         @RequestParam(name = "page", defaultValue = "0") int pagina,
                         Model model) {
        StatusPedido filtro = converterStatus(status);
        Page<PedidoResumoDTO> resultado = pedidoService.listar(filtro, pagina);

        List<StatusPedido> opcoes = Arrays.stream(StatusPedido.values())
                .filter(s -> s != StatusPedido.CARRINHO)
                .toList();

        model.addAttribute("pagina", resultado);
        model.addAttribute("pedidos", resultado.getContent());
        model.addAttribute("statusOpcoes", opcoes);
        model.addAttribute("statusSel", filtro == null || filtro == StatusPedido.CARRINHO ? "" : filtro.name());
        return "admin/pedidos";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable("id") Long id, Model model) {
        Pedido pedido = pedidoService.buscar(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("pagamento", pagamentoService.ultimoDoPedido(id).orElse(null));
        model.addAttribute("nota", notaFiscalService.buscarPorPedido(id).orElse(null));
        model.addAttribute("pendenciasNota", notaFiscalService.pendencias(pedido));
        model.addAttribute("prazoCancelamentoNota", notaFiscalService.getPrazoCancelamentoHoras());
        return "admin/pedido";
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable("id") Long id,
                                @RequestParam("novoStatus") String novoStatus,
                                RedirectAttributes redirect) {
        StatusPedido novo = converterStatus(novoStatus);
        if (novo == null) {
            redirect.addFlashAttribute("erro", "Status inválido.");
            return "redirect:/admin/pedidos/" + id;
        }
        try {
            pedidoService.alterarStatus(id, novo);
            redirect.addFlashAttribute("sucesso", novo == StatusPedido.CANCELADO
                    ? "Pedido cancelado e estoque devolvido."
                    : "Status atualizado para “" + novo.getDescricao() + "”.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }

    /** Aceita só nomes válidos do enum; qualquer outra coisa vira "sem filtro". */
    private StatusPedido converterStatus(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return StatusPedido.valueOf(texto.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

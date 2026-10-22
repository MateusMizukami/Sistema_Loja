package com.mizukami.vendas.controllers;

import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.NotaFiscalService;
import com.mizukami.vendas.services.PagamentoService;
import com.mizukami.vendas.services.PedidoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;
    private final NotaFiscalService notaFiscalService;

    public PedidoController(PedidoService pedidoService, PagamentoService pagamentoService,
                            NotaFiscalService notaFiscalService) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
        this.notaFiscalService = notaFiscalService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("pedidos", pedidoService.historico(usuario.getId()));
        return "pedidos";
    }

    @GetMapping("/{id}")
    public String detalhe(@AuthenticationPrincipal UsuarioDetails usuario,
                          @PathVariable("id") Long id, Model model) {
        Pedido pedido = pedidoService.buscarDoCliente(usuario.getId(), id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("pagamento", pagamentoService.ultimoDoPedido(id).orElse(null));
        model.addAttribute("expiracaoMinutos", pagamentoService.getExpiracaoMinutos());
        model.addAttribute("nota", notaFiscalService.buscarPorPedido(id).orElse(null));
        return "pedido";
    }
}

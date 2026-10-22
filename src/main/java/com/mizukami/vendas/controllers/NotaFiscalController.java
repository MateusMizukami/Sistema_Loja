package com.mizukami.vendas.controllers;

import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.NotaFiscalService;
import com.mizukami.vendas.services.PedidoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Nota fiscal vista pelo cliente: só do próprio pedido. */
@Controller
public class NotaFiscalController {

    private final NotaFiscalService notaFiscalService;
    private final PedidoService pedidoService;

    public NotaFiscalController(NotaFiscalService notaFiscalService, PedidoService pedidoService) {
        this.notaFiscalService = notaFiscalService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pedidos/{pedidoId}/nota-fiscal")
    public String ver(@AuthenticationPrincipal UsuarioDetails usuario,
                      @PathVariable("pedidoId") Long pedidoId, Model model) {
        pedidoService.buscarDoCliente(usuario.getId(), pedidoId);

        model.addAttribute("nota", notaFiscalService.buscarObrigatoria(pedidoId));
        model.addAttribute("voltar", "/pedidos/" + pedidoId);
        return "nota-fiscal";
    }
}

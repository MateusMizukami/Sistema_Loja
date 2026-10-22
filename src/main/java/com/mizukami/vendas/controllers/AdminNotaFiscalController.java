package com.mizukami.vendas.controllers;

import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.NotaFiscal;
import com.mizukami.vendas.services.NotaFiscalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pedidos/{pedidoId}/nota-fiscal")
public class AdminNotaFiscalController {

    private final NotaFiscalService notaFiscalService;

    public AdminNotaFiscalController(NotaFiscalService notaFiscalService) {
        this.notaFiscalService = notaFiscalService;
    }

    @GetMapping
    public String ver(@PathVariable("pedidoId") Long pedidoId, Model model) {
        NotaFiscal nota = notaFiscalService.buscarObrigatoria(pedidoId);
        model.addAttribute("nota", nota);
        model.addAttribute("voltar", "/admin/pedidos/" + pedidoId);
        return "nota-fiscal";
    }

    @PostMapping("/emitir")
    public String emitir(@PathVariable("pedidoId") Long pedidoId, RedirectAttributes redirect) {
        try {
            NotaFiscal nota = notaFiscalService.emitir(pedidoId);
            redirect.addFlashAttribute("sucesso", "Nota fiscal nº " + nota.getNumero() + " emitida com sucesso.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/pedidos/" + pedidoId;
    }

    @PostMapping("/cancelar")
    public String cancelar(@PathVariable("pedidoId") Long pedidoId,
                           @RequestParam(name = "justificativa", required = false) String justificativa,
                           RedirectAttributes redirect) {
        try {
            notaFiscalService.cancelar(pedidoId, justificativa);
            redirect.addFlashAttribute("sucesso", "Nota fiscal cancelada.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/pedidos/" + pedidoId;
    }
}

package com.mizukami.vendas.controllers;

import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.CarrinhoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @GetMapping
    public String ver(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("carrinho", carrinhoService.buscarCarrinho(usuario.getId()).orElse(null));
        return "carrinho";
    }

    @PostMapping("/adicionar")
    public String adicionar(@AuthenticationPrincipal UsuarioDetails usuario,
                            @RequestParam("produtoId") Long produtoId,
                            @RequestParam(name = "quantidade", defaultValue = "1") int quantidade,
                            RedirectAttributes redirect) {
        try {
            carrinhoService.adicionar(usuario.getId(), produtoId, quantidade);
            redirect.addFlashAttribute("sucesso", "Produto adicionado ao carrinho.");
            return "redirect:/carrinho";
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/produtos/" + produtoId;
        }
    }

    @PostMapping("/itens/{itemId}/quantidade")
    public String alterarQuantidade(@AuthenticationPrincipal UsuarioDetails usuario,
                                    @PathVariable("itemId") Long itemId,
                                    @RequestParam("quantidade") int quantidade,
                                    RedirectAttributes redirect) {
        try {
            carrinhoService.alterarQuantidade(usuario.getId(), itemId, quantidade);
            redirect.addFlashAttribute("sucesso", "Quantidade atualizada.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/carrinho";
    }

    @PostMapping("/itens/{itemId}/remover")
    public String remover(@AuthenticationPrincipal UsuarioDetails usuario,
                          @PathVariable("itemId") Long itemId,
                          RedirectAttributes redirect) {
        carrinhoService.remover(usuario.getId(), itemId);
        redirect.addFlashAttribute("sucesso", "Item removido do carrinho.");
        return "redirect:/carrinho";
    }

    @PostMapping("/esvaziar")
    public String esvaziar(@AuthenticationPrincipal UsuarioDetails usuario, RedirectAttributes redirect) {
        carrinhoService.esvaziar(usuario.getId());
        redirect.addFlashAttribute("sucesso", "Carrinho esvaziado.");
        return "redirect:/carrinho";
    }
}

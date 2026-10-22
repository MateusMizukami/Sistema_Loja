package com.mizukami.vendas.controllers;

import com.mizukami.vendas.services.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable("id") Long id, Model model) {
        // Lança ProdutoNaoEncontradoException (404) se não existir ou estiver inativo
        model.addAttribute("produto", produtoService.buscarAtivoPorId(id));
        return "produto";
    }
}

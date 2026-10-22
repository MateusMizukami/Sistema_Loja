package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.PaginaLink;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.services.CategoriaService;
import com.mizukami.vendas.services.ProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private static final int TAMANHO_MAXIMO_TERMO = 100;

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    public HomeController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    /** Home e busca compartilham a mesma tela: a home é a busca sem filtros. */
    @GetMapping({"/", "/busca"})
    public String catalogo(@RequestParam(name = "termo", required = false) String termo,
                           @RequestParam(name = "categoria", required = false) Long categoriaId,
                           @RequestParam(name = "min", required = false) BigDecimal min,
                           @RequestParam(name = "max", required = false) BigDecimal max,
                           @RequestParam(name = "ordem", required = false) String ordem,
                           @RequestParam(name = "page", defaultValue = "0") int pagina,
                           Model model) {

        String termoLimpo = normalizarTermo(termo);
        BigDecimal minimo = (min != null && min.signum() >= 0) ? min : null;
        BigDecimal maximo = (max != null && max.signum() >= 0) ? max : null;

        if (minimo != null && maximo != null && minimo.compareTo(maximo) > 0) {
            BigDecimal troca = minimo;
            minimo = maximo;
            maximo = troca;
            model.addAttribute("aviso", "Os valores de preço estavam invertidos e foram ajustados.");
        }

        String ordemValida = ProdutoService.normalizarOrdem(ordem);

        Page<Produto> resultado = produtoService.buscar(termoLimpo, categoriaId, minimo, maximo, ordemValida, pagina);

        model.addAttribute("resultado", resultado);
        model.addAttribute("produtos", resultado.getContent());
        model.addAttribute("categorias", categoriaService.listarOrdenadas());

        // Valores para preencher o formulário de filtros
        model.addAttribute("termo", termoLimpo);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("min", minimo);
        model.addAttribute("max", maximo);
        model.addAttribute("ordem", ordemValida);
        model.addAttribute("filtrosAtivos",
                termoLimpo != null || categoriaId != null || minimo != null || maximo != null);

        int atual = resultado.getNumber();
        int total = resultado.getTotalPages();
        List<PaginaLink> links = new ArrayList<>();
        for (int i = Math.max(0, atual - 2); i <= Math.min(total - 1, atual + 2); i++) {
            links.add(new PaginaLink(i + 1,
                    montarUrl(termoLimpo, categoriaId, minimo, maximo, ordemValida, i), i == atual));
        }
        model.addAttribute("links", links);
        model.addAttribute("urlAnterior", atual > 0
                ? montarUrl(termoLimpo, categoriaId, minimo, maximo, ordemValida, atual - 1) : null);
        model.addAttribute("urlProxima", resultado.hasNext()
                ? montarUrl(termoLimpo, categoriaId, minimo, maximo, ordemValida, atual + 1) : null);

        return "index";
    }

    private String normalizarTermo(String termo) {
        if (termo == null) {
            return null;
        }
        String limpo = termo.trim();
        if (limpo.isEmpty()) {
            return null;
        }
        return limpo.length() > TAMANHO_MAXIMO_TERMO ? limpo.substring(0, TAMANHO_MAXIMO_TERMO) : limpo;
    }

    private String montarUrl(String termo, Long categoriaId, BigDecimal min, BigDecimal max,
                             String ordem, int pagina) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/busca");
        Map<String, Object> variaveis = new HashMap<>();

        adicionar(builder, variaveis, "termo", termo);
        adicionar(builder, variaveis, "categoria", categoriaId);
        adicionar(builder, variaveis, "min", min == null ? null : min.toPlainString());
        adicionar(builder, variaveis, "max", max == null ? null : max.toPlainString());
        if (!ProdutoService.ORDEM_PADRAO.equals(ordem)) {
            adicionar(builder, variaveis, "ordem", ordem);
        }
        if (pagina > 0) {
            adicionar(builder, variaveis, "page", pagina);
        }
        return builder.encode().buildAndExpand(variaveis).toUriString();
    }

    private void adicionar(UriComponentsBuilder builder, Map<String, Object> variaveis,
                           String nome, Object valor) {
        if (valor != null) {
            builder.queryParam(nome, "{" + nome + "}");
            variaveis.put(nome, valor);
        }
    }
}

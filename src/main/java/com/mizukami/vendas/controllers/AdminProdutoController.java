package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.ProdutoFormDTO;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.services.AdminProdutoService;
import com.mizukami.vendas.services.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/produtos")
public class AdminProdutoController {

    private final AdminProdutoService produtoService;
    private final CategoriaService categoriaService;

    public AdminProdutoController(AdminProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "termo", required = false) String termo,
                         @RequestParam(name = "page", defaultValue = "0") int pagina,
                         Model model) {
        Page<Produto> resultado = produtoService.listar(termo, pagina);
        model.addAttribute("pagina", resultado);
        model.addAttribute("produtos", resultado.getContent());
        model.addAttribute("termo", termo == null ? "" : termo.trim());
        return "admin/produtos";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produto", new ProdutoFormDTO());
        prepararFormulario(model, "Novo produto", "/admin/produtos/novo", null, false);
        return "admin/produto-form";
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("produto") ProdutoFormDTO produto,
                        BindingResult result,
                        @RequestParam(name = "imagem", required = false) MultipartFile imagem,
                        Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                produtoService.criar(produto, imagem);
                redirect.addFlashAttribute("sucesso", "Produto cadastrado.");
                return "redirect:/admin/produtos";
            } catch (RegraDeNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        }
        prepararFormulario(model, "Novo produto", "/admin/produtos/novo", null, false);
        return "admin/produto-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model) {
        Produto existente = produtoService.buscar(id);
        model.addAttribute("produto", ProdutoFormDTO.de(existente));
        prepararFormulario(model, "Editar produto", "/admin/produtos/" + id + "/editar",
                existente.getUrlImagem(), true);
        return "admin/produto-form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("produto") ProdutoFormDTO produto,
                            BindingResult result,
                            @RequestParam(name = "imagem", required = false) MultipartFile imagem,
                            Model model,
                            RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                produtoService.atualizar(id, produto, imagem);
                redirect.addFlashAttribute("sucesso", "Produto atualizado.");
                return "redirect:/admin/produtos";
            } catch (RegraDeNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        }
        prepararFormulario(model, "Editar produto", "/admin/produtos/" + id + "/editar",
                produtoService.buscar(id).getUrlImagem(), true);
        return "admin/produto-form";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        produtoService.alterarAtivo(id, false);
        redirect.addFlashAttribute("sucesso", "Produto desativado. Ele não aparece mais no catálogo.");
        return "redirect:/admin/produtos";
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        produtoService.alterarAtivo(id, true);
        redirect.addFlashAttribute("sucesso", "Produto reativado.");
        return "redirect:/admin/produtos";
    }

    private void prepararFormulario(Model model, String titulo, String acao, String imagemAtual, boolean edicao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        model.addAttribute("imagemAtual", imagemAtual);
        model.addAttribute("edicao", edicao);
        model.addAttribute("categorias", categoriaService.listarOrdenadas());
    }
}

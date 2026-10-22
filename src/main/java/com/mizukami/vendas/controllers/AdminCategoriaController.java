package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.CategoriaDTO;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.services.AdminCategoriaService;
import com.mizukami.vendas.services.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categorias")
public class AdminCategoriaController {

    private final AdminCategoriaService adminService;
    private final CategoriaService categoriaService;

    public AdminCategoriaController(AdminCategoriaService adminService, CategoriaService categoriaService) {
        this.adminService = adminService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Categoria> categorias = categoriaService.listarOrdenadas();
        model.addAttribute("categorias", categorias);
        model.addAttribute("contagens", adminService.contarProdutos(categorias));
        return "admin/categorias";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("categoria", new CategoriaDTO());
        prepararFormulario(model, "Nova categoria", "/admin/categorias/nova");
        return "admin/categoria-form";
    }

    @PostMapping("/nova")
    public String criar(@Valid @ModelAttribute("categoria") CategoriaDTO categoria,
                        BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                adminService.criar(categoria);
                redirect.addFlashAttribute("sucesso", "Categoria cadastrada.");
                return "redirect:/admin/categorias";
            } catch (RegraDeNegocioException e) {
                result.rejectValue("nome", "nome.duplicado", e.getMessage());
            }
        }
        prepararFormulario(model, "Nova categoria", "/admin/categorias/nova");
        return "admin/categoria-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("categoria", new CategoriaDTO(adminService.buscar(id).getNome()));
        prepararFormulario(model, "Editar categoria", "/admin/categorias/" + id + "/editar");
        return "admin/categoria-form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("categoria") CategoriaDTO categoria,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                adminService.atualizar(id, categoria);
                redirect.addFlashAttribute("sucesso", "Categoria atualizada.");
                return "redirect:/admin/categorias";
            } catch (RegraDeNegocioException e) {
                result.rejectValue("nome", "nome.duplicado", e.getMessage());
            }
        }
        prepararFormulario(model, "Editar categoria", "/admin/categorias/" + id + "/editar");
        return "admin/categoria-form";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirect) {
        try {
            adminService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Categoria excluída.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    private void prepararFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
    }
}

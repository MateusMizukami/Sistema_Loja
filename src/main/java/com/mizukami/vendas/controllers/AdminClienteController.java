package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.ClienteAdminDTO;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.AdminClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clientes")
public class AdminClienteController {

    private final AdminClienteService clienteService;

    public AdminClienteController(AdminClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(@RequestParam(name = "termo", required = false) String termo,
                         @RequestParam(name = "page", defaultValue = "0") int pagina,
                         Model model) {
        Page<Usuario> resultado = clienteService.listar(termo, pagina);
        model.addAttribute("pagina", resultado);
        model.addAttribute("clientes", resultado.getContent());
        model.addAttribute("termo", termo == null ? "" : termo.trim());
        return "admin/clientes";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable("id") Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("pedidos", clienteService.pedidosDoCliente(id));
        return "admin/cliente";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model) {
        Usuario cliente = clienteService.buscar(id);
        model.addAttribute("clienteForm", new ClienteAdminDTO(cliente.getNome(), cliente.getTelefone(), cliente.getCpf()));
        model.addAttribute("clienteId", id);
        model.addAttribute("clienteEmail", cliente.getEmail());
        return "admin/cliente-form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("clienteForm") ClienteAdminDTO form,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("clienteId", id);
            model.addAttribute("clienteEmail", clienteService.buscar(id).getEmail());
            return "admin/cliente-form";
        }
        clienteService.atualizar(id, form);
        redirect.addFlashAttribute("sucesso", "Dados do cliente atualizados.");
        return "redirect:/admin/clientes/" + id;
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable("id") Long id,
                            @AuthenticationPrincipal UsuarioDetails admin,
                            RedirectAttributes redirect) {
        return alterarStatus(id, false, admin, redirect, "Conta desativada. O cliente não consegue mais entrar.");
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable("id") Long id,
                         @AuthenticationPrincipal UsuarioDetails admin,
                         RedirectAttributes redirect) {
        return alterarStatus(id, true, admin, redirect, "Conta reativada.");
    }

    private String alterarStatus(Long id, boolean ativo, UsuarioDetails admin,
                                 RedirectAttributes redirect, String mensagemOk) {
        try {
            clienteService.alterarAtivo(id, ativo, admin.getId());
            redirect.addFlashAttribute("sucesso", mensagemOk);
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/clientes/" + id;
    }
}

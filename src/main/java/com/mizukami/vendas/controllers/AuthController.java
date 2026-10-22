package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.CadastroDTO;
import com.mizukami.vendas.exceptions.EmailJaCadastradoException;
import com.mizukami.vendas.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UserDetails logado) {
        if (logado != null) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/cadastro")
    public String exibirCadastro(@AuthenticationPrincipal UserDetails logado, Model model) {
        if (logado != null) {
            return "redirect:/";
        }
        model.addAttribute("cadastro", new CadastroDTO());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String processarCadastro(@Valid @ModelAttribute("cadastro") CadastroDTO cadastro,
                                    BindingResult result) {

        if (!result.hasFieldErrors("senha") && !result.hasFieldErrors("confirmarSenha")
                && !cadastro.getSenha().equals(cadastro.getConfirmarSenha())) {
            result.rejectValue("confirmarSenha", "senhas.diferentes", "As senhas não conferem");
        }

        if (result.hasErrors()) {
            return "cadastro";
        }

        try {
            usuarioService.cadastrar(cadastro);
        } catch (EmailJaCadastradoException e) {
            result.rejectValue("email", "email.duplicado", "Este e-mail já está em uso");
            return "cadastro";
        }

        return "redirect:/login?cadastrado";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}

package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.PerfilDTO;
import com.mizukami.vendas.dto.SenhaDTO;
import com.mizukami.vendas.exceptions.SenhaAtualIncorretaException;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String dados(@AuthenticationPrincipal UsuarioDetails logado, Model model) {
        Usuario usuario = usuarioService.buscarPorId(logado.getId());
        model.addAttribute("perfil", new PerfilDTO(usuario.getNome(), usuario.getTelefone(), usuario.getCpf()));
        model.addAttribute("email", usuario.getEmail());
        return "perfil/dados";
    }

    @PostMapping
    public String atualizar(@AuthenticationPrincipal UsuarioDetails logado,
                            @Valid @ModelAttribute("perfil") PerfilDTO perfil,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("email", logado.getUsername());
            return "perfil/dados";
        }
        usuarioService.atualizarPerfil(logado.getId(), perfil);
        logado.setNome(perfil.getNome().trim()); // atualiza o nome mostrado no header

        redirect.addFlashAttribute("sucesso", "Dados atualizados com sucesso.");
        return "redirect:/perfil";
    }

    @GetMapping("/senha")
    public String formularioSenha(Model model) {
        model.addAttribute("senhaForm", new SenhaDTO());
        return "perfil/senha";
    }

    @PostMapping("/senha")
    public String alterarSenha(@AuthenticationPrincipal UsuarioDetails logado,
                               @Valid @ModelAttribute("senhaForm") SenhaDTO form,
                               BindingResult result,
                               RedirectAttributes redirect) {

        if (!result.hasFieldErrors("novaSenha") && !result.hasFieldErrors("confirmarNovaSenha")
                && !form.getNovaSenha().equals(form.getConfirmarNovaSenha())) {
            result.rejectValue("confirmarNovaSenha", "senhas.diferentes", "As senhas não conferem");
        }
        if (!result.hasFieldErrors("novaSenha") && !result.hasFieldErrors("senhaAtual")
                && form.getNovaSenha().equals(form.getSenhaAtual())) {
            result.rejectValue("novaSenha", "senha.igual", "A nova senha deve ser diferente da atual");
        }
        if (result.hasErrors()) {
            return "perfil/senha";
        }

        try {
            usuarioService.alterarSenha(logado.getId(), form.getSenhaAtual(), form.getNovaSenha());
        } catch (SenhaAtualIncorretaException e) {
            result.rejectValue("senhaAtual", "senha.incorreta", "Senha atual incorreta");
            return "perfil/senha";
        }

        redirect.addFlashAttribute("sucesso", "Senha alterada com sucesso.");
        return "redirect:/perfil";
    }
}

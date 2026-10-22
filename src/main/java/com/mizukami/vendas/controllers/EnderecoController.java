package com.mizukami.vendas.controllers;

import com.mizukami.vendas.dto.EnderecoDTO;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.EnderecoService;
import jakarta.validation.Valid;
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
@RequestMapping("/perfil/enderecos")
public class EnderecoController {

    private static final String RETORNO_CHECKOUT = "checkout";

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioDetails usuario, Model model) {
        model.addAttribute("enderecos", enderecoService.listar(usuario.getId()));
        return "perfil/enderecos";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(name = "retorno", required = false) String retorno, Model model) {
        prepararFormulario(model, new EnderecoDTO(), "Novo endereço", "/perfil/enderecos/novo", retorno);
        return "perfil/endereco-form";
    }

    @PostMapping("/novo")
    public String criar(@AuthenticationPrincipal UsuarioDetails usuario,
                        @Valid @ModelAttribute("endereco") EnderecoDTO endereco,
                        BindingResult result,
                        @RequestParam(name = "retorno", required = false) String retorno,
                        Model model,
                        RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, null, "Novo endereço", "/perfil/enderecos/novo", retorno);
            return "perfil/endereco-form";
        }
        try {
            enderecoService.criar(usuario.getId(), endereco);
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/perfil/enderecos";
        }
        redirect.addFlashAttribute("sucesso", "Endereço cadastrado.");
        return RETORNO_CHECKOUT.equals(retorno) ? "redirect:/checkout" : "redirect:/perfil/enderecos";
    }

    @GetMapping("/{id}/editar")
    public String editar(@AuthenticationPrincipal UsuarioDetails usuario,
                         @PathVariable("id") Long id, Model model) {
        EnderecoDTO dto = EnderecoDTO.de(enderecoService.buscar(usuario.getId(), id));
        prepararFormulario(model, dto, "Editar endereço", "/perfil/enderecos/" + id + "/editar", null);
        return "perfil/endereco-form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@AuthenticationPrincipal UsuarioDetails usuario,
                            @PathVariable("id") Long id,
                            @Valid @ModelAttribute("endereco") EnderecoDTO endereco,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            prepararFormulario(model, null, "Editar endereço", "/perfil/enderecos/" + id + "/editar", null);
            return "perfil/endereco-form";
        }
        enderecoService.atualizar(usuario.getId(), id, endereco);
        redirect.addFlashAttribute("sucesso", "Endereço atualizado.");
        return "redirect:/perfil/enderecos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@AuthenticationPrincipal UsuarioDetails usuario,
                          @PathVariable("id") Long id, RedirectAttributes redirect) {
        enderecoService.excluir(usuario.getId(), id);
        redirect.addFlashAttribute("sucesso", "Endereço excluído.");
        return "redirect:/perfil/enderecos";
    }

    /** Se "dto" for nulo, mantém o objeto "endereco" já vinculado ao formulário (com os erros). */
    private void prepararFormulario(Model model, EnderecoDTO dto, String titulo, String acao, String retorno) {
        if (dto != null) {
            model.addAttribute("endereco", dto);
        }
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        // Só aceita o valor conhecido, evitando redirecionamento para endereços arbitrários
        model.addAttribute("retorno", RETORNO_CHECKOUT.equals(retorno) ? RETORNO_CHECKOUT : null);
    }
}

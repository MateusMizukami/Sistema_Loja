package com.mizukami.vendas.controllers;

import com.mizukami.vendas.exceptions.GatewayPagamentoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.FormaPagamento;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.CarrinhoService;
import com.mizukami.vendas.services.CheckoutService;
import com.mizukami.vendas.services.EnderecoService;
import com.mizukami.vendas.services.PagamentoService;
import com.mizukami.vendas.services.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CarrinhoService carrinhoService;
    private final CheckoutService checkoutService;
    private final EnderecoService enderecoService;
    private final PagamentoService pagamentoService;
    private final UsuarioService usuarioService;

    public CheckoutController(CarrinhoService carrinhoService,
                              CheckoutService checkoutService,
                              EnderecoService enderecoService,
                              PagamentoService pagamentoService,
                              UsuarioService usuarioService) {
        this.carrinhoService = carrinhoService;
        this.checkoutService = checkoutService;
        this.enderecoService = enderecoService;
        this.pagamentoService = pagamentoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String resumo(@AuthenticationPrincipal UsuarioDetails usuario,
                         Model model, RedirectAttributes redirect) {
        Optional<Pedido> carrinho = carrinhoService.buscarCarrinho(usuario.getId())
                .filter(c -> !c.getItens().isEmpty());

        if (carrinho.isEmpty()) {
            redirect.addFlashAttribute("erro", "Seu carrinho está vazio.");
            return "redirect:/carrinho";
        }

        model.addAttribute("carrinho", carrinho.get());
        model.addAttribute("enderecos", enderecoService.listar(usuario.getId()));
        model.addAttribute("cpfCadastrado", temCpf(usuario.getId()));
        model.addAttribute("pagamentoDisponivel", pagamentoService.disponivel());
        model.addAttribute("expiracaoMinutos", pagamentoService.getExpiracaoMinutos());
        return "checkout";
    }

    @PostMapping
    public String finalizar(@AuthenticationPrincipal UsuarioDetails usuario,
                            @RequestParam(name = "enderecoId", required = false) Long enderecoId,
                            @RequestParam(name = "forma", required = false) String forma,
                            RedirectAttributes redirect) {
        if (enderecoId == null) {
            redirect.addFlashAttribute("erro", "Selecione um endereço de entrega.");
            return "redirect:/checkout";
        }
        FormaPagamento escolhida = FormaPagamento.deTexto(forma);
        if (escolhida == null) {
            redirect.addFlashAttribute("erro", "Escolha a forma de pagamento.");
            return "redirect:/checkout";
        }
        // Verificado antes de criar o pedido, para não baixar o estoque à toa
        if (!temCpf(usuario.getId())) {
            redirect.addFlashAttribute("erro", "Cadastre seu CPF em “Minha conta” para pagar.");
            return "redirect:/checkout";
        }
        if (!pagamentoService.disponivel()) {
            redirect.addFlashAttribute("erro", "O pagamento online está indisponível no momento.");
            return "redirect:/checkout";
        }

        Pedido pedido;
        try {
            pedido = checkoutService.finalizar(usuario.getId(), enderecoId);
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/carrinho";
        }

        try {
            pagamentoService.iniciar(usuario.getId(), pedido.getId(), escolhida);
            return "redirect:/pedidos/" + pedido.getId() + "/pagamento";
        } catch (RegraDeNegocioException | GatewayPagamentoException e) {
            // O pedido já existe (e reserva o estoque): o cliente pode tentar pagar de novo pela tela do pedido
            redirect.addFlashAttribute("erro", "O pedido #" + pedido.getId() + " foi criado, mas não foi possível "
                    + "gerar o pagamento: " + e.getMessage() + " Tente novamente abaixo.");
            return "redirect:/pedidos/" + pedido.getId();
        }
    }

    private boolean temCpf(Long usuarioId) {
        String cpf = usuarioService.buscarPorId(usuarioId).getCpf();
        return cpf != null && cpf.replaceAll("\\D", "").length() == 11;
    }
}

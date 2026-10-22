package com.mizukami.vendas.controllers;

import com.mizukami.vendas.exceptions.GatewayPagamentoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.FormaPagamento;
import com.mizukami.vendas.models.Pagamento;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.security.UsuarioDetails;
import com.mizukami.vendas.services.PagamentoService;
import com.mizukami.vendas.services.PedidoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/pedidos/{pedidoId}")
public class PagamentoController {

    private final PagamentoService pagamentoService;
    private final PedidoService pedidoService;

    public PagamentoController(PagamentoService pagamentoService, PedidoService pedidoService) {
        this.pagamentoService = pagamentoService;
        this.pedidoService = pedidoService;
    }

    @PostMapping("/pagar")
    public String pagar(@AuthenticationPrincipal UsuarioDetails usuario,
                        @PathVariable("pedidoId") Long pedidoId,
                        @RequestParam(name = "forma", required = false) String forma,
                        RedirectAttributes redirect) {
        FormaPagamento escolhida = FormaPagamento.deTexto(forma);
        if (escolhida == null) {
            redirect.addFlashAttribute("erro", "Escolha a forma de pagamento.");
            return "redirect:/pedidos/" + pedidoId;
        }
        try {
            pagamentoService.iniciar(usuario.getId(), pedidoId, escolhida);
            return "redirect:/pedidos/" + pedidoId + "/pagamento";
        } catch (RegraDeNegocioException | GatewayPagamentoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos/" + pedidoId;
        }
    }

    @GetMapping("/pagamento")
    public String ver(@AuthenticationPrincipal UsuarioDetails usuario,
                      @PathVariable("pedidoId") Long pedidoId, Model model) {
        Pedido pedido = pedidoService.buscarDoCliente(usuario.getId(), pedidoId);
        Optional<Pagamento> pagamento = pagamentoService.ultimoDoPedido(pedidoId);
        if (pagamento.isEmpty()) {
            return "redirect:/pedidos/" + pedidoId;
        }
        model.addAttribute("pedido", pedido);
        model.addAttribute("pagamento", pagamento.get());
        return "pagamento";
    }

    /** Consultado pela própria página a cada poucos segundos. Lê só o banco, sem chamar o Asaas. */
    @GetMapping("/pagamento/status")
    @ResponseBody
    public Map<String, String> status(@AuthenticationPrincipal UsuarioDetails usuario,
                                      @PathVariable("pedidoId") Long pedidoId) {
        Pedido pedido = pedidoService.buscarDoCliente(usuario.getId(), pedidoId);
        String statusPagamento = pagamentoService.ultimoDoPedido(pedidoId)
                .map(p -> p.getStatus().name())
                .orElse("SEM_COBRANCA");
        return Map.of("pagamento", statusPagamento, "pedido", pedido.getStatus().name());
    }

    /** Botão "Já paguei": consulta o Asaas na hora. */
    @PostMapping("/pagamento/verificar")
    public String verificar(@AuthenticationPrincipal UsuarioDetails usuario,
                            @PathVariable("pedidoId") Long pedidoId,
                            RedirectAttributes redirect) {
        try {
            pagamentoService.verificar(usuario.getId(), pedidoId);
        } catch (RegraDeNegocioException | GatewayPagamentoException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pedidos/" + pedidoId + "/pagamento";
    }
}

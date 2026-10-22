package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.ItemNotaFiscal;
import com.mizukami.vendas.models.ItemPedido;
import com.mizukami.vendas.models.NotaFiscal;
import com.mizukami.vendas.models.Pedido;
import com.mizukami.vendas.models.StatusNotaFiscal;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.NotaFiscalRepository;
import com.mizukami.vendas.repositories.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class NotaFiscalService {

    //Pedidos com pagamento aprovado em diante.
    private static final Set<StatusPedido> EMISSIVEIS = EnumSet.of(
            StatusPedido.PAGAMENTO_APROVADO, StatusPedido.EM_PREPARACAO,
            StatusPedido.ENVIADO, StatusPedido.FINALIZADO);

    private static final int JUSTIFICATIVA_MINIMA = 15;
    private static final int JUSTIFICATIVA_MAXIMA = 255;

    private final NotaFiscalRepository notaRepository;
    private final PedidoRepository pedidoRepository;
    private final EmissorNotaFiscal emissor;
    private final String emitenteNome;
    private final String emitenteCnpj;
    private final long prazoCancelamentoHoras;

    public NotaFiscalService(NotaFiscalRepository notaRepository,
                             PedidoRepository pedidoRepository,
                             EmissorNotaFiscal emissor,
                             @Value("${app.fiscal.emitente-nome:Loja Vendas (empresa fictícia)}") String emitenteNome,
                             @Value("${app.fiscal.emitente-cnpj:00.000.000/0001-00}") String emitenteCnpj,
                             @Value("${app.fiscal.prazo-cancelamento-horas:24}") long prazoCancelamentoHoras) {
        this.notaRepository = notaRepository;
        this.pedidoRepository = pedidoRepository;
        this.emissor = emissor;
        this.emitenteNome = emitenteNome;
        this.emitenteCnpj = emitenteCnpj;
        this.prazoCancelamentoHoras = prazoCancelamentoHoras;
    }

    public long getPrazoCancelamentoHoras() {
        return prazoCancelamentoHoras;
    }

    @Transactional(readOnly = true)
    public Optional<NotaFiscal> buscarPorPedido(Long pedidoId) {
        return notaRepository.findByPedidoId(pedidoId);
    }

    @Transactional(readOnly = true)
    public NotaFiscal buscarObrigatoria(Long pedidoId) {
        return notaRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Este pedido não tem nota fiscal."));
    }

    @Transactional(readOnly = true)
    public boolean temNotaAtiva(Long pedidoId) {
        return notaRepository.existsByPedidoIdAndStatus(pedidoId, StatusNotaFiscal.AUTORIZADA);
    }

    /** Motivos que impedem a emissão. Lista vazia = pode emitir (se ainda não houver nota). */
    public List<String> pendencias(Pedido pedido) {
        List<String> pendencias = new ArrayList<>();

        if (!EMISSIVEIS.contains(pedido.getStatus())) {
            pendencias.add("O pedido precisa estar com o pagamento aprovado.");
        }
        Usuario cliente = pedido.getCliente();
        String cpf = cliente.getCpf() == null ? "" : cliente.getCpf().replaceAll("\\D", "");
        if (cpf.length() != 11) {
            pendencias.add("O cliente não tem CPF cadastrado (edite os dados dele em Clientes).");
        }
        if (pedido.getEnderecoEntrega() == null || pedido.getEnderecoEntrega().isBlank()) {
            pendencias.add("O pedido não tem endereço de entrega.");
        }
        return pendencias;
    }

    @Transactional
    public NotaFiscal emitir(Long pedidoId) {
        Pedido pedido = pedidoRepository.findDetalhadoById(pedidoId)
                .filter(p -> p.getStatus() != StatusPedido.CARRINHO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));

        if (notaRepository.existsByPedidoId(pedidoId)) {
            throw new RegraDeNegocioException("Este pedido já tem nota fiscal.");
        }
        List<String> pendencias = pendencias(pedido);
        if (!pendencias.isEmpty()) {
            throw new RegraDeNegocioException("Não é possível emitir a nota. " + String.join(" ", pendencias));
        }

        Usuario cliente = pedido.getCliente();
        NotaFiscal nota = new NotaFiscal();
        nota.setPedido(pedido);
        nota.setEmitenteNome(emitenteNome);
        nota.setEmitenteCnpj(emitenteCnpj);
        nota.setNaturezaOperacao("Venda de mercadoria");
        nota.setDestinatarioNome(cliente.getNome());
        nota.setDestinatarioCpf(cliente.getCpf());
        nota.setDestinatarioEmail(cliente.getEmail());
        nota.setEnderecoEntrega(pedido.getEnderecoEntrega());

        int numeroItem = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            ItemNotaFiscal itemNota = new ItemNotaFiscal();
            itemNota.setNumeroItem(numeroItem++);
            itemNota.setDescricao(item.getProduto().getNome());
            itemNota.setQuantidade(item.getQuantidade());
            itemNota.setValorUnitario(item.getPrecoUnitario());
            itemNota.setValorTotal(item.getSubtotal());
            nota.adicionarItem(itemNota);
            total = total.add(item.getSubtotal());
        }
        nota.setValorTotal(total);

        emissor.emitir(nota);

        try {
            return notaRepository.saveAndFlush(nota);
        } catch (DataIntegrityViolationException e) {
            // Duas emissões simultâneas do mesmo pedido: a segunda desfaz (inclusive o número reservado)
            throw new RegraDeNegocioException("Este pedido já tem nota fiscal.");
        }
    }

    @Transactional
    public void cancelar(Long pedidoId, String justificativa) {
        String motivo = justificativa == null ? "" : justificativa.trim();
        if (motivo.length() < JUSTIFICATIVA_MINIMA) {
            throw new RegraDeNegocioException(
                    "A justificativa deve ter no mínimo " + JUSTIFICATIVA_MINIMA + " caracteres.");
        }
        if (motivo.length() > JUSTIFICATIVA_MAXIMA) {
            throw new RegraDeNegocioException(
                    "A justificativa deve ter no máximo " + JUSTIFICATIVA_MAXIMA + " caracteres.");
        }

        NotaFiscal nota = buscarObrigatoria(pedidoId);
        if (nota.getStatus() != StatusNotaFiscal.AUTORIZADA) {
            throw new RegraDeNegocioException("Esta nota já está cancelada.");
        }
        if (LocalDateTime.now().isAfter(nota.getEmitidaEm().plusHours(prazoCancelamentoHoras))) {
            throw new RegraDeNegocioException("O prazo de cancelamento (" + prazoCancelamentoHoras
                    + " horas após a emissão) já passou.");
        }

        emissor.cancelar(nota, motivo);
        nota.setStatus(StatusNotaFiscal.CANCELADA);
        nota.setCanceladaEm(LocalDateTime.now());
        nota.setMotivoCancelamento(motivo);
    }
}

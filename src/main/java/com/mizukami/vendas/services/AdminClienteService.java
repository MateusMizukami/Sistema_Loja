package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.ClienteAdminDTO;
import com.mizukami.vendas.dto.PedidoResumoDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.StatusPedido;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminClienteService {

    public static final int TAMANHO_PAGINA = 15;

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;

    public AdminClienteService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listar(String termo, int pagina) {
        String filtro = termo == null ? "" : termo.trim();
        return usuarioRepository.buscarPorPerfil(Perfil.ROLE_USER, filtro,
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA, Sort.by("nome").ascending()));
    }

    /** Só devolve clientes. Contas de gerente respondem como "não encontrado". */
    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getPerfil() == Perfil.ROLE_USER)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<PedidoResumoDTO> pedidosDoCliente(Long clienteId) {
        return pedidoRepository.findByClienteIdAndStatusNotOrderByDataCriacaoDesc(clienteId, StatusPedido.CARRINHO)
                .stream()
                .map(PedidoResumoDTO::de)
                .toList();
    }

    @Transactional
    public void atualizar(Long id, ClienteAdminDTO dto) {
        Usuario cliente = buscar(id);
        cliente.setNome(dto.getNome().trim());
        cliente.setTelefone(vazioParaNulo(dto.getTelefone()));
        cliente.setCpf(vazioParaNulo(dto.getCpf()));
        usuarioRepository.save(cliente);
    }

    @Transactional
    public void alterarAtivo(Long id, boolean ativo, Long adminLogadoId) {
        if (id.equals(adminLogadoId)) {
            throw new RegraDeNegocioException("Você não pode alterar o status da sua própria conta.");
        }
        Usuario cliente = buscar(id);
        cliente.setAtivo(ativo);
        usuarioRepository.save(cliente);
    }

    private String vazioParaNulo(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return texto.trim();
    }
}

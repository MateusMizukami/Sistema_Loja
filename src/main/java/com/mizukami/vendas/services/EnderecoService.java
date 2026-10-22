package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.EnderecoDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Endereco;
import com.mizukami.vendas.repositories.EnderecoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class EnderecoService {

    public static final int LIMITE_POR_USUARIO = 10;

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Endereco> listar(Long usuarioId) {
        return enderecoRepository.findByUsuarioIdOrderByIdAsc(usuarioId);
    }

    /** Só devolve endereços do próprio usuário. */
    @Transactional(readOnly = true)
    public Endereco buscar(Long usuarioId, Long enderecoId) {
        return enderecoRepository.findByIdAndUsuarioId(enderecoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado"));
    }

    @Transactional
    public Endereco criar(Long usuarioId, EnderecoDTO dto) {
        if (enderecoRepository.countByUsuarioId(usuarioId) >= LIMITE_POR_USUARIO) {
            throw new RegraDeNegocioException("Você pode cadastrar no máximo " + LIMITE_POR_USUARIO + " endereços.");
        }
        Endereco endereco = new Endereco();
        endereco.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        preencher(endereco, dto);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public void atualizar(Long usuarioId, Long enderecoId, EnderecoDTO dto) {
        Endereco endereco = buscar(usuarioId, enderecoId);
        preencher(endereco, dto);
        enderecoRepository.save(endereco);
    }

    /** Pedidos antigos não são afetados: eles guardam o endereço como texto. */
    @Transactional
    public void excluir(Long usuarioId, Long enderecoId) {
        enderecoRepository.delete(buscar(usuarioId, enderecoId));
    }

    private void preencher(Endereco endereco, EnderecoDTO dto) {
        String digitos = dto.getCep().replaceAll("\\D", "");
        endereco.setCep(digitos.substring(0, 5) + "-" + digitos.substring(5));
        endereco.setLogradouro(dto.getLogradouro().trim());
        endereco.setNumero(dto.getNumero().trim());
        String complemento = dto.getComplemento() == null ? "" : dto.getComplemento().trim();
        endereco.setComplemento(complemento.isEmpty() ? null : complemento);
        endereco.setBairro(dto.getBairro().trim());
        endereco.setCidade(dto.getCidade().trim());
        endereco.setEstado(dto.getEstado().trim().toUpperCase(Locale.ROOT));
    }
}

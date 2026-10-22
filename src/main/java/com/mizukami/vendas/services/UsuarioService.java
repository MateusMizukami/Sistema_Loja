package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.CadastroDTO;
import com.mizukami.vendas.dto.PerfilDTO;
import com.mizukami.vendas.exceptions.EmailJaCadastradoException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.SenhaAtualIncorretaException;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Cadastro público: sempre cria um cliente (ROLE_USER).
    @Transactional
    public Usuario cadastrar(CadastroDTO dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(Perfil.ROLE_USER);
        usuario.setAtivo(true);

        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new EmailJaCadastradoException(email);
        }
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    // O CPF é guardado formatado (000.000.000-00).
    @Transactional
    public void atualizarPerfil(Long usuarioId, PerfilDTO dto) {
        Usuario usuario = buscarPorId(usuarioId);
        usuario.setNome(dto.getNome().trim());

        String telefone = dto.getTelefone() == null ? "" : dto.getTelefone().trim();
        usuario.setTelefone(telefone.isEmpty() ? null : telefone);

        String cpf = dto.getCpf() == null ? "" : dto.getCpf().replaceAll("\\D", "");
        usuario.setCpf(cpf.isEmpty() ? null : formatarCpf(cpf));

        usuarioRepository.save(usuario);
    }

    @Transactional
    public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorId(usuarioId);
        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new SenhaAtualIncorretaException();
        }
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    private String formatarCpf(String digitos) {
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9, 11);
    }
}

package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.CadastroDTO;
import com.mizukami.vendas.dto.PerfilDTO;
import com.mizukami.vendas.exceptions.EmailJaCadastradoException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.SenhaAtualIncorretaException;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cadastro, leitura, atualização de perfil e troca de senha do cliente*/
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    private CadastroDTO cadastro() {
        CadastroDTO dto = new CadastroDTO();
        dto.setNome("  Maria Silva ");
        dto.setEmail("  MARIA@Email.COM ");
        dto.setSenha("segredo123");
        dto.setConfirmarSenha("segredo123");
        return dto;
    }

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Maria Silva");
        u.setEmail("maria@email.com");
        u.setSenha("hash-antigo");
        u.setPerfil(Perfil.ROLE_USER);
        u.setAtivo(true);
        return u;
    }

    @Test
    @DisplayName("cria cliente com e-mail em minúsculas e senha criptografada")
    void cadastrarNovoCliente() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("segredo123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrar(cadastro());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Maria Silva");
        assertThat(salvo.getEmail()).isEqualTo("maria@email.com");
        assertThat(salvo.getSenha()).isEqualTo("hash-bcrypt").isNotEqualTo("segredo123");
        assertThat(salvo.getPerfil()).isEqualTo(Perfil.ROLE_USER);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("e-mail já usado é recusado e nada é gravado")
    void cadastrarEmailRepetido() {
        when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(cadastro())).isInstanceOf(EmailJaCadastradoException.class);

        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    @DisplayName("duplicidade vira EmailJaCadastrado")
    void cadastrarCorridaNoBanco() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.cadastrar(cadastro())).isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    @DisplayName("Buscar por id: devolve o usuário existente")
    void buscarPorIdExistente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));

        assertThat(service.buscarPorId(1L).getEmail()).isEqualTo("maria@email.com");
    }

    @Test
    @DisplayName("Buscar por id: usuário inexistente responde como não encontrado")
    void buscarPorIdInexistente() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(404L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("formata o CPF e transforma telefone vazio em nulo")
    void atualizarPerfil() {
        Usuario existente = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizarPerfil(1L, new PerfilDTO("  Maria Souza ", "   ", "52998224725"));

        assertThat(existente.getNome()).isEqualTo("Maria Souza");
        assertThat(existente.getTelefone()).isNull();
        assertThat(existente.getCpf()).isEqualTo("529.982.247-25");
        verify(usuarioRepository).save(existente);
    }

    @Test
    @DisplayName("com a senha atual correta, grava a nova")
    void alterarSenhaCorreta() {
        Usuario existente = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("antiga", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("nova12345")).thenReturn("hash-novo");

        service.alterarSenha(1L, "antiga", "nova12345");

        assertThat(existente.getSenha()).isEqualTo("hash-novo");
    }

    @Test
    @DisplayName("com a senha atual errada, recusa e não muda")
    void alterarSenhaErrada() {
        Usuario existente = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("errada", "hash-antigo")).thenReturn(false);

        assertThatThrownBy(() -> service.alterarSenha(1L, "errada", "nova12345"))
                .isInstanceOf(SenhaAtualIncorretaException.class);

        assertThat(existente.getSenha()).isEqualTo("hash-antigo");
        verify(passwordEncoder, never()).encode(any());
    }
}

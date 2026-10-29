package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.ClienteAdminDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.PedidoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gestão de clientes no painel do gerente.*/
@ExtendWith(MockitoExtension.class)
class AdminClienteServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    PedidoRepository pedidoRepository;

    AdminClienteService service;

    @BeforeEach
    void preparar() {
        service = new AdminClienteService(usuarioRepository, pedidoRepository);
    }

    private Usuario cliente(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Ana Luiza");
        u.setEmail("ana@email.com");
        u.setPerfil(Perfil.ROLE_USER);
        u.setAtivo(true);
        return u;
    }

    @Test
    @DisplayName("busca só clientes e remove espaços do termo")
    void listarComTermo() {
        when(usuarioRepository.buscarPorPerfil(eq(Perfil.ROLE_USER), eq("ana"), any(Pageable.class)))
                .thenReturn(new PageImpl<Usuario>(List.of(cliente(1L))));

        Page<Usuario> pagina = service.listar("  ana ", 0);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("sem termo usa texto vazio, que traz todos os clientes")
    void listarSemTermo() {
        when(usuarioRepository.buscarPorPerfil(eq(Perfil.ROLE_USER), eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<Usuario>(List.of(cliente(1L), cliente(2L))));

        assertThat(service.listar(null, 0).getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("devolve o cliente existente")
    void buscarExistente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente(1L)));

        assertThat(service.buscar(1L).getEmail()).isEqualTo("ana@email.com");
    }

    @Test
    @DisplayName("conta de gerente responde como não encontrada")
    void buscarGerenteNaoEhCliente() {
        Usuario gerente = cliente(2L);
        gerente.setPerfil(Perfil.ROLE_ADMIN);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(gerente));

        assertThatThrownBy(() -> service.buscar(2L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("troca nome, telefone e CPF; telefone vazio vira nulo")
    void atualizarDados() {
        Usuario existente = cliente(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, new ClienteAdminDTO("  Maria Silva ", "  ", "529.982.247-25"));

        assertThat(existente.getNome()).isEqualTo("Maria Silva");
        assertThat(existente.getTelefone()).isNull();
        assertThat(existente.getCpf()).isEqualTo("529.982.247-25");
        verify(usuarioRepository).save(existente);
    }

    @Test
    @DisplayName("a conta fica inativa")
    void desativarCliente() {
        Usuario existente = cliente(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.alterarAtivo(1L, false, 99L);

        assertThat(existente.isAtivo()).isFalse();
        verify(usuarioRepository).save(existente);
    }

    @Test
    @DisplayName("a conta volta a ficar ativa")
    void reativarCliente() {
        Usuario existente = cliente(1L);
        existente.setAtivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.alterarAtivo(1L, true, 99L);

        assertThat(existente.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("O gerente não pode alterar o status da própria conta")
    void gerenteNaoAlteraAPropriaConta() {
        assertThatThrownBy(() -> service.alterarAtivo(5L, false, 5L))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}

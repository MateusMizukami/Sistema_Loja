package com.mizukami.vendas.integration;

import com.mizukami.vendas.dto.CadastroDTO;
import com.mizukami.vendas.dto.ClienteAdminDTO;
import com.mizukami.vendas.dto.PerfilDTO;
import com.mizukami.vendas.exceptions.EmailJaCadastradoException;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.UsuarioRepository;
import com.mizukami.vendas.security.CustomUserDetailsService;
import com.mizukami.vendas.services.AdminClienteService;
import com.mizukami.vendas.services.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CRUD de cliente: cadastro, perfil, painel do gerente e login,
 * com o banco H2.*/
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClienteCrudIntegrationTest {

    private static final long ID_DE_OUTRO_GERENTE = -1L;

    @Autowired
    UsuarioService usuarioService;
    @Autowired
    AdminClienteService adminClienteService;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    CustomUserDetailsService userDetailsService;

    private String emailNovo() {
        return "cliente" + System.nanoTime() + "@teste.com";
    }

    private Usuario cadastrar(String email) {
        CadastroDTO dto = new CadastroDTO();
        dto.setNome("Carlos Alberto");
        dto.setEmail(email);
        dto.setSenha("senha123");
        dto.setConfirmarSenha("senha123");
        return usuarioService.cadastrar(dto);
    }

    @Test
    @DisplayName("cadastro grava um cliente ativo, com senha criptografada (BCrypt)")
    void cadastrar() {
        Usuario criado = cadastrar(emailNovo());

        assertThat(criado.getId()).isNotNull();
        assertThat(criado.getPerfil()).isEqualTo(Perfil.ROLE_USER);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getSenha()).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", criado.getSenha())).isTrue();
    }

    @Test
    @DisplayName("o e-mail é normalizado (minúsculas, sem espaços) e não pode se repetir")
    void emailNormalizadoEUnico() {
        String base = emailNovo();
        cadastrar("  " + base.toUpperCase() + " ");

        assertThat(usuarioRepository.findByEmail(base)).isPresent();
        assertThatThrownBy(() -> cadastrar(base)).isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    @DisplayName("o cliente edita o próprio perfil e o CPF é guardado formatado")
    void clienteAtualizaPerfil() {
        Usuario criado = cadastrar(emailNovo());

        usuarioService.atualizarPerfil(criado.getId(),
                new PerfilDTO("Maria Souza", "(67) 99999-0000", "52998224725"));

        Usuario lido = usuarioService.buscarPorId(criado.getId());
        assertThat(lido.getNome()).isEqualTo("Maria Souza");
        assertThat(lido.getTelefone()).isEqualTo("(67) 99999-0000");
        assertThat(lido.getCpf()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("o gerente edita os dados do cliente")
    void gerenteEditaCliente() {
        Usuario criado = cadastrar(emailNovo());

        adminClienteService.atualizar(criado.getId(),
                new ClienteAdminDTO("Nome corrigido", "", "529.982.247-25"));

        Usuario lido = adminClienteService.buscar(criado.getId());
        assertThat(lido.getNome()).isEqualTo("Nome corrigido");
        assertThat(lido.getTelefone()).isNull();
        assertThat(lido.getCpf()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("o painel lista e busca clientes pelo e-mail")
    void gerenteBuscaCliente() {
        String email = emailNovo();
        cadastrar(email);

        Page<Usuario> pagina = adminClienteService.listar(email, 0);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("conta de gerente não aparece como cliente")
    void gerenteNaoEhCliente() {
        Usuario gerente = new Usuario();
        gerente.setNome("Gerente de teste");
        gerente.setEmail(emailNovo());
        gerente.setSenha(passwordEncoder.encode("senha123"));
        gerente.setPerfil(Perfil.ROLE_ADMIN);
        Usuario salvo = usuarioRepository.save(gerente);

        assertThatThrownBy(() -> adminClienteService.buscar(salvo.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("conta desativada continua no banco, mas o login é bloqueado")
    void desativarBloqueiaLogin() {
        String email = emailNovo();
        Usuario criado = cadastrar(email);
        assertThat(userDetailsService.loadUserByUsername(email).isEnabled()).isTrue();

        adminClienteService.alterarAtivo(criado.getId(), false, ID_DE_OUTRO_GERENTE);

        assertThat(usuarioRepository.findById(criado.getId())).isPresent();   // não foi apagado
        assertThat(userDetailsService.loadUserByUsername(email).isEnabled()).isFalse();

        adminClienteService.alterarAtivo(criado.getId(), true, ID_DE_OUTRO_GERENTE);

        assertThat(userDetailsService.loadUserByUsername(email).isEnabled()).isTrue();
    }
}

package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.ProdutoFormDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.repositories.CategoriaRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CRUD de produto no painel do gerente*/
@ExtendWith(MockitoExtension.class)
class AdminProdutoServiceTest {

    @Mock
    ProdutoRepository produtoRepository;
    @Mock
    CategoriaRepository categoriaRepository;
    @Mock
    ArmazenamentoImagemService armazenamento;

    AdminProdutoService service;
    Categoria categoria;

    @BeforeEach
    void preparar() {
        service = new AdminProdutoService(produtoRepository, categoriaRepository, armazenamento);
        categoria = new Categoria("Informática");
        categoria.setId(1L);
    }

    private ProdutoFormDTO formularioValido() {
        ProdutoFormDTO dto = new ProdutoFormDTO();
        dto.setNome("Mouse sem fio");
        dto.setPreco(new BigDecimal("89.90"));
        dto.setEstoque(10);
        dto.setCategoriaId(1L);
        dto.setAtivo(true);
        return dto;
    }

    private Produto produtoExistente() {
        Produto p = new Produto();
        p.setId(10L);
        p.setNome("Mouse");
        p.setPreco(new BigDecimal("89.90"));
        p.setEstoque(5);
        p.setAtivo(true);
        p.setCategoria(categoria);
        p.setUrlImagem("/uploads/produtos/antiga.png");
        return p;
    }

    @Test
    @DisplayName("grava produto ativo, com nome sem espaços sobrando e preço com 2 casas")
    void criarComDadosValidos() {
        ProdutoFormDTO dto = formularioValido();
        dto.setNome("  Notebook Dev  ");
        dto.setDescricao("   ");
        dto.setPreco(new BigDecimal("4500.5"));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto criado = service.criar(dto, null);

        assertThat(criado.getNome()).isEqualTo("Notebook Dev");
        assertThat(criado.getDescricao()).isNull();
        assertThat(criado.getPreco()).isEqualByComparingTo("4500.50");
        assertThat(criado.getPreco().scale()).isEqualTo(2);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getCategoria()).isSameAs(categoria);
        verify(armazenamento, never()).salvar(any());
    }

    @Test
    @DisplayName("categoria inexistente é recusada e nada é gravado")
    void criarComCategoriaInexistente() {
        ProdutoFormDTO dto = formularioValido();
        dto.setCategoriaId(99L);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(dto, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Categoria");

        verify(produtoRepository, never()).save(any(Produto.class));
    }

    @Test
    @DisplayName("com imagem, guarda a URL devolvida pelo armazenamento")
    void criarComImagem() {
        MockMultipartFile imagem = new MockMultipartFile("imagem", "foto.png", "image/png", new byte[]{1, 2, 3});
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(armazenamento.salvar(imagem)).thenReturn("/uploads/produtos/nova.png");
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto criado = service.criar(formularioValido(), imagem);

        assertThat(criado.getUrlImagem()).isEqualTo("/uploads/produtos/nova.png");
    }

    @Test
    @DisplayName("devolve o produto existente")
    void buscarExistente() {
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente()));

        assertThat(service.buscar(10L).getNome()).isEqualTo("Mouse");
    }

    @Test
    @DisplayName("produto inexistente responde como não encontrado")
    void buscarInexistente() {
        when(produtoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(404L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("devolve a página de produtos do repositório")
    void listar() {
        when(produtoRepository.findAll(ArgumentMatchers.<Specification<Produto>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<Produto>(List.of(produtoExistente())));

        Page<Produto> pagina = service.listar("mouse", 0);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getNome()).isEqualTo("Mouse");
    }

    @Test
    @DisplayName("altera os campos, troca a imagem e apaga a imagem antiga")
    void atualizarTrocandoImagem() {
        Produto existente = produtoExistente();
        ProdutoFormDTO dto = formularioValido();
        dto.setNome("Mouse Gamer");
        dto.setPreco(new BigDecimal("129.9"));
        dto.setEstoque(3);
        dto.setAtivo(false);
        MockMultipartFile imagem = new MockMultipartFile("imagem", "novo.png", "image/png", new byte[]{9});
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(armazenamento.salvar(imagem)).thenReturn("/uploads/produtos/nova.png");

        service.atualizar(10L, dto, imagem);

        assertThat(existente.getNome()).isEqualTo("Mouse Gamer");
        assertThat(existente.getPreco()).isEqualByComparingTo("129.90");
        assertThat(existente.getEstoque()).isEqualTo(3);
        assertThat(existente.isAtivo()).isFalse();
        assertThat(existente.getUrlImagem()).isEqualTo("/uploads/produtos/nova.png");
        verify(produtoRepository).saveAndFlush(existente);
        verify(armazenamento).remover("/uploads/produtos/antiga.png");
    }

    @Test
    @DisplayName("a opção de remover imagem limpa a URL e apaga o arquivo")
    void atualizarRemovendoImagem() {
        Produto existente = produtoExistente();
        ProdutoFormDTO dto = formularioValido();
        dto.setRemoverImagem(true);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        service.atualizar(10L, dto, null);

        assertThat(existente.getUrlImagem()).isNull();
        verify(armazenamento).remover("/uploads/produtos/antiga.png");
    }

    @Test
    @DisplayName("desativa o produto")
    void desativarProduto() {
        Produto existente = produtoExistente();
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(existente));

        service.alterarAtivo(10L, false);

        assertThat(existente.isAtivo()).isFalse();
        verify(produtoRepository).save(existente);
        verify(produtoRepository, never()).delete(any(Produto.class));
    }

    @Test
    @DisplayName("o produto volta a ficar ativo")
    void reativarProduto() {
        Produto existente = produtoExistente();
        existente.setAtivo(false);
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(existente));

        service.alterarAtivo(10L, true);

        assertThat(existente.isAtivo()).isTrue();
    }
}

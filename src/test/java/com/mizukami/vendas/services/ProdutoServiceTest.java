package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.ProdutoNaoEncontradoException;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Leitura de produtos no catálogo público*/
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    ProdutoRepository produtoRepository;

    ProdutoService service;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(produtoRepository);
    }

    @Test
    @DisplayName("devolve o produto ativo")
    void buscarAtivoExistente() {
        Produto p = new Produto();
        p.setId(5L);
        p.setNome("Teclado");
        when(produtoRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(p));

        assertThat(service.buscarAtivoPorId(5L).getNome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("produto inexistente ou inativo resulta em 'não encontrado'")
    void buscarAtivoInexistente() {
        when(produtoRepository.findByIdAndAtivoTrue(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarAtivoPorId(9L)).isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("pagina de 12 em 12 itens")
    void buscaUsaPaginaDeDoze() {
        when(produtoRepository.findAll(ArgumentMatchers.<Specification<Produto>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<Produto>(List.of()));

        service.buscar(null, null, null, null, "nome", 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(produtoRepository).findAll(ArgumentMatchers.<Specification<Produto>>any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(ProdutoService.TAMANHO_PAGINA);
    }

    @Test
    @DisplayName("Ordenação: valores válidos são mantidos")
    void ordemValida() {
        for (String ordem : List.of("nome", "menor-preco", "maior-preco", "recentes")) {
            assertThat(ProdutoService.normalizarOrdem(ordem)).isEqualTo(ordem);
        }
    }

    @Test
    @DisplayName("Ordenação: valor inválido volta para o padrão")
    void ordemInvalida() {
        assertThat(ProdutoService.normalizarOrdem(null)).isEqualTo(ProdutoService.ORDEM_PADRAO);
        assertThat(ProdutoService.normalizarOrdem("")).isEqualTo(ProdutoService.ORDEM_PADRAO);
        assertThat(ProdutoService.normalizarOrdem("preco; drop table")).isEqualTo(ProdutoService.ORDEM_PADRAO);
    }
}

package com.mizukami.vendas.integration;

import com.mizukami.vendas.dto.ProdutoFormDTO;
import com.mizukami.vendas.exceptions.ProdutoNaoEncontradoException;
import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.repositories.CategoriaRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import com.mizukami.vendas.services.AdminProdutoService;
import com.mizukami.vendas.services.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CRUD do produto: service + repository + banco H2.*/
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProdutoCrudIntegrationTest {

    @Autowired
    AdminProdutoService adminProdutoService;
    @Autowired
    ProdutoService produtoService;
    @Autowired
    CategoriaRepository categoriaRepository;
    @Autowired
    ProdutoRepository produtoRepository;

    private Categoria categoria;

    @BeforeEach
    void criarCategoria() {
        categoria = categoriaRepository.save(new Categoria("Categoria de teste " + System.nanoTime()));
    }

    private ProdutoFormDTO formulario(String nome, String preco, int estoque) {
        ProdutoFormDTO dto = new ProdutoFormDTO();
        dto.setNome(nome);
        dto.setPreco(new BigDecimal(preco));
        dto.setEstoque(estoque);
        dto.setCategoriaId(categoria.getId());
        dto.setAtivo(true);
        return dto;
    }

    @Test
    @DisplayName("o produto criado é gravado e pode ser lido de volta")
    void criarELer() {
        Produto criado = adminProdutoService.criar(formulario("Produto CRUD", "99.9", 7), null);

        assertThat(criado.getId()).isNotNull();

        Produto lido = adminProdutoService.buscar(criado.getId());
        assertThat(lido.getNome()).isEqualTo("Produto CRUD");
        assertThat(lido.getPreco()).isEqualByComparingTo("99.90");
        assertThat(lido.getEstoque()).isEqualTo(7);
        assertThat(lido.isAtivo()).isTrue();
        assertThat(lido.getCategoria().getId()).isEqualTo(categoria.getId());
    }

    @Test
    @DisplayName("a listagem do gerente encontra o produto pelo nome")
    void listarPorNome() {
        String marca = "ZZ" + System.nanoTime();
        adminProdutoService.criar(formulario("Cadeira " + marca, "350.00", 2), null);

        Page<Produto> pagina = adminProdutoService.listar(marca, 0);

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getNome()).contains(marca);
    }

    @Test
    @DisplayName("preço, estoque e nome são alterados no banco")
    void atualizar() {
        Produto criado = adminProdutoService.criar(formulario("Produto antigo", "10.00", 1), null);

        adminProdutoService.atualizar(criado.getId(), formulario("Produto novo", "25.5", 8), null);

        Produto lido = produtoRepository.findById(criado.getId()).orElseThrow();
        assertThat(lido.getNome()).isEqualTo("Produto novo");
        assertThat(lido.getPreco()).isEqualByComparingTo("25.50");
        assertThat(lido.getEstoque()).isEqualTo(8);
    }

    @Test
    @DisplayName("produto desativado some do catálogo público, mas continua no banco")
    void desativarEReativar() {
        Produto criado = adminProdutoService.criar(formulario("Produto para desativar", "40.00", 5), null);
        Long id = criado.getId();

        adminProdutoService.alterarAtivo(id, false);

        assertThatThrownBy(() -> produtoService.buscarAtivoPorId(id))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
        assertThat(produtoRepository.findById(id)).isPresent();   // não foi apagado

        adminProdutoService.alterarAtivo(id, true);

        assertThatCode(() -> produtoService.buscarAtivoPorId(id)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("filtra por categoria e faixa de preço")
    void catalogoFiltraPorCategoriaEPreco() {
        adminProdutoService.criar(formulario("Barato", "10.00", 3), null);
        adminProdutoService.criar(formulario("Caro", "100.00", 3), null);

        Page<Produto> resultado = produtoService.buscar(
                null, categoria.getId(), new BigDecimal("50"), null, "nome", 0);

        assertThat(resultado.getContent()).extracting(Produto::getNome).containsExactly("Caro");
    }
}

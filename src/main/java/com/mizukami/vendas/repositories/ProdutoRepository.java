package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    List<Produto> findByAtivoTrue();

    List<Produto> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);

    List<Produto> findByCategoriaIdAndAtivoTrue(Long categoriaId);

    Optional<Produto> findByIdAndAtivoTrue(Long id);

    /** Produtos ativos com estoque até o limite, do menor para o maior (usado no dashboard). */
    List<Produto> findByAtivoTrueAndEstoqueLessThanEqualOrderByEstoqueAscNomeAsc(int limite);

    /**
     * Baixa de estoque: só altera se o produto estiver ativo e houver estoque suficiente.
     * Retorna o número de linhas alteradas (0 = sem estoque/indisponível).
     */
    @Modifying
    @Query("update Produto p set p.estoque = p.estoque - :quantidade "
            + "where p.id = :id and p.ativo = true and p.estoque >= :quantidade")
    int baixarEstoque(@Param("id") Long id, @Param("quantidade") int quantidade);

    /** Devolução de estoque (cancelamento de pedido). Funciona mesmo com o produto inativo. */
    @Modifying
    @Query("update Produto p set p.estoque = p.estoque + :quantidade where p.id = :id")
    int devolverEstoque(@Param("id") Long id, @Param("quantidade") int quantidade);
}

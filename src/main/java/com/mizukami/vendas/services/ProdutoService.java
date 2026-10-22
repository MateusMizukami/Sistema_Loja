package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.ProdutoNaoEncontradoException;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    public static final int TAMANHO_PAGINA = 12;
    public static final String ORDEM_PADRAO = "nome";

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public Page<Produto> buscar(String termo, Long categoriaId, BigDecimal minimo,
                                BigDecimal maximo, String ordem, int pagina) {

        Specification<Produto> spec = ProdutoSpecifications.ativo();

        if (termo != null && !termo.isBlank()) {
            spec = spec.and(ProdutoSpecifications.nomeContem(termo));
        }
        if (categoriaId != null) {
            spec = spec.and(ProdutoSpecifications.categoria(categoriaId));
        }
        if (minimo != null) {
            spec = spec.and(ProdutoSpecifications.precoMinimo(minimo));
        }
        if (maximo != null) {
            spec = spec.and(ProdutoSpecifications.precoMaximo(maximo));
        }

        Sort sort = ordenacao(ordem);
        int numero = Math.max(pagina, 0);

        Page<Produto> resultado = produtoRepository.findAll(spec, PageRequest.of(numero, TAMANHO_PAGINA, sort));

        if (resultado.getTotalPages() > 0 && numero >= resultado.getTotalPages()) {
            resultado = produtoRepository.findAll(spec,
                    PageRequest.of(resultado.getTotalPages() - 1, TAMANHO_PAGINA, sort));
        }
        return resultado;
    }

    public Produto buscarAtivoPorId(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    public static String normalizarOrdem(String ordem) {
        if (ordem == null) {
            return ORDEM_PADRAO;
        }
        return switch (ordem) {
            case "nome", "menor-preco", "maior-preco", "recentes" -> ordem;
            default -> ORDEM_PADRAO;
        };
    }

    private Sort ordenacao(String ordem) {
        return switch (normalizarOrdem(ordem)) {
            case "menor-preco" -> Sort.by("preco").ascending().and(Sort.by("nome").ascending());
            case "maior-preco" -> Sort.by("preco").descending().and(Sort.by("nome").ascending());
            case "recentes" -> Sort.by("id").descending();
            default -> Sort.by("nome").ascending();
        };
    }
}

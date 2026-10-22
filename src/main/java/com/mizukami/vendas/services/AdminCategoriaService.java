package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.CategoriaDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.repositories.CategoriaRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public AdminCategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    /** Quantidade de produtos (ativos e inativos) de cada categoria. */
    @Transactional(readOnly = true)
    public Map<Long, Long> contarProdutos(List<Categoria> categorias) {
        Map<Long, Long> contagens = new HashMap<>();
        for (Categoria categoria : categorias) {
            contagens.put(categoria.getId(), produtoRepository.count(ProdutoSpecifications.categoria(categoria.getId())));
        }
        return contagens;
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    }

    @Transactional
    public void criar(CategoriaDTO dto) {
        String nome = dto.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Já existe uma categoria com esse nome.");
        }
        salvar(new Categoria(nome));
    }

    @Transactional
    public void atualizar(Long id, CategoriaDTO dto) {
        Categoria categoria = buscar(id);
        String nome = dto.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException("Já existe uma categoria com esse nome.");
        }
        categoria.setNome(nome);
        salvar(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscar(id);
        long produtos = produtoRepository.count(ProdutoSpecifications.categoria(id));
        if (produtos > 0) {
            throw new RegraDeNegocioException("Não é possível excluir: a categoria “" + categoria.getNome()
                    + "” tem " + produtos + " produto(s). Mova-os para outra categoria antes.");
        }
        categoriaRepository.delete(categoria);
    }

    private void salvar(Categoria categoria) {
        try {
            categoriaRepository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException e) {
            throw new RegraDeNegocioException("Já existe uma categoria com esse nome.");
        }
    }
}

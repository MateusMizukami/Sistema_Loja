package com.mizukami.vendas.services;

import com.mizukami.vendas.dto.ProdutoFormDTO;
import com.mizukami.vendas.exceptions.RecursoNaoEncontradoException;
import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.repositories.CategoriaRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.RoundingMode;

@Service
public class AdminProdutoService {

    public static final int TAMANHO_PAGINA = 15;

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ArmazenamentoImagemService armazenamento;

    public AdminProdutoService(ProdutoRepository produtoRepository,
                               CategoriaRepository categoriaRepository,
                               ArmazenamentoImagemService armazenamento) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.armazenamento = armazenamento;
    }

    /** Lista todos os produtos, ativos e inativos. */
    @Transactional(readOnly = true)
    public Page<Produto> listar(String termo, int pagina) {
        Specification<Produto> spec = (root, query, cb) -> cb.conjunction();
        if (termo != null && !termo.isBlank()) {
            spec = spec.and(ProdutoSpecifications.nomeContem(termo.trim()));
        }
        return produtoRepository.findAll(spec,
                PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA, Sort.by("id").descending()));
    }

    @Transactional(readOnly = true)
    public Produto buscar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
    }

    @Transactional
    public Produto criar(ProdutoFormDTO dto, MultipartFile imagem) {
        Produto produto = new Produto();
        produto.setAtivo(true);
        preencher(produto, dto);

        String novaImagem = guardarImagem(imagem);
        if (novaImagem != null) {
            produto.setUrlImagem(novaImagem);
        }
        try {
            return produtoRepository.save(produto);
        } catch (RuntimeException e) {
            armazenamento.remover(novaImagem);
            throw e;
        }
    }

    @Transactional
    public void atualizar(Long id, ProdutoFormDTO dto, MultipartFile imagem) {
        Produto produto = buscar(id);
        String imagemAntiga = produto.getUrlImagem();

        preencher(produto, dto);
        produto.setAtivo(dto.isAtivo());

        String novaImagem = guardarImagem(imagem);
        boolean trocouImagem = false;
        if (novaImagem != null) {
            produto.setUrlImagem(novaImagem);
            trocouImagem = true;
        } else if (dto.isRemoverImagem()) {
            produto.setUrlImagem(null);
            trocouImagem = true;
        }

        try {
            produtoRepository.saveAndFlush(produto);
        } catch (RuntimeException e) {
            armazenamento.remover(novaImagem);
            throw e;
        }
        if (trocouImagem) {
            armazenamento.remover(imagemAntiga);
        }
    }

    /** "Excluir" = desativar: produtos com vendas nunca saem do banco. */
    @Transactional
    public void alterarAtivo(Long id, boolean ativo) {
        Produto produto = buscar(id);
        produto.setAtivo(ativo);
        produtoRepository.save(produto);
    }

    private void preencher(Produto produto, ProdutoFormDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RegraDeNegocioException("Categoria inválida."));

        produto.setNome(dto.getNome().trim());
        String descricao = dto.getDescricao() == null ? "" : dto.getDescricao().trim();
        produto.setDescricao(descricao.isEmpty() ? null : descricao);
        produto.setPreco(dto.getPreco().setScale(2, RoundingMode.HALF_UP));
        produto.setEstoque(dto.getEstoque());
        produto.setCategoria(categoria);
    }

    private String guardarImagem(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            return null;
        }
        return armazenamento.salvar(imagem);
    }
}

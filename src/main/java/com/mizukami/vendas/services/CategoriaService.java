package com.mizukami.vendas.services;

import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.repositories.CategoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarOrdenadas() {
        return categoriaRepository.findAll(Sort.by("nome").ascending());
    }
}

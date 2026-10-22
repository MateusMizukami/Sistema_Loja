package com.mizukami.vendas.services;

import com.mizukami.vendas.models.Produto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class ProdutoSpecifications {

    private static final char ESCAPE = '\\';

    private ProdutoSpecifications() {}

    public static Specification<Produto> ativo() {
        return (root, query, cb) -> cb.isTrue(root.<Boolean>get("ativo"));
    }

    // Busca por parte do nome, sem diferenciar maiúsculas/minúsculas.
    public static Specification<Produto> nomeContem(String termo) {
        String padrao = "%" + escapar(termo.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("nome")), padrao, ESCAPE);
    }

    public static Specification<Produto> categoria(Long categoriaId) {
        return (root, query, cb) -> cb.equal(root.get("categoria").<Long>get("id"), categoriaId);
    }

    public static Specification<Produto> precoMinimo(BigDecimal minimo) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("preco"), minimo);
    }

    public static Specification<Produto> precoMaximo(BigDecimal maximo) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("preco"), maximo);
    }

    // Impede que % e _ digitados pelo usuário funcionem como curingas do LIKE.
    private static String escapar(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

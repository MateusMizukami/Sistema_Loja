package com.mizukami.vendas.exceptions;

/** Violação de regra de negócio. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}

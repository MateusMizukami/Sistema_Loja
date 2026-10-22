package com.mizukami.vendas.exceptions;

public class EstoqueInsuficienteException extends RegraDeNegocioException {

    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}

package com.mizukami.vendas.exceptions;

public class SenhaAtualIncorretaException extends RegraDeNegocioException {

    public SenhaAtualIncorretaException() {
        super("Senha atual incorreta");
    }
}

package com.mizukami.vendas.services;

import com.mizukami.vendas.models.NotaFiscal;


public interface EmissorNotaFiscal {

    // Preenche número, série, protocolo, código de verificação, status e data da nota.
    void emitir(NotaFiscal nota);

    // Registra o cancelamento no emissor
    void cancelar(NotaFiscal nota, String justificativa);

    // Verdadeiro -> notas simuladas não tem valor fiscal.
    boolean simulado();
}

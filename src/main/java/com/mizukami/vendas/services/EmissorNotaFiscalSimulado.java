package com.mizukami.vendas.services;

import com.mizukami.vendas.models.NotaFiscal;
import com.mizukami.vendas.models.SequenciaNotaFiscal;
import com.mizukami.vendas.models.StatusNotaFiscal;
import com.mizukami.vendas.repositories.SequenciaNotaFiscalRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class EmissorNotaFiscalSimulado implements EmissorNotaFiscal {

    public static final int SERIE = 1;

    private final SequenciaNotaFiscalRepository sequenciaRepository;

    public EmissorNotaFiscalSimulado(SequenciaNotaFiscalRepository sequenciaRepository) {
        this.sequenciaRepository = sequenciaRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitir(NotaFiscal nota) {
        SequenciaNotaFiscal sequencia = sequenciaRepository.findComBloqueio(SERIE)
                .orElseThrow(() -> new IllegalStateException("A sequência da série " + SERIE + " não foi inicializada."));

        long numero = sequencia.proximo();
        nota.setSerie(SERIE);
        nota.setNumero(numero);
        nota.setCodigoVerificacao(UUID.randomUUID().toString());
        nota.setProtocolo("SIMULADO-" + SERIE + "-" + numero);
        nota.setSimulada(true);
        nota.setStatus(StatusNotaFiscal.AUTORIZADA);
        nota.setEmitidaEm(LocalDateTime.now());
    }

    @Override
    public void cancelar(NotaFiscal nota, String justificativa) {
        // Como a nota é simulada não há órgão externo para avisar.
    }

    @Override
    public boolean simulado() {
        return true;
    }
}

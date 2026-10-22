package com.mizukami.vendas.config;

import com.mizukami.vendas.models.SequenciaNotaFiscal;
import com.mizukami.vendas.repositories.SequenciaNotaFiscalRepository;
import com.mizukami.vendas.services.EmissorNotaFiscalSimulado;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Cria a linha da numeração da série na primeira execução, antes de qualquer emissão. */
@Component
public class NotaFiscalInicializador implements CommandLineRunner {

    private final SequenciaNotaFiscalRepository sequenciaRepository;

    public NotaFiscalInicializador(SequenciaNotaFiscalRepository sequenciaRepository) {
        this.sequenciaRepository = sequenciaRepository;
    }

    @Override
    public void run(String... args) {
        if (!sequenciaRepository.existsById(EmissorNotaFiscalSimulado.SERIE)) {
            sequenciaRepository.save(new SequenciaNotaFiscal(EmissorNotaFiscalSimulado.SERIE));
        }
    }
}

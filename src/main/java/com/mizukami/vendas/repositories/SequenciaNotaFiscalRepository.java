package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.SequenciaNotaFiscal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SequenciaNotaFiscalRepository extends JpaRepository<SequenciaNotaFiscal, Integer> {

    /** Trava a linha até o fim da transação: duas emissões simultâneas nunca recebem o mesmo número. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SequenciaNotaFiscal s where s.serie = :serie")
    Optional<SequenciaNotaFiscal> findComBloqueio(@Param("serie") Integer serie);
}

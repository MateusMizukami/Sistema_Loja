package com.mizukami.vendas.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Validação do CPF usado no cadastro e no pagamento. */
class CpfValidatorTest {

    private final CpfValidator validador = new CpfValidator();

    @Test
    @DisplayName("Aceita CPF válido, com ou sem máscara")
    void aceitaCpfValido() {
        assertThat(validador.isValid("529.982.247-25", null)).isTrue();
        assertThat(validador.isValid("52998224725", null)).isTrue();
    }

    @Test
    @DisplayName("Aceita campo vazio (o CPF só é exigido na hora de pagar)")
    void aceitaVazio() {
        assertThat(validador.isValid(null, null)).isTrue();
        assertThat(validador.isValid("   ", null)).isTrue();
    }

    @Test
    @DisplayName("Recusa dígito verificador errado e sequências repetidas")
    void recusaCpfInvalido() {
        assertThat(validador.isValid("529.982.247-24", null)).isFalse();
        assertThat(validador.isValid("111.111.111-11", null)).isFalse();
        assertThat(validador.isValid("123", null)).isFalse();
    }

    @Test
    @DisplayName("Recusa texto com letras, mesmo que tenha 11 dígitos")
    void recusaLetras() {
        assertThat(validador.isValid("abc52998224725", null)).isFalse();
    }
}

package com.mizukami.vendas.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        // Só aceita dígitos, ponto, hífen e espaços
        if (!valor.matches("^[0-9.\\-\\s]+$")) {
            return false;
        }
        String d = valor.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }

        int soma1 = 0;
        int soma2 = 0;
        for (int i = 0; i < 9; i++) {
            int n = d.charAt(i) - '0';
            soma1 += n * (10 - i);
            soma2 += n * (11 - i);
        }
        int d1 = (soma1 * 10) % 11;
        if (d1 == 10) {
            d1 = 0;
        }
        soma2 += d1 * 2;
        int d2 = (soma2 * 10) % 11;
        if (d2 == 10) {
            d2 = 0;
        }
        return d1 == d.charAt(9) - '0' && d2 == d.charAt(10) - '0';
    }
}

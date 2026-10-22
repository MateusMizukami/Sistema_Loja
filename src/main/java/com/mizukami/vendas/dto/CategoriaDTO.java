package com.mizukami.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoriaDTO {

    @NotBlank(message = "Informe o nome da categoria")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nome;

    public CategoriaDTO() {}

    public CategoriaDTO(String nome) {
        this.nome = nome;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}

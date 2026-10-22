package com.mizukami.vendas.dto;

import com.mizukami.vendas.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PerfilDTO {

    @NotBlank(message = "Informe seu nome")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nome;

    @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Telefone inválido. Use apenas números, espaços, parênteses e hífen")
    private String telefone;

    @CpfValido(message = "CPF inválido. Confira os números")
    private String cpf;

    public PerfilDTO() {}

    public PerfilDTO(String nome, String telefone, String cpf) {
        this.nome = nome;
        this.telefone = telefone;
        this.cpf = cpf;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}

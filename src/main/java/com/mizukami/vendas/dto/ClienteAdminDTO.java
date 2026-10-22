package com.mizukami.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos que o gerente pode editar no perfil de um cliente (e-mail e perfil ficam de fora). */
public class ClienteAdminDTO {

    @NotBlank(message = "Informe o nome")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    private String nome;

    @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Telefone inválido. Use apenas números, espaços, parênteses e hífen")
    private String telefone;

    @Pattern(regexp = "^$|^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$", message = "CPF inválido. Use o formato 000.000.000-00")
    private String cpf;

    public ClienteAdminDTO() {}

    public ClienteAdminDTO(String nome, String telefone, String cpf) {
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

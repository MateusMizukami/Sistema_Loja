package com.mizukami.vendas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de cadastro público.
 * Não expõe perfil, ativo nem id, para que o visitante não consiga
 * se cadastrar como administrador via manipulação do formulário.
 */
public class CadastroDTO {

    @NotBlank(message = "Informe seu nome completo")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    @Pattern(regexp = "^\\s*\\S+(\\s+\\S+)+\\s*$", message = "Informe nome e sobrenome")
    private String nome;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "Formato de e-mail inválido")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 6, max = 64, message = "A senha deve ter entre 6 e 64 caracteres")
    private String senha;

    @NotBlank(message = "Confirme sua senha")
    private String confirmarSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}

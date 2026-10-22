package com.mizukami.vendas.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    // A regra de mínimo de 6 caracteres deve ser validada no DTO de cadastro.
    @NotBlank(message = "A senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    private String cpf;
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil = Perfil.ROLE_USER;

    @Column(nullable = false)
    private boolean ativo = true;

    // Identificador deste cliente no Asaas, criado no primeiro pagamento.
    @Column(length = 100)
    private String asaasClienteId;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Endereco> enderecos = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetodoPagamento> metodosPagamento = new ArrayList<>();

    public Usuario() {}

    public void adicionarEndereco(Endereco endereco) {
        endereco.setUsuario(this);
        this.enderecos.add(endereco);
    }

    public void adicionarMetodoPagamento(MetodoPagamento metodo) {
        metodo.setUsuario(this);
        this.metodosPagamento.add(metodo);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getAsaasClienteId() { return asaasClienteId; }
    public void setAsaasClienteId(String asaasClienteId) { this.asaasClienteId = asaasClienteId; }
    public List<Endereco> getEnderecos() { return enderecos; }
    public void setEnderecos(List<Endereco> enderecos) { this.enderecos = enderecos; }
    public List<MetodoPagamento> getMetodosPagamento() { return metodosPagamento; }
    public void setMetodosPagamento(List<MetodoPagamento> metodosPagamento) { this.metodosPagamento = metodosPagamento; }
}

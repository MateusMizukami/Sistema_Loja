package com.mizukami.vendas.dto;

import com.mizukami.vendas.models.Endereco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EnderecoDTO {

    @NotBlank(message = "Informe o CEP")
    @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato 00000-000")
    private String cep;

    @NotBlank(message = "Informe o logradouro")
    @Size(max = 150, message = "Máximo de 150 caracteres")
    private String logradouro;

    @NotBlank(message = "Informe o número")
    @Size(max = 20, message = "Máximo de 20 caracteres")
    private String numero;

    @Size(max = 100, message = "Máximo de 100 caracteres")
    private String complemento;

    @NotBlank(message = "Informe o bairro")
    @Size(max = 100, message = "Máximo de 100 caracteres")
    private String bairro;

    @NotBlank(message = "Informe a cidade")
    @Size(max = 100, message = "Máximo de 100 caracteres")
    private String cidade;

    @NotBlank(message = "Informe o estado")
    @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "Use a sigla do estado com 2 letras (ex.: MS)")
    private String estado;

    public static EnderecoDTO de(Endereco e) {
        EnderecoDTO dto = new EnderecoDTO();
        dto.cep = e.getCep();
        dto.logradouro = e.getLogradouro();
        dto.numero = e.getNumero();
        dto.complemento = e.getComplemento();
        dto.bairro = e.getBairro();
        dto.cidade = e.getCidade();
        dto.estado = e.getEstado();
        return dto;
    }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

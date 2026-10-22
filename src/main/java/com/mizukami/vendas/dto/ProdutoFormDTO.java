package com.mizukami.vendas.dto;

import com.mizukami.vendas.models.Produto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProdutoFormDTO {

    @NotBlank(message = "Informe o nome do produto")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    private String nome;

    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String descricao;

    @NotNull(message = "Informe o preço")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Use no máximo 8 dígitos inteiros e 2 casas decimais")
    private BigDecimal preco;

    @NotNull(message = "Informe o estoque")
    @Min(value = 0, message = "O estoque não pode ser negativo")
    @Max(value = 1000000, message = "O estoque máximo é 1.000.000")
    private Integer estoque;

    @NotNull(message = "Selecione uma categoria")
    private Long categoriaId;

    private boolean ativo = true;

    private boolean removerImagem;

    public static ProdutoFormDTO de(Produto p) {
        ProdutoFormDTO dto = new ProdutoFormDTO();
        dto.nome = p.getNome();
        dto.descricao = p.getDescricao();
        dto.preco = p.getPreco();
        dto.estoque = p.getEstoque();
        dto.categoriaId = p.getCategoria() == null ? null : p.getCategoria().getId();
        dto.ativo = p.isAtivo();
        return dto;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public Integer getEstoque() { return estoque; }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isRemoverImagem() { return removerImagem; }
    public void setRemoverImagem(boolean removerImagem) { this.removerImagem = removerImagem; }
}

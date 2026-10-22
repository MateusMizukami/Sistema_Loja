package com.mizukami.vendas.config;

import com.mizukami.vendas.models.Categoria;
import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Produto;
import com.mizukami.vendas.models.Usuario;
import com.mizukami.vendas.repositories.CategoriaRepository;
import com.mizukami.vendas.repositories.ProdutoRepository;
import com.mizukami.vendas.repositories.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Popula o banco com dados iniciais. Roda a cada inicialização,
 * mas só insere quando as tabelas estão vazias.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      CategoriaRepository categoriaRepository,
                      ProdutoRepository produtoRepository,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        criarAdmin();
        criarCatalogo();
    }

    private void criarAdmin() {
        if (usuarioRepository.existsByEmail("admin@loja.com")) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome("Gerente da Loja");
        admin.setEmail("admin@loja.com");
        admin.setSenha(passwordEncoder.encode("admin123"));
        admin.setPerfil(Perfil.ROLE_ADMIN);
        usuarioRepository.save(admin);
    }

    private void criarCatalogo() {
        if (categoriaRepository.count() > 0) {
            return;
        }
        Categoria informatica = categoriaRepository.save(new Categoria("Informática"));
        Categoria smartphones = categoriaRepository.save(new Categoria("Smartphones"));
        Categoria casa = categoriaRepository.save(new Categoria("Casa e Cozinha"));

        produtoRepository.save(produto("Notebook Dev Pro", "Notebook com 16GB de RAM e SSD de 512GB.", "4500.00", 10, informatica));
        produtoRepository.save(produto("Mouse sem fio", "Mouse ergonômico com conexão Bluetooth.", "89.90", 50, informatica));
        produtoRepository.save(produto("Teclado mecânico", "Teclado mecânico com switches vermelhos.", "299.90", 30, informatica));
        produtoRepository.save(produto("Smartphone XYZ", "Celular com câmera de 108MP e tela OLED.", "2100.50", 25, smartphones));
        produtoRepository.save(produto("Fone de ouvido Bluetooth", "Fone com cancelamento de ruído.", "349.00", 40, smartphones));
        produtoRepository.save(produto("Cafeteira elétrica", "Cafeteira com capacidade para 30 xícaras.", "159.99", 15, casa));
    }

    private Produto produto(String nome, String descricao, String preco, int estoque, Categoria categoria) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPreco(new BigDecimal(preco));
        p.setEstoque(estoque);
        p.setAtivo(true);
        p.setCategoria(categoria);
        return p;
    }
}

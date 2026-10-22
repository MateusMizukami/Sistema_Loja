package com.mizukami.vendas.services;

import com.mizukami.vendas.exceptions.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda as imagens dos produtos em disco (pasta uploads/produtos).
 * O nome original do arquivo nunca é usado, o arquivo recebe um nome aleatório e a extensão
 * é decidida pela assinatura do arquivo, não pelo nome ou pelo tipo informado.
 */
@Service
public class ArmazenamentoImagemService {

    public static final long TAMANHO_MAXIMO_BYTES = 2L * 1024 * 1024;
    private static final String PREFIXO_URL = "/uploads/produtos/";

    private final Path pasta;

    public ArmazenamentoImagemService(@Value("${app.upload.dir:uploads}") String pastaBase) {
        this.pasta = Path.of(pastaBase, "produtos").toAbsolutePath().normalize();
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de uploads: " + pasta, e);
        }
    }

    // Salva a imagem e devolve a URL.
    public String salvar(MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RegraDeNegocioException("A imagem deve ter no máximo 2 MB.");
        }

        String extensao;
        try (InputStream in = arquivo.getInputStream()) {
            extensao = extensaoPeloConteudo(in.readNBytes(12));
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível ler a imagem enviada.");
        }
        if (extensao == null) {
            throw new RegraDeNegocioException("Formato inválido. Envie uma imagem JPG, PNG, GIF ou WebP.");
        }

        String nome = UUID.randomUUID() + "." + extensao;
        Path destino = pasta.resolve(nome).normalize();
        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível salvar a imagem. Tente novamente.");
        }
        return PREFIXO_URL + nome;
    }

    // Apaga o arquivo apontado pela URL, se for um upload nosso. Ignora qualquer outra URL.
    public void remover(String url) {
        if (url == null || !url.startsWith(PREFIXO_URL)) {
            return;
        }
        Path alvo = pasta.resolve(url.substring(PREFIXO_URL.length())).normalize();
        if (!alvo.startsWith(pasta)) {
            return; // proteção contra caminhos como ../
        }
        try {
            Files.deleteIfExists(alvo);
        } catch (IOException ignorada) {
        }
    }

    private String extensaoPeloConteudo(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}

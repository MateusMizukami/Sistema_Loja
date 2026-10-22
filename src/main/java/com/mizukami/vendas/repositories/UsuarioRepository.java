package com.mizukami.vendas.repositories;

import com.mizukami.vendas.models.Perfil;
import com.mizukami.vendas.models.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByPerfil(Perfil perfil);

    @Query("select u from Usuario u where u.perfil = :perfil and ("
            + "lower(u.nome) like lower(concat('%', :termo, '%')) "
            + "or lower(u.email) like lower(concat('%', :termo, '%')))")
    Page<Usuario> buscarPorPerfil(@Param("perfil") Perfil perfil,
                                  @Param("termo") String termo,
                                  Pageable pageable);
}

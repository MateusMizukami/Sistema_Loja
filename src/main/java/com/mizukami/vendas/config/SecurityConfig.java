package com.mizukami.vendas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleHabilitado) throws Exception {

        http.authorizeHttpRequests(auth -> {
            // Visitante (público)
            auth.requestMatchers("/", "/busca", "/produtos/**", "/cadastro", "/login",
                    "/acesso-negado", "/error", "/css/**", "/js/**", "/img/**", "/uploads/**", "/favicon.ico", "/webhooks/asaas").permitAll();

            // Console do H2: liberado apenas quando habilitado (desenvolvimento)
            if (h2ConsoleHabilitado) {
                auth.requestMatchers("/h2-console/**").permitAll();
            }

            // Gerente
            auth.requestMatchers("/admin/**").hasRole("ADMIN");

            // Cliente
            auth.requestMatchers("/carrinho/**", "/checkout/**", "/pedidos/**").hasRole("USER");

            // Qualquer usuário logado (cliente ou gerente)
            auth.requestMatchers("/perfil/**").authenticated();

            auth.anyRequest().authenticated();
        });

        http.formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .defaultSuccessUrl("/", false)   // volta para a página que o usuário tentou acessar
                .failureUrl("/login?erro")
                .permitAll());

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

        http.exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"));

        // Webhook do Asaas: não usa sessão nem formulário; é autenticado pelo token no header
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/webhooks/asaas"));

        if (h2ConsoleHabilitado) {
            // O console do H2 usa formulários sem token CSRF e é renderizado em frames
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

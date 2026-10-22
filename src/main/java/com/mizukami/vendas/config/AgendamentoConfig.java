package com.mizukami.vendas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas agendadas (verificação de pagamentos pendentes e expiração de pedidos). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}

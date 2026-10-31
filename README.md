# Loja Vendas

E-commerce em Java com Spring Boot, sistema apresenta catálogo com busca e filtros, carrinho, checkout, pagamento por PIX ou cartão (Asaas), nota fiscal e painel de administração.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Como executar](#como-executar)
- [Configuração](#configuração)
- [Pagamentos com o Asaas (sandbox)](#pagamentos-com-o-asaas-sandbox)
- [Testes](#testes)
- [Principais decisões técnicas](#principais-decisões-técnicas)
- [Limitações e próximos passos](#limitações-e-próximos-passos)
- [Autor](#autor)

## Funcionalidades

O sistema tem três perfis de usuário, com permissões aplicadas no servidor.

| Perfil | O que pode fazer |
|---|---|
| **Visitante** | Ver o catálogo, buscar, filtrar por categoria e faixa de preço, ordenar, cadastrar-se e fazer login |
| **Cliente** | Tudo do visitante, mais: carrinho persistente, checkout, pagamento por PIX ou cartão, histórico de pedidos, nota fiscal, edição dos dados pessoais, troca de senha e gerenciamento de endereços |
| **Gerente** | Dashboard (vendas do mês e estoque baixo), CRUD de produtos com upload de imagem, categorias, gestão de clientes (editar, ativar e desativar) e acompanhamento dos pedidos, incluindo a emissão e o cancelamento de notas fiscais |

### Fluxo de uma compra

1. O cliente adiciona produtos ao carrinho. O estoque é validado e o preço do momento é congelado.
2. No checkout ele escolhe o endereço e a forma de pagamento. O pedido é criado, o estoque é reservado e o Asaas gera o PIX (QR Code e copia e cola) ou a página segura do cartão.
3. Quando o pagamento é confirmado, o pedido passa sozinho para **Pagamento aprovado** (por webhook ou por verificação periódica).
4. O gerente acompanha o pedido, avança o status e emite a nota fiscal.
5. Pedidos não pagos em 30 minutos expiram, e o estoque é devolvido.

## Tecnologias

| Camada | Tecnologia                                              |
|---|---------------------------------------------------------|
| Linguagem | Java                                                    |
| Framework | Spring Boot 4.1, Spring MVC                             |
| Telas | Thymeleaf, HTML e CSS                                   |
| Persistência | Spring Data JPA, Hibernate, MySQL (H2 em memória nos testes) |
| Segurança | Spring Security (BCrypt, controle de acesso por perfil) |
| Integração | API do Asaas (ambiente sandbox) e webhook               |
| Testes | JUnit 5, Mockito, AssertJ, Spring Boot Test             |
| Build | Maven (Maven Wrapper incluído)                          |

## Arquitetura

Arquitetura em camadas, no padrão MVC. O controller nunca acessa o banco, e a regra de negócio fica nos services.

```
Navegador → Controller → Service → Repository → Banco de dados
                ↑            ↑
              DTOs      Entidades
```

```
src/main/java/com/mizukami/vendas
├── config/        Segurança, uploads, agendamento e dados iniciais
├── controllers/   Recebem as requisições e escolhem a tela
├── dto/           Objetos de formulário e de exibição
├── exceptions/    Erros de negócio com mensagem pronta
├── models/        Entidades JPA e enums
├── repositories/  Interfaces Spring Data JPA
├── security/      Integração do usuário com o Spring Security
├── services/      Regras de negócio e integrações (Asaas, nota fiscal)
└── validation/    Validador de CPF

src/main/resources
├── templates/     Páginas Thymeleaf (públicas, cliente e admin/)
└── static/        CSS, JavaScript e imagem padrão
```

Modelo de dados com 12 tabelas: usuários, endereços, categorias, produtos, pedidos, itens de pedido, pagamentos, eventos de pagamento, notas fiscais, itens de nota, sequência de numeração das notas e métodos de pagamento (reservada para evolução).

## Como executar

### Pré-requisitos

- JDK 21 ou superior
- MySQL 8 ou superior em execução (o banco `loja` é criado automaticamente)
- Git

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/MateusMizukami/Sistema_Loja.git
cd NOME_DO_REPOSITORIO

# 2. Informe a senha do seu MySQL (nunca escreva senhas nos arquivos do projeto)
export DB_PASSWORD='sua_senha_do_mysql'

# 3. Execute
./mvnw spring-boot:run
```

Abra http://localhost:8080.

No **IntelliJ**, defina as variáveis em *Run → Edit Configurations → Environment variables* (por exemplo, `DB_PASSWORD=sua_senha`) e execute a classe `VendasApplication`.

### Conta do gerente

Na primeira execução, o sistema cria uma conta de gerente e alguns produtos de exemplo:

| E-mail | Senha |
|---|---|
| `admin@loja.com` | `admin123` |

Essa senha existe só para desenvolvimento. Clientes se cadastram pela própria tela de cadastro.

## Configuração

As principais propriedades ficam em `src/main/resources/application.properties`. Os segredos vêm de **variáveis de ambiente**:

| Variável | Para que serve | Obrigatória |
|---|---|---|
| `DB_PASSWORD` | Senha do MySQL | Sim |
| `ASAAS_API_KEY` | Chave de API do Asaas (sandbox) | Só para pagar |
| `ASAAS_WEBHOOK_TOKEN` | Token que autentica o webhook do Asaas | Só se usar webhook |

```properties
# Banco de dados (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/loja?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Upload de imagens dos produtos
app.upload.dir=uploads

# Asaas (sandbox) e prazo do pedido
app.asaas.api-key=${ASAAS_API_KEY:}
app.asaas.base-url=https://api-sandbox.asaas.com/v3
app.asaas.webhook-token=${ASAAS_WEBHOOK_TOKEN:}
app.pagamento.expiracao-minutos=30
```

> Sem `ASAAS_API_KEY` o sistema funciona normalmente, mas o pagamento online fica indisponível.

## Pagamentos com o Asaas (sandbox)

1. Crie uma conta de testes em [sandbox.asaas.com](https://sandbox.asaas.com) e gere uma chave de API.
2. Cadastre uma **chave PIX** na conta do sandbox. Sem ela o Asaas não gera cobranças por PIX.
3. Defina `ASAAS_API_KEY` (a chave do sandbox começa com `$aact_hmlg_`; em terminal, use aspas simples ao exportar).
4. Informe um CPF válido de teste em *Minha conta*. O CPF é exigido para pagar.
5. Faça um pedido e escolha PIX. Para simular o pagamento, abra a cobrança no painel do sandbox e use **Confirmar pagamento**. O pedido é atualizado em até 30 segundos.

O webhook (`POST /webhooks/asaas`) é opcional em desenvolvimento, pois o Asaas não alcança o `localhost`. Uma tarefa agendada consulta as cobranças pendentes a cada 30 segundos. Para usar o webhook, é preciso um túnel público (como o ngrok) e `ASAAS_WEBHOOK_TOKEN`.

## Testes

```bash
./mvnw test
```

Os testes não usam o MySQL nem a internet: os de integração rodam em um banco H2 em memória (perfil `test`) e o Asaas não é chamado.

| Tipo | O que cobre |
|---|---|
| Unitários (Mockito) | Regras do CRUD de produto e de cliente, cadastro, perfil, troca de senha, catálogo e validação de CPF |
| Integração (H2) | CRUD de produto e de cliente de ponta a ponta, desativação lógica e bloqueio de login |

## Principais decisões técnicas

- **Dinheiro em `BigDecimal`**, para evitar erros de arredondamento.
- **Produto desativado, nunca apagado**, para não quebrar o histórico de pedidos.
- **Snapshot dos dados da compra:** o endereço do pedido, o preço do item e os dados da nota fiscal são copiados no momento da compra.
- **Carrinho = pedido com status `CARRINHO`**, salvo no banco, funcionando entre dispositivos.
- **Baixa de estoque atômica:** um único `UPDATE` condicional (`estoque >= quantidade`) dentro de uma transação, que impede vender a mesma unidade duas vezes.
- **DTOs nos formulários**, para que ninguém envie campos indevidos (por exemplo, o perfil de gerente no cadastro).
- **Cada cliente só acessa o que é seu:** as consultas usam o id do usuário logado, não apenas o id da URL.
- **Pagamento e nota fiscal atrás de interfaces** (`PagamentoGateway` e `EmissorNotaFiscal`), o que permite trocar o provedor sem alterar as regras.
- **Webhook:** autenticado por token (comparação em tempo constante) e com registro do id de cada evento.
- **Upload:** o tipo da imagem é decidido pelo conteúdo do arquivo, com limite de tamanho e nome aleatório.
- **Segredos fora do código**, em variáveis de ambiente.

## Limitações e próximos passos

- O pagamento roda no **ambiente de testes** do Asaas. Produção exige conta aprovada e chave de produção.
- A **nota fiscal é de demonstração**: não é enviada à SEFAZ nem tem chave de acesso. A emissão real exige empresa com CNPJ, certificado digital, dados fiscais dos produtos e um provedor; ela entraria como uma nova implementação de `EmissorNotaFiscal`.

## Autor

**Nome**: Mateus Suegama Mizukami

Disciplina: *PROGRAMAÇÃO PARA WEB* · Instituição: *Universidade Federal de Mato Grosso do Sul - UFMS* · Ano: 2026
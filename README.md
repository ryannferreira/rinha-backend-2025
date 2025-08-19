-----

# Rinha de Backend 2025

Middleware de processamento de pagamentos de alta performance desenvolvido em Java com o framework Vert.x. Ele foi projetado para ser uma solução resiliente e escalável, atuando como um intermediário inteligente entre um cliente e processadores de pagamento externos. O sistema conta com um balanceador de carga, verificações ativas de saúde (*health checks*) e um mecanismo dinâmico de failover para garantir alta disponibilidade.

-----

## Tecnologias Utilizadas (*Tech Stack*)

  * **Linguagem:** Java 21
  * **Framework:** Vert.x 5
  * **Banco de Dados:** PostgreSQL 16
  * **Ferramenta de Build:** Maven
  * **Conteinerização:** Docker & Docker Compose
  * **Balanceador de Carga:** Nginx
  * **Migrações de Banco de Dados:** Flyway

-----

## Arquitetura

O sistema é arquitetado como um conjunto de serviços conteinerizados orquestrados pelo Docker Compose, composto pelas seguintes partes:

* **Gateway (Nginx):** Atua como o ponto de entrada único, responsável pelo balanceamento de carga entre as instâncias da aplicação.
* **API de Pagamentos (Java/Vert.x):** O núcleo do sistema, com duas instâncias rodando em paralelo para garantir alta disponibilidade.
* **Banco de Dados (PostgreSQL):** O repositório central para persistência e consistência de todas as transações de pagamento.
* **Serviços Externos:** A aplicação se comunica com um conjunto de processadores de pagamento externos através de uma rede Docker compartilhada.

-----

## Decisões de Arquitetura

A arquitetura desta aplicação foi projetada com dois objetivos principais em mente: **alta performance** para lidar com picos de carga e **máxima resiliência** para garantir a consistência dos dados, mesmo diante de falhas em serviços externos.

### 1. Gateway e Balanceamento de Carga (Nginx)

O Nginx atua como a porta de entrada e o distribuidor inteligente de tráfego, garantindo que as requisições sejam manipuladas da forma mais eficiente possível.

* **Balanceamento de Carga com `least_conn`:** Em vez de uma simples alternância (`round-robin`), utilizamos a estratégia de "menos conexões". O Nginx redireciona cada nova requisição para a instância da API que possui o menor número de conexões ativas no momento. Isso otimiza a distribuição de carga, garantindo que nenhum servidor fique sobrecarregado desnecessariamente.
* **Conexões Persistentes com `keepalive`:** Mantemos um cache de conexões TCP abertas entre o Nginx e as instâncias da API. Isso elimina a sobrecarga de estabelecer uma nova conexão a cada requisição, reduzindo significativamente a latência e o consumo de CPU em cenários de alto tráfego.

### 2. Lógica da Aplicação e Orquestração (Java/Vert.x & Docker)

A camada de aplicação e sua orquestração são construídas para serem reativas, tolerantes a falhas e escaláveis horizontalmente.

* **Modelo Reativo Não-Bloqueante (Padrão Reactor):** O núcleo da aplicação, construído com Vert.x, opera sobre um Event Loop. Isso permite que um número mínimo de threads gerencie um volume massivo de requisições simultâneas, já que nenhuma operação de I/O (rede ou banco de dados) bloqueia a execução.
* **Alta Disponibilidade com Replicação:** Foram definidas duas instâncias idênticas da API (`api01` e `api02`). O Nginx trabalha em conjunto com essa replicação, garantindo que, se uma instância falhar, o tráfego seja automaticamente direcionado para a instância saudável, sem impacto para o usuário.
* **Failover no Nível da Aplicação:** Além da resiliência da infraestrutura, a própria aplicação possui uma lógica de failover. Se a chamada para um processador de pagamento externo falhar, o sistema automaticamente tenta a operação com um serviço secundário. Isso garante a continuidade do negócio mesmo com dependências parcialmente degradadas.
* **Resposta Rápida com Reconhecimento Assíncrono:** Para minimizar a latência percebida pelo cliente, a API adota o padrão *Asynchronous Request-Acknowledge*. A requisição é validada, persistida e a resposta `202 Accepted` é retornada imediatamente, enquanto o processamento pesado ocorre em segundo plano.

#### **Princípios de Design do Código**

* **Separação em Camadas (Layered Architecture):** O código é estritamente dividido em `Handler` (camada de apresentação/HTTP), `Service` (lógica de negócio) e `Repository` (acesso a dados). Isso promove baixo acoplamento e facilita a manutenção e os testes.
* **Modelo Multi-Verticle:** A aplicação é inicializada por um `MainVerticle` que orquestra a configuração e o deploy do `HttpServerVerticle`. Essa separação de responsabilidades no nível do framework mantém o código da camada web isolado das tarefas de inicialização.

### 3. Camada de Persistência e Otimização

O banco de dados é o pilar da consistência e foi agressivamente otimizado para performance de escrita, uma decisão estratégica para o cenário da Rinha de Backend.

* **Consistência com Persistência Imediata:** Seguindo uma variação do padrão *Transactional Outbox*, a primeira ação de qualquer requisição é salvá-la atomicamente no banco de dados com o status `PENDING`. Isso torna o banco a única fonte da verdade e garante que nenhuma transação seja perdida, mesmo que a aplicação reinicie.

## Endpoints da API

### Criar Pagamento

Aceita e enfileira um novo pagamento para processamento. O processamento é assíncrono.

  * **Endpoint**: `POST /payments`

  * **Cabeçalhos**: `Content-Type: application/json`

  * **Corpo da Requisição**:

    ```json
    {
      "correlationId": "4a7901b8-7d26-4d9d-aa19-4dc1c7cf60b3",
      "amount": "19.90"
    }
    ```

  * **Respostas**:

      * `202 Accepted`: A requisição de pagamento foi recebida com sucesso e enfileirada para processamento.
      * `400 Bad Request`: O corpo da requisição está malformado, faltam campos obrigatórios ou contém tipos de dados inválidos.
      * `500 Internal Server Error`: Ocorreu um erro inesperado no servidor.

### Obter Resumo de Pagamentos

Recupera um resumo agregado de todos os pagamentos processados com sucesso, agrupados por processador.

  * **Endpoint**: `GET /payments-summary`

  * **Parâmetros de Consulta (Opcional)**:

      * `from`: Início da janela de tempo (formato ISO 8601, ex: `2025-01-01T00:00:00Z`).
      * `to`: Fim da janela de tempo (formato ISO 8601, ex: `2025-01-31T23:59:59Z`).

  * **Resposta de Sucesso (`200 OK`)**:

    ```json
    {
      "default": {
        "totalRequests": 1500,
        "totalAmount": 75000.50
      },
      "fallback": {
        "totalRequests": 120,
        "totalAmount": 6000.75
      }
    }
    ```

-----

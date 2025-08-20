# Rinha de Backend 2025

Middleware de processamento de pagamentos de alta performance desenvolvido em Java com o framework Vert.x. Ele foi projetado para ser uma solução resiliente e escalável, atuando como um intermediário inteligente entre um cliente e processadores de pagamento externos.

A arquitetura separa completamente a camada de recebimento de requisições da camada de processamento, utilizando um **modelo híbrido de *threads*** (Event Loop + Worker Pool) e um **barramento de eventos (Event Bus)** para comunicação assíncrona. O sistema conta com um balanceador de carga, verificações ativas de saúde (*health checks*) e um mecanismo dinâmico de failover para garantir máxima disponibilidade e performance.

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

A arquitetura foi desenhada para otimizar a resiliência e a capacidade de processamento sob alta carga, adotando padrões mais avançados do Vert.x e focando na separação de responsabilidades.

### 1\. Gateway e Balanceamento de Carga (Nginx)

O Nginx atua como a porta de entrada e o distribuidor inteligente de tráfego, garantindo que as requisições sejam manipuladas da forma mais eficiente possível.

  * **Balanceamento de Carga com `least_conn`:** Em vez de uma simples alternância (`round-robin`), utilizamos a estratégia de "menos conexões". O Nginx redireciona cada nova requisição para a instância da API que possui o menor número de conexões ativas no momento. Isso otimiza a distribuição de carga, garantindo que nenhum servidor fique sobrecarregado.
  * **Conexões Persistentes com `keepalive`:** Mantemos um cache de conexões TCP abertas entre o Nginx e as instâncias da API. Isso elimina a sobrecarga de estabelecer uma nova conexão a cada requisição, reduzindo significativamente a latência e o consumo de CPU.

### 2\. Lógica da Aplicação e Orquestração (Java/Vert.x)

A camada de aplicação é construída sobre um modelo híbrido para maximizar a performance de I/O e, ao mesmo tempo, garantir que operações potencialmente demoradas não afetem a responsividade do sistema.

  * **Modelo Híbrido: Reativo (Event Loop) + Worker**

      * O `HttpServerVerticle` opera sobre o **Event Loop** do Vert.x. Ele é responsável por aceitar conexões HTTP, validar requisições e interagir com o banco de dados. Como essas são operações de I/O não-bloqueantes, uma única thread pode gerenciar milhares de requisições simultâneas com altíssima eficiência.
      * O `PaymentProcessorVerticle` é implantado como um **Worker Verticle**. Isso significa que cada instância roda em seu próprio pool de threads, separado do Event Loop principal. A sua única responsabilidade é processar pagamentos, o que envolve chamadas de rede para serviços externos que podem ser lentas. Essa separação garante que o processamento pesado nunca bloqueie a thread principal que aceita novas requisições.

  * **Comunicação Desacoplada com Event Bus**

      * Quando o `HttpServerVerticle` recebe uma requisição de pagamento válida, ele a salva no banco com status `PENDING` e imediatamente publica uma mensagem no **Event Bus** interno do Vert.x. Em seguida, retorna a resposta `202 Accepted` ao cliente.
      * As instâncias do `PaymentProcessorVerticle` atuam como consumidores, escutando mensagens no Event Bus. Ao receberem um novo pagamento, elas executam a lógica de chamada aos processadores externos. Esse padrão desacopla totalmente o recebimento da requisição do seu processamento.

  * **Resiliência com Health Check Ativo e Failover Dinâmico**

      * Um `HealthCheckerService` monitora ativamente os processadores de pagamento externos em intervalos regulares, verificando sua disponibilidade e tempo de resposta.
      * O `HealthStatusService` centraliza o estado de saúde dos processadores. Com base nos dados do *health check*, ele elege o "melhor" processador, priorizando o que está saudável e, em caso de empate, o que tem o menor tempo de resposta.
      * A lógica de `failover` é reativa: o `PaymentProcessorVerticle` primeiro tenta a operação com o processador primário (o "melhor"). Se a chamada falhar, o fluxo é automaticamente recuperado (`.recover`), e uma nova tentativa é feita com o processador secundário, garantindo a continuidade do negócio.

#### **Princípios de Design do Código**

  * **Separação em Camadas (Layered Architecture):** O código é estritamente dividido em `Handler`, `Service` e `Repository`.
  * **Modelo Multi-Verticle:** A aplicação é dividida em `HttpServerVerticle` (para a camada web) e `PaymentProcessorVerticle` (para a camada de negócio), cada um com responsabilidades e modelos de *threading* distintos, orquestrados pelo `MainVerticle`.

### 3\. Camada de Persistência e Otimização

O banco de dados é o pilar da consistência e foi agressivamente otimizado para performance de escrita.

  * **Consistência com Persistência Imediata:** Seguindo o padrão *Transactional Outbox*, a primeira ação de qualquer requisição é salvá-la atomicamente no banco de dados com o status `PENDING`. Isso torna o banco a única fonte da verdade e garante que nenhuma transação seja perdida.

-----

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
      * `400 Bad Request`: O corpo da requisição está malformado.
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

# todo-envers-lab — laboratório Hibernate Envers

Projeto de estudo para discutir auditoria de entidades com exemplos reproduzíveis em Spring Boot e PostgreSQL.

## Estudar o Envers com esta aplicação

A [trilha de estudo por tópicos](docs/envers/README.md) prioriza os fundamentos pelo princípio de Pareto. Comece pelos seis tópicos essenciais e pratique na aplicação; depois avance para estratégias, relacionamentos e decisões de produção. Cada tópico inclui exemplos, consultas SQL, evidências disponíveis, perguntas de compreensão e referências oficiais do Hibernate 6.6.

## Base utilizada

| Componente | Escolha |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.16 |
| HTTP e validação | Spring Web e Jakarta Validation |
| Persistência | Spring Data JPA / Hibernate ORM |
| Auditoria | Hibernate Envers, gerenciado pelo Spring Boot (6.6.53.Final) |
| Banco | PostgreSQL 16 |
| Estrutura do banco | Flyway; Hibernate valida o esquema |
| Testes | JUnit, MockMvc e Testcontainers com PostgreSQL real |
| Interface | HTML, CSS e JavaScript servidos pela própria API |

O projeto de agendamento foi referência da stack Java/Spring. As regras, configurações e integrações governamentais não são necessárias neste laboratório.

## Executar

Pré-requisitos: Java 21, Maven 3.9 e Docker com Compose. Os testes também precisam de acesso ao Docker.

```bash
docker compose up -d --wait
mvn spring-boot:run
```

Abra **http://localhost:8080**. A aplicação escuta somente em `127.0.0.1` por padrão. Se a porta estiver ocupada, execute `PORT=8085 mvn spring-boot:run` e abra a nova porta.

O PostgreSQL deste projeto é publicado em `127.0.0.1:5433`, banco `todo`, usuário `todo`. A senha padrão `todo_local` é exclusiva de desenvolvimento local. Para alterá-la antes da primeira inicialização, defina `DB_PASSWORD` tanto no Compose quanto na aplicação. Alterar essa variável após a criação do volume não altera a senha do usuário já existente no PostgreSQL.

Configurações opcionais: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `SERVER_ADDRESS`. `DB_PORT` altera a porta publicada pelo Compose; ajuste também a porta em `DB_URL` ao executar a API.

Os dados ficam no volume Docker `todo-envers-lab_todo_data`. Para parar o banco preservando os dados:

```bash
docker compose stop
```

## Primeiro experimento

1. Informe o autor `Ana` e crie “Estudar Envers”.
2. Troque o autor para `Bruno` e edite o título para “Estudar auditoria”.
3. Conclua a tarefa e veja a nova revisão.
4. Edite e salve sem alterar nenhum valor: a contagem de revisões deve permanecer igual.
5. Exclua a tarefa. O histórico permanece visível e pode ser consultado pelo ID.

O campo “Autor do experimento” envia o cabeçalho `X-Autor`. **É uma identidade simulada, sem autenticação.** Não prova quem fez a alteração. Em uma aplicação real, a identidade deve vir de uma fonte autenticada. Sem o cabeçalho, o autor HTTP é `anonimo-laboratorio`; operações internas usam `sistema`.

## API

| Método | Rota | Comportamento |
|---|---|---|
| POST | `/api/tarefas` | Cria uma tarefa pendente; retorna 201 e Location |
| GET | `/api/tarefas?pagina=0&tamanho=20` | Lista o estado atual; página inicial 0, tamanho máximo 100 |
| GET | `/api/tarefas/{id}` | Consulta uma tarefa atual |
| PUT | `/api/tarefas/{id}` | Atualiza título, descrição e conclusão |
| DELETE | `/api/tarefas/{id}` | Exclui a tarefa e preserva a auditoria |
| GET | `/api/tarefas/{id}/historico?pagina=0&tamanho=20` | Revisa eventos da tarefa, do mais recente ao mais antigo |
| GET | `/api/tarefas/{id}/historico/{revisao}` | Consulta a revisão exata daquela tarefa |

Corpo de criação:

```json
{"titulo":"Estudar Envers","descricao":"Preparar demonstração"}
```

Corpo de atualização completa:

```json
{"titulo":"Estudar auditoria","descricao":"Preparar demonstração","concluida":true}
```

Uma consulta ao histórico de um ID sem revisões retorna lista vazia. A rota da revisão exata retorna 404 quando a tarefa não participou daquela revisão. Não representa uma consulta ao estado em qualquer ponto do tempo; esse é outro experimento com `AuditReader.find`.

## O que observar no banco

```bash
docker compose exec postgres psql -U todo -d todo
```

```sql
-- Estado atual
SELECT * FROM tarefa ORDER BY id;

-- Metadados de cada revisão; instante é epoch em milissegundos
SELECT id, to_timestamp(instante / 1000.0) AS instante, autor
FROM revisao ORDER BY id;

-- Histórico: 0 = criação, 1 = alteração, 2 = exclusão
SELECT id, rev, revtype, titulo, concluida, titulo_mod, concluida_mod
FROM tarefa_aud ORDER BY rev, id;
```

`@Audited(withModifiedFlag = true)` habilita snapshots e indicadores de campos modificados. `Revisao` e `RevisaoListener` acrescentam autor e instante. `store_data_at_delete=true` preserva o conteúdo na revisão de exclusão. `HistoricoService` usa `AuditReader` diretamente, sem depender do módulo Spring Data Envers, para estudar a API nativa.

As listas `camposModificados` devem ser interpretadas nas revisões de alteração. Criação e exclusão são identificadas pelo tipo de operação.

## Testes

```bash
mvn test
```

O Testcontainers cria um PostgreSQL temporário, independente do banco do Compose. Não utiliza banco em memória. No Docker 29, se o cliente Docker do Testcontainers reclamar da versão mínima da API, execute:

```bash
mvn -Dapi.version=1.44 test
```

Os testes verificam:

- Criação, edição, consulta e exclusão via HTTP, incluindo snapshots históricos e autor.
- Identificação dos campos modificados.
- Rollback sem dados nem histórico persistidos.
- Duas tarefas modificadas na mesma transação compartilhando a revisão.
- Atualização sem mudança não gerando revisão adicional.
- SQL direto alterando o estado atual sem gerar auditoria Envers.
- Validação de entradas, recursos inexistentes e consulta de revisão exata.
- Disponibilidade do HTML inicial.

## Roteiro de evolução

Esta primeira base cobre CRUD, auditoria básica, metadados personalizados, flags de modificação e tela de experimentação. Não cobre todos os recursos do Envers.

1. **Base funcional:** validar CRUD, persistência e contrato HTTP. Critério: operações e erros esperados passando no PostgreSQL.
2. **Auditoria fundamental:** validar os cenários acima e inspecionar as três tabelas. Critério: snapshots e revisões correspondem às transações confirmadas.
3. **Experimentos avançados:** campos `@NotAudited`, entidades relacionadas e coleções, consultas por data/campo, `AuditReader.find`, comparação entre `DefaultAuditStrategy` e `ValidityAuditStrategy`. Critério: cada experimento terá teste e explicação do efeito no banco. Comparar estratégias em bancos isolados, com migrações adequadas.
4. **Demonstração e discussão:** evoluir a tela com comparação visual antes/depois e identidade autenticada. Medir custo de escrita/consulta e crescimento do histórico com carga controlada antes de afirmar capacidade.

Veja [o roteiro de estudo](docs/roteiro-auditoria.md) para perguntas de discussão e limites.

Veja também [a validação inicial](docs/validacao-inicial.md), com o resultado dos testes e a demonstração executada pela interface.

## Referências

- [Hibernate Envers](https://hibernate.org/orm/envers/)
- [Guia Envers da série Hibernate 6.6](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers)
- [Dependências gerenciadas pelo Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html)

As versões foram fixadas para reproduzir este laboratório. Não há alegação de que sejam as versões mais recentes de todas as linhas.

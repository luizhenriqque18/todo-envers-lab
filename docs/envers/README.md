# Envers na prática: trilha de estudo com o ToDo

Objetivo: explicar e avaliar uma solução de auditoria de entidades usando esta aplicação, com exemplos que você consegue conferir no código, na tela e no PostgreSQL.

## Como aplicar Pareto nesta trilha

Comece pelos tópicos **01 a 06**. Eles foram priorizados para responder às perguntas mais frequentes de implementação e discussão: o que é registrado, quando, como consultar, de onde vem a autoria e o que fica fora da cobertura. “80/20” é uma orientação de foco; não é uma garantia de domínio de 80% do Envers.

Leia um tópico, faça o experimento e explique a resposta à pergunta final com suas palavras. Se não conseguir, retorne ao exemplo antes de avançar. Os tempos abaixo são sugestões de estudo, não medições.

| Ordem | Tópico | Pergunta principal | Tempo sugerido |
|---|---|---|---|
| 01 — essencial | [Fundamentos](01-fundamentos.md) | Histórico e controle de concorrência são a mesma coisa? | 15 min |
| 02 — essencial | [Mapeamento e tabelas](02-mapeamento-e-tabelas.md) | Onde os dados históricos ficam e quais campos entram? | 20 min |
| 03 — essencial | [Transações e revisões](03-transacoes-e-revisoes.md) | Cada clique, SQL ou entidade gera uma revisão? | 25 min |
| 04 — essencial | [Consulta do histórico](04-consulta-do-historico.md) | Qual a diferença entre um evento e o estado em uma revisão? | 25 min |
| 05 — essencial | [Autoria e metadados](05-autoria-e-metadados.md) | Quem alterou e como essa identidade chegou à auditoria? | 20 min |
| 06 — essencial | [Limites e cuidados](06-limites-e-cuidados.md) | Quais caminhos de alteração escapam? | 20 min |
| 07 — aprofundamento | [Estratégias de auditoria](07-estrategias-de-auditoria.md) | O que comparar antes de mudar a estratégia? | 25 min |
| 08 — aprofundamento | [Relacionamentos](08-relacionamentos.md) | O histórico da tarefa aponta para qual versão da lista? | 25 min |
| 09 — aprofundamento | [Adoção em produção](09-adocao-em-producao.md) | Quais decisões faltam para sair do laboratório? | 25 min |

## Antes dos experimentos

Siga o [README da aplicação](../../README.md) para iniciar PostgreSQL e API. A tela fica em `http://localhost:8080` na configuração padrão. Use tarefas de estudo novas; anote o ID e os números de revisão retornados. Os números ilustrativos destes textos não são IDs garantidos do seu banco.

Para observar as tabelas:

```bash
docker compose exec postgres psql -U todo -d todo
```

Execute os comandos na raiz do projeto. No `psql`, configure `\set id_tarefa 1` substituindo `1` pelo ID da sua tarefa; as consultas dos tópicos usam `:id_tarefa`.

## Como interpretar as evidências

- **Implementado:** o comportamento está no código atual.
- **Teste existente:** há um teste que verifica o cenário. A execução anterior está registrada na [validação inicial](../validacao-inicial.md); a criação desta trilha não representa nova execução dos testes.
- **Proposto:** exercício que exige código, esquema ou teste adicional. Os trechos são sugestões, não funcionalidades prontas.
- **Resultado esperado:** previsão para você conferir ao executar o exercício; não é um resultado medido nesta etapa de documentação.

A trilha não modifica a aplicação nem acrescenta os recursos propostos. A aplicação utiliza Spring Boot 3.5.16 e Hibernate/Envers 6.6.53.Final, conforme a base e sua validação inicial. O guia online da série 6.6 pode mostrar um patch posterior; confirme APIs e configurações na versão resolvida ao evoluir o projeto.

## Critério de compreensão do núcleo

Você deve conseguir demonstrar uma tarefa criada, alterada e excluída; relacionar seus snapshots às revisões; explicar rollback e revisão compartilhada; distinguir autor informado de identidade autenticada; e apontar um caminho que altera o banco sem gerar histórico Envers.

Para a discussão com a equipe, comece por: **“Quais alterações precisamos rastrear e por quais caminhos elas chegam ao banco?”** Use as respostas para avaliar a cobertura.

## Fonte e escopo

Referência principal: [capítulo 24 — Envers, Hibernate ORM 6.6](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers). Cada tópico aponta para as seções relevantes. As instruções de tela, SQL, testes e decisões de adoção foram elaboradas para este laboratório; não são transcrições do guia.

Esta é uma trilha priorizada, não um catálogo completo. Rastreamento de tipos de entidades alteradas, herança, mapeamentos especiais e particionamento detalhado podem ser estudados depois, conforme a necessidade.

**Começar:** [01 — Fundamentos](01-fundamentos.md).

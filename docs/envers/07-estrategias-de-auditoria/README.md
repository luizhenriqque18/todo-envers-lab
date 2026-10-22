# 07 — Estratégias de auditoria

**Prioridade:** aprofundamento. **Pergunta:** qual estratégia combina com minhas consultas e escritas?

## O ponto de partida do laboratório

A configuração atual não define `org.hibernate.envers.audit_strategy`. Assim, usa a estratégia padrão da série 6.6, `DefaultAuditStrategy`. A [migração V1](../../../src/main/resources/db/migration/V1__tarefas_e_auditoria.sql) guarda `rev`, mas não possui `revend`.

**Implementado:** esquema e consultas da base padrão. **Proposto:** comparação com `ValidityAuditStrategy`. Não existe medição comparativa neste projeto.

## Modelo mental mínimo

Para uma tarefa que muda nas revisões ilustrativas 10 e 12:

| Estratégia | Representação conceitual do primeiro snapshot |
|---|---|
| Padrão | Começou na revisão 10; a consulta encontra a mudança seguinte |
| Validade | Começou na revisão 10 e deixou de valer na revisão 12 |

A estratégia de validade acrescenta o limite final e atualiza esse limite no registro anterior. Isso muda tanto o esquema quanto os comandos de escrita. O intervalo conceitual é `[início, fim)`: a revisão final já pertence ao novo estado. A escolha envolve custo de escrita e consulta; não há resultado de desempenho medido aqui. [Comparação oficial](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-audit-strategy).

## Experimento que você já pode fazer

No banco atual, observe as colunas e os snapshots de uma tarefa editada duas vezes:

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'tarefa_aud'
ORDER BY ordinal_position;

SELECT id, rev, revtype, titulo
FROM tarefa_aud WHERE id = :id_tarefa ORDER BY rev;
```

**Resultado esperado:** não há coluna `revend`. A consulta confirma a estrutura da base, não mede sua velocidade. A existência de índices e o padrão real das consultas também entram na comparação.

## Experimento proposto com validade

O fragmento abaixo seria usado em uma futura configuração isolada:

```properties
# Proposta; não adicionar à configuração atual sem preparar o esquema.
spring.jpa.properties.org.hibernate.envers.audit_strategy=org.hibernate.envers.strategy.ValidityAuditStrategy
```

Para realizar uma comparação reproduzível:

1. Prepare dois bancos de estudo novos, um por estratégia, sem mudar o banco da demonstração.
2. Gere e revise as migrações correspondentes. O banco de validade precisa das colunas e restrições apropriadas; somente alterar a propriedade não basta com `ddl-auto=validate`.
3. Execute a mesma sequência de criações, alterações e exclusões nos dois bancos.
4. Confira se as consultas recuperam os mesmos estados e verifique o fechamento dos intervalos no banco de validade.
5. Meça separadamente escrita e consulta, com volume, concorrência, mediana, p95, erros e ambiente registrados. Inspecione planos SQL antes de atribuir diferenças à estratégia.

Critério funcional de aceitação: os mesmos estados históricos são recuperados e os intervalos estão coerentes. Critério de desempenho: evidência suficiente para o volume e as consultas escolhidas; nenhum número do guia substitui essa medição.

## Armadilha e decisão

Não migre um histórico preenchido apenas adicionando a coluna de fim e alterando a propriedade. A validade dos registros antigos precisa ser preparada e verificada. Por isso, o primeiro experimento usa bancos novos. Também não confunda a estratégia de auditoria com a estratégia de geração do ID da revisão ou com a nomenclatura das flags.

## Explique com suas palavras

“Por que uma estratégia que facilita algumas consultas pode acrescentar trabalho na gravação?”

## Referência oficial

- [Choosing an audit strategy](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-audit-strategy).
- [Configuring the ValidityAuditStrategy](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-audit-ValidityAuditStrategy).

[Anterior](../06-limites-e-cuidados/README.md) · [Índice](../README.md) · [Próximo: relacionamentos](../08-relacionamentos/README.md)

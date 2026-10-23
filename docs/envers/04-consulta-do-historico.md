# 04 — Consultar o histórico e identificar mudanças

**Prioridade:** essencial. **Pergunta:** quero o evento de uma revisão ou o estado que valia nela?

## A consulta implementada

O [HistoricoService](../../src/main/java/br/com/laboratorio/todo/audit/HistoricoService.java) usa um `EntityManager` dentro de transação de leitura:

```java
AuditReaderFactory.get(entityManager).createQuery()
    .forRevisionsOfEntityWithChanges(Tarefa.class, true)
    .add(AuditEntity.id().eq(id));
```

Trecho extraído da aplicação. O `true` inclui revisões de exclusão. Cada resultado é convertido pelo serviço a partir de quatro posições:

| Posição | Conteúdo usado no projeto |
|---|---|
| 0 | Snapshot de `Tarefa` |
| 1 | Entidade personalizada `Revisao` |
| 2 | `RevisionType`, convertido em criação, alteração ou exclusão |
| 3 | Conjunto dos nomes das propriedades modificadas |

A documentação 6.6 classifica essa consulta com nomes de propriedades modificadas como **experimental**. A decisão do laboratório é estudá-la com teste; em uma adoção real, inclua essa API na validação de atualização de versão. Isso não significa que todo o Envers seja experimental. Veja a [seção específica](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-tracking-obtain-properties-changed-queries).

## Duas perguntas diferentes

Considere este exemplo hipotético: a tarefa A muda nas revisões 10 e 12; a tarefa B muda na revisão 11.

| Pergunta | Resultado que faz sentido |
|---|---|
| A tarefa A foi alterada exatamente na revisão 11? | Não há evento de A nessa revisão |
| Como estava A quando ocorreu a revisão 11? | Estado de A que começou na revisão 10 |

**Implementado:** `GET /api/tarefas/{id}/historico/{revisao}` filtra igualdade de revisão. No primeiro caso retorna 404. O [teste `revisaoExataNaoRetornaEstadoDeOutraRevisao`](../../src/test/java/br/com/laboratorio/todo/AuditoriaIntegrationTest.java) verifica essa distinção no contrato atual.

**Proposto:** consulta do estado vigente com `AuditReader.find`. Exemplo para um futuro método transacional, com `id` e `numeroRevisao` recebidos como parâmetros:

```java
// Exemplo proposto; não existe rota para este comportamento.
var reader = AuditReaderFactory.get(entityManager);
Tarefa snapshot = reader.find(Tarefa.class, id, numeroRevisao);
```

Na configuração padrão da base, a busca pode recuperar o estado de revisão anterior. A propriedade `find_by_revision_exact_match` altera essa semântica; não a confunda com o filtro explícito usado pela nossa rota. Antes da criação ou depois da exclusão, a consulta de estado exige tratamento de ausência; consultar eventos de exclusão é outra operação.

## Experimento implementado

1. Crie uma tarefa com título “Consultar histórico”.
2. Modifique somente a descrição.
3. Abra “Histórico” e confira `camposModificados` na alteração.
4. Anote o número de revisão retornado e abra a rota exata no navegador.

Use os IDs retornados, substituindo os valores ilustrativos:

```text
GET /api/tarefas/1/historico?pagina=0&tamanho=20
GET /api/tarefas/1/historico/2
```

```sql
SELECT rev, descricao, descricao_mod, titulo_mod
FROM tarefa_aud WHERE id = :id_tarefa ORDER BY rev;
```

**Resultado esperado:** o snapshot mantém os demais valores, mas a lista de campos da alteração destaca `descricao`. O teste de CRUD já cobre indicadores para uma alteração de título e conclusão; este exercício manual isola a descrição.

## Decisões e armadilhas

As rotas de histórico são paginadas e ordenadas por revisão decrescente. A página inicial não representa necessariamente todo o histórico. Identificar campos modificados também não produz, sozinho, uma comparação visual antes/depois: a interface atual mostra snapshots; uma comparação requer buscar e comparar os estados apropriados.

Um próximo experimento pode filtrar `AuditEntity.property("titulo").hasChanged()` com as flags habilitadas. Ainda não há essa opção na API. As flags referem-se a propriedades Java; observe `criadaEm` no código e `criada_em` no SQL.

## Explique com suas palavras

“Por que um 404 na rota da revisão exata não significa que a tarefa não existia naquele momento?”

## Referência oficial

- [Consultas Envers](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-queries).
- [Flags de propriedades](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-tracking-properties-changes).
- [Filtro por propriedade modificada](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-tracking-properties-changes-queries).
- [Configuração da correspondência de revisão](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-configuration).

[Anterior](03-transacoes-e-revisoes.md) · [Índice](README.md) · [Próximo: autoria](05-autoria-e-metadados.md)

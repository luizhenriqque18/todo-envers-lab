# 06 — Limites e cuidados

**Prioridade:** essencial. **Pergunta:** em quais situações o histórico pode ficar incompleto?

## Parta dos caminhos de escrita

O [TarefaService](../../../src/main/java/br/com/laboratorio/todo/tarefa/TarefaService.java) carrega entidades gerenciadas para atualizar ou remover. O [teste `sqlDiretoAlteraEstadoAtualSemGerarAuditoria`](../../../src/test/java/br/com/laboratorio/todo/AuditoriaIntegrationTest.java) utiliza outro caminho: um `UPDATE` por `JdbcTemplate`.

Nesse teste, o título atual passa de “Original” para “SQL direto”, enquanto o histórico continua com um único snapshot, “Original”. Essa diferença é o exemplo mais importante deste tópico.

| Caminho ou requisito | Situação na base |
|---|---|
| Escrita de entidade auditada pelo serviço | Implementada e coberta por teste |
| SQL direto por JDBC | Teste existente demonstra ausência de nova revisão |
| SQL executado por outro sistema ou manualmente | Deve entrar no levantamento dos caminhos externos |
| `CriteriaUpdate` / `CriteriaDelete` em massa | O guia oficial alerta que não são capturados |
| Leitura de tarefa | Não há trilha de acesso implementada |
| Impedir adulteração do histórico por usuários SQL | Não há proteção específica implementada |

Não deduza que uma operação em massa é auditada só porque usa JPA. Inspecione também atualizações em massa JPQL/HQL, métodos de repositório com comandos próprios e rotinas de manutenção. Esses caminhos exigem avaliação e testes específicos. [Alerta oficial sobre operações Criteria em massa](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-basics).

## Experimento com teste existente

Na raiz do projeto:

```bash
mvn -Dapi.version=1.44 '-Dtest=AuditoriaIntegrationTest#sqlDiretoAlteraEstadoAtualSemGerarAuditoria' test
```

Leia as três verificações do método: título atual alterado, apenas uma revisão e snapshot antigo preservado. O PostgreSQL é temporário e separado do banco da tela. O parâmetro de API Docker corresponde ao ajuste usado neste ambiente.

**Resultado esperado:** o teste passa demonstrando a lacuna de cobertura. Isso é evidência de um limite conhecido, não de falha do teste.

Para observar a mesma distinção sem modificar nada manualmente, confira no banco da tela:

```sql
SELECT id, titulo FROM tarefa WHERE id = :id_tarefa;
SELECT id, rev, titulo FROM tarefa_aud
WHERE id = :id_tarefa ORDER BY rev;
```

Essas consultas apenas exibem estados; se a tarefa foi alterada só pelos caminhos auditados, não se espera reproduzir a divergência do teste.

## Decisões que surgem da cobertura

Para o sistema real, liste primeiro todas as origens de escrita. Se todas puderem passar por um fluxo Hibernate controlado, avalie o Envers nesse contexto. Se for obrigatório observar escritas externas, será necessário avaliar mecanismos complementares, como captura no banco ou CDC. A escolha depende do requisito; esta base não compara essas soluções.

Auditar acesso, provar autoria, proteger o histórico contra adulteração e restaurar registros são requisitos adicionais. O [autor simulado](../05-autoria-e-metadados/README.md) e os snapshots atuais não demonstram essas capacidades.

## Exercício de decisão

Escreva uma lista das origens de alteração do sistema que você pretende discutir: API, batch, scripts, integração e manutenção. Para cada uma, marque se usa entidades gerenciadas e qual teste provaria sua cobertura. Esse levantamento é proposto; não foi realizado sobre outro projeto nesta entrega.

## Explique com suas palavras

“Se duas aplicações alteram a mesma tabela e apenas uma usa Envers, posso prometer histórico completo?”

## Referência oficial

- [Basics: eventos de persistência e limitação das operações em massa](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-basics).
- [Entidade de revisão personalizada](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-revisionlog).

[Anterior](../05-autoria-e-metadados/README.md) · [Índice](../README.md) · [Próximo: estratégias](../07-estrategias-de-auditoria/README.md)

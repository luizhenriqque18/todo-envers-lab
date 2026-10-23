# 08 — Relacionamentos e histórico

**Prioridade:** aprofundamento. **Pergunta:** ao abrir uma tarefa antiga, qual nome de lista devo mostrar?

## Use uma evolução pequena do ToDo

Hoje, [Tarefa](../../src/main/java/br/com/laboratorio/todo/tarefa/Tarefa.java) tem apenas campos simples. Não existe entidade `Lista`, associação JPA ou tabela de auditoria de lista. O roteiro a seguir é **proposto**, não uma funcionalidade da tela.

Imagine a seguinte sequência:

1. Criar a lista “Estudos”.
2. Criar uma tarefa vinculada à lista.
3. Renomear a lista para “Preparação técnica”.
4. Consultar a tarefa no instante anterior à renomeação.

A pergunta de negócio vem antes da anotação: você precisa do nome antigo da lista ou aceita apresentar seu nome atual ao lado da tarefa antiga?

## Duas decisões de modelagem

| Necessidade | Proposta de estudo |
|---|---|
| Recuperar também os dados antigos da lista | Auditar `Lista` e testar a navegação histórica da associação |
| Preservar o vínculo com um cadastro de referência consultado no estado atual | Estudar `targetAuditMode = RelationTargetAuditMode.NOT_AUDITED` |

Na segunda opção, o alvo consultado é atual. Isso não equivale a preservar o estado histórico dele. Também é diferente de `@NotAudited`, que exclui uma propriedade do histórico. [Mapeamentos adicionais e alvo não auditado](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-additional-mappings).

## Esboço proposto

Fragmento para uma futura associação, não presente na entidade atual. `Lista` precisaria existir e ser auditada neste primeiro cenário:

```java
// Proposta dentro de Tarefa; requer entidade Lista e nova migração.
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "lista_id")
private Lista lista;
```

Antes de implementar, defina obrigatoriedade, propriedade da relação e comportamento ao remover uma lista. Uma coleção inversa com `mappedBy` merece teste próprio; não presuma que toda mudança de coleção funcione como alteração de um campo simples.

## Preparação e critérios do experimento

1. Adicione `Lista`, sua auditoria e as migrações das tabelas atuais e históricas, incluindo o vínculo da tarefa.
2. Implemente uma forma de criar/renomear a lista e vincular ou mover a tarefa.
3. Escreva testes com transações separadas para criação, renomeação e mudança de vínculo.
4. Consulte os snapshots dentro de transação, convertendo os dados necessários para DTOs antes de encerrar a sessão.
5. Verifique no banco os IDs de relação e as revisões de ambas as entidades.

**Resultado esperado para o cenário com ambas auditadas:** a consulta histórica deve recuperar o nome da lista compatível com a revisão solicitada. Para um cenário separado com alvo não auditado, o teste deve evidenciar que o nome vem do cadastro atual.

Teste ainda o alvo removido. Não adicione uma configuração para ignorar relações ausentes sem decidir o que a ausência deve significar para a aplicação. Com `open-in-view=false`, evite devolver entidades com associações lazy diretamente ao navegador.

## O que observar agora no banco

Sem criar nada, você pode confirmar a estrutura atual:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

**Resultado esperado na base criada pelo projeto:** tabelas de tarefa, revisão e controle do Flyway; não há `lista` nem `lista_aud`. Os comandos de consulta dessas futuras tabelas só serão definidos após escolher e implementar o mapeamento.

## Explique com suas palavras

“Mostrar uma tarefa antiga junto com o nome atual de sua lista atende ao requisito de auditoria da equipe?”

Explique qual requisito levaria a cada escolha e como um teste demonstraria a diferença.

## Referência oficial

- [Additional mapping annotations](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-additional-mappings).
- [Mapping exceptions](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-mappingexceptions).
- [Configuração de mudanças de coleções](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-configuration).

[Anterior](07-estrategias-de-auditoria.md) · [Índice](README.md) · [Próximo: produção](09-adocao-em-producao.md)

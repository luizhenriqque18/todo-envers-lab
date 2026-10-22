# 01 — Fundamentos: histórico de estados

**Prioridade:** essencial. **Pergunta:** o que significa versionar uma tarefa?

## Um exemplo pequeno

Você cria “Estudar Envers”, troca o título para “Estudar auditoria” e conclui a tarefa. A lista atual mostra apenas o último estado. O histórico permite observar os estados anteriores.

| Momento ilustrativo | Estado atual após a ação | Registro histórico esperado |
|---|---|---|
| Criar | Estudar Envers, pendente | Snapshot de criação |
| Editar título | Estudar auditoria, pendente | Novo snapshot |
| Concluir | Estudar auditoria, concluída | Novo snapshot |

Neste projeto, um snapshot é uma representação dos campos auditados de `Tarefa` naquela revisão. Mesmo mudando apenas `concluida`, a linha histórica contém também o título e a descrição. As flags de modificação ajudam a distinguir quais valores mudaram.

## Três conceitos que você precisa separar

| Conceito | Pergunta que responde | Situação no ToDo |
|---|---|---|
| Estado atual | Como está a tarefa agora? | `tarefa` e `GET /api/tarefas/{id}` |
| Histórico Envers | Como estavam os campos auditados em uma revisão? | `tarefa_aud` e rotas de histórico |
| Concorrência otimista JPA | Minha edição conflita com uma edição simultânea? | `@Version` ainda não implementado |

Ter histórico não resolve a disputa entre dois usuários que editaram o mesmo registro. Acrescentar `@Version` exige também definir como a API comunica e trata conflitos. E consultar um snapshot não restaura automaticamente a tarefa atual.

## Onde observar no código

- [Tarefa.java](../../../src/main/java/br/com/laboratorio/todo/tarefa/Tarefa.java): entidade auditada com `@Audited(withModifiedFlag = true)`.
- [TarefaService.java](../../../src/main/java/br/com/laboratorio/todo/tarefa/TarefaService.java): alterações do estado atual.
- [HistoricoService.java](../../../src/main/java/br/com/laboratorio/todo/audit/HistoricoService.java): leitura dos snapshots sem alterar a tarefa.

**Implementado:** estado atual e histórico. **Teste existente:** `crudHttpRegistraAutorCamposEExclusaoComSnapshot`, em [AuditoriaIntegrationTest](../../../src/test/java/br/com/laboratorio/todo/AuditoriaIntegrationTest.java).

## Experimento

1. Crie uma tarefa nova com o título “Fundamentos do Envers”. Anote o ID.
2. Edite somente o título e salve.
3. Conclua a tarefa pelo botão da tela.
4. Consulte o histórico e compare os três snapshots com a lista atual.

No `psql`, configure o ID conforme o [guia de entrada](../README.md) e execute:

```sql
SELECT id, titulo, concluida FROM tarefa WHERE id = :id_tarefa;
SELECT id, rev, revtype, titulo, concluida
FROM tarefa_aud WHERE id = :id_tarefa ORDER BY rev;
```

**Resultado esperado:** uma linha atual e três linhas históricas para esse roteiro, cada ação confirmada em uma transação separada. Os números de revisão podem ter intervalos.

## Armadilha e decisão

Evite explicar o Envers como um gravador de todos os comandos SQL ou como um sistema de recuperação automática. Para esta aplicação, apresente o que existe: histórico de entidades auditadas e uma consulta para visualizar os estados.

## Explique com suas palavras

“Se eu já tenho histórico, por que ainda posso precisar de `@Version`?”

Você entendeu quando consegue separar rastreabilidade do passado e tratamento de edições simultâneas, usando a mesma tarefa como exemplo.

## Referência oficial

- [Envers — Basics](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-basics).
- [Configuração: campo de concorrência otimista](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-configuration).

[Índice](../README.md) · [Próximo: mapeamento e tabelas](../02-mapeamento-e-tabelas/README.md)

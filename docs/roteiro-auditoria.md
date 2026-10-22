# Roteiro para discutir auditoria com evidências

Para estudar cada conceito com exemplos do código e exercícios, siga a [trilha Envers por tópicos](envers/README.md). Este roteiro continua como apoio à demonstração e à discussão com a equipe.

## Pergunta inicial

Precisamos recuperar o estado anterior de entidades, identificar quem mudou dados, registrar acesso/leitura ou capturar alterações feitas por qualquer sistema no banco? São requisitos diferentes. Este laboratório começa pelo histórico de entidades alteradas pelo Hibernate.

## Conceitos para demonstrar

| Conceito | Exemplo | Situação nesta base |
|---|---|---|
| Estado atual | O título atual de uma tarefa | Implementado |
| Snapshot histórico | O título armazenado em uma revisão | Implementado |
| Revisão por transação | Duas tarefas mudam e compartilham revisão | Teste automatizado |
| Campos modificados | Apenas `titulo` e `concluida` mudaram | Implementado |
| Metadados | Autor informado e instante da revisão | Implementado; identidade simulada |
| Exclusão | Tarefa removida, histórico preservado | Implementado com conteúdo na exclusão |
| Rollback | A transação é desfeita | Teste automatizado |
| Campo ignorado | Um campo com `@NotAudited` | Próximo experimento |
| Relacionamento | Tarefa pertence a uma lista com histórico | Próximo experimento |
| Estado em uma revisão global | Encontrar snapshot vigente em uma revisão sem alteração da tarefa | Próximo experimento com `AuditReader.find` |
| Estratégias | Comparar consulta e escrita com validade por início/fim | Próximo experimento em banco separado |

O número de revisão é global ao conjunto auditado. Não é uma sequência individual por tarefa, nem precisa ser contínuo. O instante é obtido pela aplicação ao gerar a revisão; não deve ser apresentado como horário exato de commit do PostgreSQL.

## Limites que precisam entrar na discussão

- **SQL externo:** alterações por SQL direto não passam pelos eventos do Hibernate. Há um teste mostrando a divergência entre estado atual e histórico. Operações em massa JPQL/HQL também precisam de avaliação específica, pois não equivalem ao ciclo de vida normal de cada entidade.
- **Autoria:** `X-Autor` é manipulável pelo cliente. Serve para simular cenários; a integração com identidade autenticada deve substituir essa fonte.
- **Leitura e acesso:** consultar dados não cria automaticamente uma trilha de acesso no Envers.
- **Imutabilidade:** guardar histórico não impede um usuário com permissão SQL de alterar ou excluir tabelas de auditoria. Permissões e proteção do histórico são decisões adicionais.
- **Concorrência:** este CRUD ainda não tem controle de edição simultânea. O `@Version` da JPA, quando adicionado, trata concorrência otimista; é diferente da revisão do Envers.
- **Restauração:** visualizar um snapshot não restaura a entidade automaticamente. Uma restauração precisa de operação explícita, regras de negócio e nova revisão.
- **Evolução do esquema:** campos e relacionamentos auditados exigem migrações coerentes nas tabelas atuais e históricas.
- **Crescimento e custo:** auditoria acrescenta gravações, espaço e consultas. Nenhum limite de volume, desempenho ou retenção foi estabelecido por esta base.
- **Dados pessoais:** selecionar campos auditados e definir retenção exige decisões do sistema; histórico preservado inclui valores antigos.

## Pequena demonstração para a equipe

1. Crie, edite e conclua uma tarefa; mostre o estado atual e as revisões.
2. Explique a diferença entre número da revisão, autor e campos modificados.
3. Exclua a tarefa e consulte o histórico pelo ID.
4. Apresente os testes de rollback, transação compartilhada e SQL direto.
5. Relacione os requisitos reais da equipe aos recursos e limites observados.

## Fontes de estudo

- https://hibernate.org/orm/envers/
- https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers

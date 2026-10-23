# 09 — Adoção em produção: transformar recursos em decisões

**Prioridade:** aprofundamento. **Pergunta:** quais evidências faltam para recomendar a solução?

## Use o laboratório como ponto de partida

O ToDo demonstra auditoria de uma entidade simples. Não valida a cobertura de outro sistema, proteção do histórico, concorrência, volume ou retenção. As decisões abaixo são propostas para avaliar adoção; não são funcionalidades prontas nem requisitos universais impostos pelo Envers.

| Decisão | Pergunta para a equipe | Evidência a produzir |
|---|---|---|
| Cobertura | Quem escreve nessas tabelas e por quais caminhos? | Inventário de APIs, jobs, scripts e integrações |
| Identidade | Qual fonte comprova o autor? | Testes de identidade autenticada e execução automática |
| Acesso | Quem pode consultar o histórico? | Regras e testes de autorização |
| Esquema | Como evoluir tabelas atuais e históricas? | Migração ensaiada e validada com dados de teste |
| Histórico anterior | O que existe antes de ativar a auditoria? | Plano de início da cobertura ou carga inicial com origem declarada |
| Operação | Como preservar e recuperar o histórico? | Ensaio de backup/restauração e permissões definidas |
| Custo | Qual o impacto no volume esperado? | Medição de escrita, consulta, armazenamento e banco |
| Retenção | Por quanto tempo cada histórico é necessário? | Política acordada e testes da recuperação histórica |

## Onde a base atual ajuda

O [pom.xml](../../pom.xml) deixa a versão do Envers sob o gerenciamento do Spring Boot. A [migração V1](../../src/main/resources/db/migration/V1__tarefas_e_auditoria.sql) mostra explicitamente as tabelas auditadas e o índice por revisão. O [application.properties](../../src/main/resources/application.properties) usa `ddl-auto=validate`.

Esses elementos tornam a estrutura revisável. Não provam que o índice atual serve para toda consulta, que atualizar dependências preservará todos os comportamentos ou que uma migração funcionará com um histórico grande.

## Experimento de observação implementável sem mudar o código

Depois de gerar algumas revisões na tela, execute no PostgreSQL:

```sql
SELECT 'atuais' AS conjunto, count(*) AS linhas FROM tarefa
UNION ALL
SELECT 'historico', count(*) FROM tarefa_aud
UNION ALL
SELECT 'revisoes', count(*) FROM revisao;

SELECT pg_size_pretty(pg_total_relation_size('tarefa')) AS tamanho_atual,
       pg_size_pretty(pg_total_relation_size('tarefa_aud')) AS tamanho_historico;

SELECT indexname, indexdef FROM pg_indexes
WHERE schemaname = 'public' AND tablename = 'tarefa_aud';
```

**Resultado esperado:** contagens e espaço do seu banco naquele momento. A contagem de tarefas atuais pode cair após exclusões enquanto o histórico permanece. Esses números não são um benchmark nem uma previsão linear de consumo em produção.

## Experimento proposto de custo

Prepare bases descartáveis e comparáveis com e sem auditoria, mantendo dados, máquina, configuração e consultas equivalentes. Meça separadamente as escritas e as consultas de histórico. Registre total de chamadas, concorrência, duração, mediana, p95, erros, condições de rede e métricas do PostgreSQL/aplicação. O objetivo é quantificar o impacto no cenário escolhido; ainda não existe esse teste de carga aqui.

Retenção exige atenção: remover todos os snapshots anteriores a uma data pode apagar o último estado conhecido de uma entidade que não mudou recentemente. Uma proposta de expurgo precisa preservar a possibilidade de responder às consultas exigidas pelo negócio. O guia discute [particionamento de tabelas auditadas](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-partitioning), mas esta base não o implementa.

## Armadilhas para levar à discussão

- O autor da tela é simulado; ainda não há controle de acesso ao histórico.
- A revisão atual usa `Integer`; avalie o volume acumulado e a faixa de numeração antes de adotar esse desenho a longo prazo.
- A consulta com nomes de propriedades modificadas é marcada como experimental no guia 6.6; inclua-a na validação de upgrades.
- Não prometa histórico anterior à ativação. Uma carga inicial proposta precisa distinguir dados conhecidos de valores que já não podem ser recuperados.
- Backups, permissões, retenção e concorrência precisam ser tratados explicitamente pelo sistema.

## Explique com suas palavras

“Qual evidência deste laboratório sustenta a escolha e qual evidência ainda preciso produzir para o sistema real?”

Uma resposta útil cita comportamentos demonstrados, requisitos ainda desconhecidos e o próximo experimento necessário.

## Referência oficial

- [Revision Log: número e metadados](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-revisionlog).
- [Escolha da estratégia](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-audit-strategy).
- [Understanding the Envers Schema](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-schema).
- [Advanced: Audit table partitioning](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers-partitioning).

[Anterior](08-relacionamentos.md) · [Índice](README.md)

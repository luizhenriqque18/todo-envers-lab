package br.com.laboratorio.todo.audit;

import br.com.laboratorio.todo.tarefa.Tarefa;
import br.com.laboratorio.todo.tarefa.TarefaDtos.Historico;
import br.com.laboratorio.todo.tarefa.TarefaDtos.Resposta;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class HistoricoService {
    private final EntityManager entityManager;

    public HistoricoService(EntityManager entityManager) { this.entityManager = entityManager; }

    public List<Historico> listar(long id, int pagina, int tamanho) {
        List<?> linhas = consulta(id).addOrder(AuditEntity.revisionNumber().desc())
                .setFirstResult(Math.multiplyExact(pagina, tamanho)).setMaxResults(tamanho).getResultList();
        return linhas.stream().map(this::converter).toList();
    }

    public Historico buscar(long id, int revisao) {
        List<?> linhas = consulta(id).add(AuditEntity.revisionNumber().eq(revisao)).getResultList();
        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Revisão desta tarefa não encontrada.");
        }
        return converter(linhas.getFirst());
    }

    private AuditQuery consulta(long id) {
        // Inclui revisões de exclusão, mesmo depois de remover a entidade atual.
        return AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntityWithChanges(Tarefa.class, true).add(AuditEntity.id().eq(id));
    }

    private Historico converter(Object linha) {
        Object[] valores = (Object[]) linha;
        Tarefa tarefa = (Tarefa) valores[0];
        Revisao revisao = (Revisao) valores[1];
        RevisionType tipo = (RevisionType) valores[2];
        @SuppressWarnings("unchecked") Set<String> campos = (Set<String>) valores[3];
        String operacao = switch (tipo) {
            case ADD -> "CRIACAO";
            case MOD -> "ALTERACAO";
            case DEL -> "EXCLUSAO";
        };
        return new Historico(revisao.getId(), Instant.ofEpochMilli(revisao.getInstante()),
                revisao.getAutor(), operacao, campos.stream().sorted().toList(), Resposta.de(tarefa));
    }
}

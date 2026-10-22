package br.com.laboratorio.todo.audit;

import org.hibernate.envers.RevisionListener;

public class RevisaoListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        ((Revisao) revisionEntity).setAutor(AutorAuditoria.atual());
    }
}

package br.com.laboratorio.todo.tarefa;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

import java.time.Instant;

@Entity
@Table(name = "tarefa")
@Audited(withModifiedFlag = true)
public class Tarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private boolean concluida;

    @Column(nullable = false, updatable = false)
    private Instant criadaEm;

    protected Tarefa() {}

    public Tarefa(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.criadaEm = Instant.now();
    }

    public void atualizar(String titulo, String descricao, boolean concluida) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.concluida = concluida;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public boolean isConcluida() { return concluida; }
    public Instant getCriadaEm() { return criadaEm; }
}

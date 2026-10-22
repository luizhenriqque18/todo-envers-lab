package br.com.laboratorio.todo.audit;

import jakarta.persistence.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revisao")
@RevisionEntity(RevisaoListener.class)
public class Revisao {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revisao_seq")
    @SequenceGenerator(name = "revisao_seq", sequenceName = "revisao_seq", allocationSize = 1)
    @RevisionNumber
    private Integer id;

    @RevisionTimestamp
    @Column(nullable = false)
    private long instante;

    @Column(nullable = false, length = 100)
    private String autor;

    public Integer getId() { return id; }
    public long getInstante() { return instante; }
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
}

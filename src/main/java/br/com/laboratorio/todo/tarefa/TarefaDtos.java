package br.com.laboratorio.todo.tarefa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class TarefaDtos {
    private TarefaDtos() {}

    public record Criar(@NotBlank @Size(max = 150) String titulo,
                        @Size(max = 2000) String descricao) {}

    public record Atualizar(@NotBlank @Size(max = 150) String titulo,
                            @Size(max = 2000) String descricao,
                            @NotNull Boolean concluida) {}

    public record Resposta(Long id, String titulo, String descricao, boolean concluida, Instant criadaEm) {
        public static Resposta de(Tarefa tarefa) {
            return new Resposta(tarefa.getId(), tarefa.getTitulo(), tarefa.getDescricao(),
                    tarefa.isConcluida(), tarefa.getCriadaEm());
        }
    }

    public record Historico(Integer revisao, Instant instante, String autor, String operacao,
                            List<String> camposModificados, Resposta tarefa) {}
}

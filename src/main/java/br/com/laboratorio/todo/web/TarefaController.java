package br.com.laboratorio.todo.web;

import br.com.laboratorio.todo.audit.HistoricoService;
import br.com.laboratorio.todo.tarefa.TarefaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static br.com.laboratorio.todo.tarefa.TarefaDtos.*;

@RestController
@RequestMapping("/api/tarefas")
@Validated
public class TarefaController {
    private final TarefaService tarefas;
    private final HistoricoService historico;

    public TarefaController(TarefaService tarefas, HistoricoService historico) {
        this.tarefas = tarefas;
        this.historico = historico;
    }

    @GetMapping
    public List<Resposta> listar(@RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return tarefas.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    public Resposta buscar(@PathVariable @Min(1) long id) { return tarefas.buscar(id); }

    @PostMapping
    public ResponseEntity<Resposta> criar(@Valid @RequestBody Criar entrada) {
        Resposta resposta = tarefas.criar(entrada);
        return ResponseEntity.created(URI.create("/api/tarefas/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    public Resposta atualizar(@PathVariable @Min(1) long id, @Valid @RequestBody Atualizar entrada) {
        return tarefas.atualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Min(1) long id) {
        tarefas.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/historico")
    public List<Historico> historico(@PathVariable @Min(1) long id,
                                    @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return historico.listar(id, pagina, tamanho);
    }

    @GetMapping("/{id}/historico/{revisao}")
    public Historico revisao(@PathVariable @Min(1) long id, @PathVariable @Min(1) int revisao) {
        return historico.buscar(id, revisao);
    }
}

package br.com.laboratorio.todo.tarefa;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static br.com.laboratorio.todo.tarefa.TarefaDtos.*;

@Service
@Transactional(readOnly = true)
public class TarefaService {
    private final TarefaRepository repository;

    public TarefaService(TarefaRepository repository) { this.repository = repository; }

    public List<Resposta> listar(int pagina, int tamanho) {
        return repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("id").descending()))
                .map(Resposta::de).getContent();
    }

    public Resposta buscar(long id) { return Resposta.de(encontrar(id)); }

    @Transactional
    public Resposta criar(Criar entrada) {
        return Resposta.de(repository.save(new Tarefa(entrada.titulo().strip(), entrada.descricao())));
    }

    @Transactional
    public Resposta atualizar(long id, Atualizar entrada) {
        Tarefa tarefa = encontrar(id);
        tarefa.atualizar(entrada.titulo().strip(), entrada.descricao(), entrada.concluida());
        return Resposta.de(tarefa);
    }

    @Transactional
    public void excluir(long id) { repository.delete(encontrar(id)); }

    private Tarefa encontrar(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
    }
}

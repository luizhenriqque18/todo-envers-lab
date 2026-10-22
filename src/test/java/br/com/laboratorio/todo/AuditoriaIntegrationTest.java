package br.com.laboratorio.todo;

import br.com.laboratorio.todo.audit.AutorAuditoria;
import br.com.laboratorio.todo.audit.HistoricoService;
import br.com.laboratorio.todo.tarefa.Tarefa;
import br.com.laboratorio.todo.tarefa.TarefaDtos;
import br.com.laboratorio.todo.tarefa.TarefaRepository;
import br.com.laboratorio.todo.tarefa.TarefaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuditoriaIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TarefaService tarefas;
    @Autowired HistoricoService historico;
    @Autowired TarefaRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void limparContexto() { AutorAuditoria.limpar(); }

    @Test
    void crudHttpRegistraAutorCamposEExclusaoComSnapshot() throws Exception {
        String criacao = mvc.perform(post("/api/tarefas").header("X-Autor", "Ana")
                        .contentType("application/json").content("""
                        {"titulo":"Estudar Envers","descricao":"Primeira versão"}
                        """))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(criacao).get("id").asLong();

        mvc.perform(put("/api/tarefas/{id}", id).header("X-Autor", "Bruno")
                        .contentType("application/json").content("""
                        {"titulo":"Estudar auditoria","descricao":"Primeira versão","concluida":true}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.concluida").value(true));
        mvc.perform(get("/api/tarefas/{id}/historico", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].autor").value("Bruno"))
                .andExpect(jsonPath("$[0].camposModificados").value(org.hamcrest.Matchers.containsInAnyOrder("titulo", "concluida")))
                .andExpect(jsonPath("$[1].autor").value("Ana"));

        int revisaoCriacao = historico.listar(id, 0, 20).getLast().revisao();
        mvc.perform(get("/api/tarefas/{id}/historico/{revisao}", id, revisaoCriacao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tarefa.titulo").value("Estudar Envers"));
        mvc.perform(delete("/api/tarefas/{id}", id).header("X-Autor", "Carla"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tarefas/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/tarefas/{id}/historico", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].operacao").value("EXCLUSAO"))
                .andExpect(jsonPath("$[0].autor").value("Carla"))
                .andExpect(jsonPath("$[0].tarefa.titulo").value("Estudar auditoria"));
        assertThat(AutorAuditoria.atual()).isEqualTo("sistema");
    }

    @Test
    void rollbackNaoPersisteTarefaNemHistorico() {
        long[] id = new long[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Tarefa tarefa = repository.saveAndFlush(new Tarefa("Desfazer", null));
            id[0] = tarefa.getId();
            status.setRollbackOnly();
        });
        assertThat(repository.findById(id[0])).isEmpty();
        assertThat(historico.listar(id[0], 0, 20)).isEmpty();
    }

    @Test
    void duasTarefasAlteradasNaMesmaTransacaoCompartilhamRevisao() {
        long primeiro = criar("Primeira");
        long segundo = criar("Segunda");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tarefas.atualizar(primeiro, new TarefaDtos.Atualizar("Primeira", null, true));
            tarefas.atualizar(segundo, new TarefaDtos.Atualizar("Segunda", null, true));
        });
        assertThat(historico.listar(primeiro, 0, 20).getFirst().revisao())
                .isEqualTo(historico.listar(segundo, 0, 20).getFirst().revisao());
    }

    @Test
    void atualizacaoSemMudancaNaoCriaNovaRevisao() {
        long id = criar("Sem mudança");
        tarefas.atualizar(id, new TarefaDtos.Atualizar("Sem mudança", null, false));
        assertThat(historico.listar(id, 0, 20)).hasSize(1);
    }

    @Test
    void sqlDiretoAlteraEstadoAtualSemGerarAuditoria() {
        long id = criar("Original");
        jdbc.update("UPDATE tarefa SET titulo = ? WHERE id = ?", "SQL direto", id);
        assertThat(tarefas.buscar(id).titulo()).isEqualTo("SQL direto");
        var revisoes = historico.listar(id, 0, 20);
        assertThat(revisoes).hasSize(1);
        assertThat(revisoes.getFirst().tarefa().titulo()).isEqualTo("Original");
    }

    @Test
    void entradasInvalidasERecursosAusentesRetornamErrosHttp() throws Exception {
        mvc.perform(post("/api/tarefas").contentType("application/json").content("{\"titulo\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tarefas?tamanho=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tarefas?pagina=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tarefas/-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tarefas/9223372036854775807")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tarefas/9223372036854775807/historico/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/tarefas").header("X-Autor", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revisaoExataNaoRetornaEstadoDeOutraRevisao() throws Exception {
        long primeiro = criar("Uma tarefa");
        long segundo = criar("Outra tarefa");
        int revisaoOutra = historico.listar(segundo, 0, 20).getFirst().revisao();
        mvc.perform(get("/api/tarefas/{id}/historico/{revisao}", primeiro, revisaoOutra))
                .andExpect(status().isNotFound());
    }

    @Test
    void telaDoLaboratorioEstaDisponivel() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Laboratório Envers")));
    }

    private long criar(String titulo) {
        return tarefas.criar(new TarefaDtos.Criar(titulo, null)).id();
    }
}

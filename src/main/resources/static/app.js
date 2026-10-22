const $ = (id) => document.getElementById(id);
let editando = null;
let pagina = 0;
let idHistorico = null;
let paginaHistorico = 0;
const tamanho = 10;

function mensagem(texto, erro = false) {
  $('mensagem').textContent = texto;
  $('mensagem').className = erro ? 'erro' : '';
}

async function api(caminho, opcoes = {}) {
  const resposta = await fetch('/api/tarefas' + caminho, {
    ...opcoes,
    headers: { 'Content-Type': 'application/json', 'X-Autor': $('autor').value.trim(), ...opcoes.headers }
  });
  if (!resposta.ok) {
    const erro = await resposta.json().catch(() => ({}));
    throw new Error(erro.detail || erro.message || `Não foi possível concluir (${resposta.status}).`);
  }
  return resposta.status === 204 ? null : resposta.json();
}

function elemento(tag, texto, classe) {
  const no = document.createElement(tag);
  if (texto !== undefined) no.textContent = texto;
  if (classe) no.className = classe;
  return no;
}

function botao(texto, acao, classe = 'secundario') {
  const no = elemento('button', texto, classe);
  no.type = 'button';
  no.addEventListener('click', async () => {
    no.disabled = true;
    try { await acao(); } catch (erro) { mensagem(erro.message, true); }
    finally { no.disabled = false; }
  });
  return no;
}

async function carregar() {
  const tarefas = await api(`?pagina=${pagina}&tamanho=${tamanho}`);
  $('tarefas').replaceChildren();
  $('pagina').textContent = `Página ${pagina + 1}`;
  $('anterior').disabled = pagina === 0;
  $('proxima').disabled = tarefas.length < tamanho;
  if (!tarefas.length) $('tarefas').append(elemento('p', 'Nenhuma tarefa nesta página.'));
  for (const tarefa of tarefas) {
    const card = elemento('article', undefined, 'tarefa');
    card.append(elemento('h3', tarefa.titulo));
    card.append(elemento('span', `#${tarefa.id} · ${tarefa.concluida ? 'Concluída' : 'Pendente'}`, 'etiqueta'));
    if (tarefa.descricao) card.append(elemento('p', tarefa.descricao));
    const acoes = elemento('div', undefined, 'acoes');
    acoes.append(botao('Editar', () => editar(tarefa)));
    acoes.append(botao(tarefa.concluida ? 'Reabrir' : 'Concluir', async () => {
      await api(`/${tarefa.id}`, { method: 'PUT', body: JSON.stringify({ ...tarefa, concluida: !tarefa.concluida }) });
      if (editando === tarefa.id) limpar();
      await carregar();
      await mostrarHistorico(tarefa.id);
      mensagem('Alteração salva. Consulte a nova revisão abaixo.');
    }));
    acoes.append(botao('Histórico', () => mostrarHistorico(tarefa.id)));
    acoes.append(botao('Excluir', async () => {
      if (!confirm(`Excluir a tarefa #${tarefa.id}? O histórico será preservado.`)) return;
      await api(`/${tarefa.id}`, { method: 'DELETE' });
      if (editando === tarefa.id) limpar();
      await carregar();
      await mostrarHistorico(tarefa.id);
      mensagem('Tarefa excluída. A revisão de exclusão está no histórico.');
    }, 'perigo'));
    card.append(acoes);
    $('tarefas').append(card);
  }
}

function editar(tarefa) {
  editando = tarefa.id;
  $('titulo').value = tarefa.titulo;
  $('descricao').value = tarefa.descricao || '';
  $('concluida').checked = tarefa.concluida;
  $('concluida').disabled = false;
  $('editor-titulo').textContent = `Editar tarefa #${tarefa.id}`;
  $('salvar').textContent = 'Salvar alterações';
  $('cancelar').hidden = false;
  $('titulo').focus();
}

function limpar() {
  editando = null;
  $('titulo').value = '';
  $('descricao').value = '';
  $('concluida').checked = false;
  $('concluida').disabled = true;
  $('editor-titulo').textContent = 'Nova tarefa';
  $('salvar').textContent = 'Criar tarefa';
  $('cancelar').hidden = true;
}

async function mostrarHistorico(id, novaPagina = 0) {
  const revisoes = await api(`/${id}/historico?pagina=${novaPagina}&tamanho=${tamanho}`);
  idHistorico = id;
  paginaHistorico = novaPagina;
  $('id-historico').value = id;
  $('historico-titulo').textContent = `Histórico da tarefa #${id}`;
  $('revisoes').replaceChildren();
  $('paginacao-historico').hidden = false;
  $('historico-pagina').textContent = `Página ${novaPagina + 1}`;
  $('historico-anterior').disabled = novaPagina === 0;
  $('historico-proxima').disabled = revisoes.length < tamanho;
  if (!revisoes.length) $('revisoes').append(elemento('p', 'Nenhuma revisão nesta página.'));
  for (const revisao of revisoes) {
    const card = elemento('article', undefined, 'revisao');
    card.append(elemento('h3', `Revisão ${revisao.revisao} · ${revisao.operacao}`));
    card.append(elemento('p', `${new Date(revisao.instante).toLocaleString('pt-BR')} · Autor: ${revisao.autor}`));
    if (revisao.operacao === 'ALTERACAO') {
      card.append(elemento('p', `Campos modificados: ${revisao.camposModificados.join(', ')}`));
    }
    card.append(elemento('pre', JSON.stringify(revisao.tarefa, null, 2)));
    $('revisoes').append(card);
  }
}

$('formulario').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  $('salvar').disabled = true;
  try {
    const dados = { titulo: $('titulo').value, descricao: $('descricao').value || null };
    const eraEdicao = editando !== null;
    if (eraEdicao) dados.concluida = $('concluida').checked;
    const tarefa = await api(eraEdicao ? `/${editando}` : '', {
      method: eraEdicao ? 'PUT' : 'POST', body: JSON.stringify(dados)
    });
    limpar();
    pagina = 0;
    await carregar();
    await mostrarHistorico(tarefa.id);
    mensagem('Tarefa salva. Um envio sem mudanças não cria nova revisão.');
  } catch (erro) { mensagem(erro.message, true); }
  finally { $('salvar').disabled = false; }
});
$('cancelar').addEventListener('click', limpar);
$('atualizar').addEventListener('click', () => carregar().catch(e => mensagem(e.message, true)));
$('anterior').addEventListener('click', () => { pagina--; carregar().catch(e => mensagem(e.message, true)); });
$('proxima').addEventListener('click', () => { pagina++; carregar().catch(e => mensagem(e.message, true)); });
$('buscar-historico').addEventListener('submit', evento => {
  evento.preventDefault();
  mostrarHistorico($('id-historico').value).catch(e => mensagem(e.message, true));
});
$('historico-anterior').addEventListener('click', () => mostrarHistorico(idHistorico, paginaHistorico - 1).catch(e => mensagem(e.message, true)));
$('historico-proxima').addEventListener('click', () => mostrarHistorico(idHistorico, paginaHistorico + 1).catch(e => mensagem(e.message, true)));
limpar();
carregar().catch(e => mensagem(e.message, true));

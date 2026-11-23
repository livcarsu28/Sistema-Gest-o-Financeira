/* Estado compartilhado das telas financeiras, sincronizado com o servidor. */
(function () {
    'use strict';
    const CONFIG = { parcelamento: true };
    const CATEGORIAS = [], FORMAS = [];
    const SEM_CATEGORIA = { id: null, nome: 'Sem categoria', cor: '#6B6670' };
    const CORES = ['#1B7F4B', '#1D6FA5', '#9A6200', '#6B4FB5', '#0B7E80', '#B4501B', '#B03363'];
    const api = document.querySelector('meta[name="financeiro-api"]').content;
    const csrf = document.querySelector('meta[name="csrf-token"]').content;
    const csrfHeader = document.querySelector('meta[name="csrf-header"]').content;
    let db = { movs: [], notas: [] };
    let ultimaConsulta = 0;

    /* ---------- datas (sempre texto AAAA-MM-DD, sem fuso) ---------- */
    const pad = n => String(n).padStart(2, '0');
    const iso = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const parse = s => { const [a, m, d] = s.split('-').map(Number); return new Date(a, m - 1, d); };
    const hoje = () => db.hoje || iso(new Date());
    function somarMeses(s, n, dia) {
        const d = parse(s), alvo = new Date(d.getFullYear(), d.getMonth() + n, 1);
        const ultimo = new Date(alvo.getFullYear(), alvo.getMonth() + 1, 0).getDate();
        alvo.setDate(Math.min(dia || d.getDate(), ultimo));
        return iso(alvo);
    }
    function somarDias(s, n) { const d = parse(s); d.setDate(d.getDate() + n); return iso(d); }
    const norm = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const maiuscula = s => s.charAt(0).toUpperCase() + s.slice(1);

    /* ---------- formatação ---------- */
    const moeda = v => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    const dataBR = s => parse(s).toLocaleDateString('pt-BR');
    const dataExtenso = s => maiuscula(parse(s).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }));
    const mesRotulo = ym => maiuscula(parse(ym + '-01').toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }));
    function rotuloDia(s) {
        const h = hoje();
        if (s === h) return 'Hoje';
        if (s === somarDias(h, -1)) return 'Ontem';
        if (s === somarDias(h, 1)) return 'Amanhã';
        return maiuscula(parse(s).toLocaleDateString('pt-BR', { weekday: 'short', day: 'numeric', month: 'short' }).replace('.', ''));
    }
    const categoria = id => CATEGORIAS.find(c => c.id === id) || SEM_CATEGORIA;

    async function requisitar(caminho = '', metodo = 'GET', corpo) {
        const headers = { Accept: 'application/json' };
        if (metodo !== 'GET') headers[csrfHeader] = csrf;
        if (corpo !== undefined) headers['Content-Type'] = 'application/json';
        const resposta = await fetch(api + caminho, { method: metodo, headers, body: JSON.stringify(corpo) });
        if (resposta.redirected || resposta.status === 401 || resposta.status === 403) {
            throw new Error('Recarregue a página e entre novamente.');
        }
        if (!resposta.ok) {
            const erro = await resposta.json().catch(() => ({}));
            throw new Error(erro.mensagem || 'O servidor não concluiu a operação.');
        }
        if (resposta.status !== 204) return resposta.json();
    }

    async function carregar() {
        const consulta = ++ultimaConsulta;
        const estado = await requisitar();
        if (consulta !== ultimaConsulta) return;
        const formas = new Map(estado.formas.map(f => [f.id, f.nome]));
        estado.movs.forEach(m => { m.forma = formas.get(m.formaPagamentoId) || ''; });
        CATEGORIAS.splice(0, CATEGORIAS.length, ...estado.categorias.map(c => ({
            ...c, cor: /^#[0-9a-f]{6}$/i.test(c.cor) ? c.cor : CORES[c.id % CORES.length]
        })).sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')));
        FORMAS.splice(0, FORMAS.length, ...estado.formas.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')));
        db = estado;
        window.dispatchEvent(new CustomEvent('financeiro:mudou'));
    }

    async function gravar(caminho, metodo, corpo) {
        await requisitar(caminho, metodo, corpo);
        try {
            await carregar();
        } catch {
            window.dispatchEvent(new CustomEvent('financeiro:sincronizacao'));
        }
    }

    const mov = '/movimentacoes', nota = '/anotacoes';
    const escopo = valor => '?proximas=' + (valor === 'proximas');
    const obter = id => db.movs.find(m => m.id === id);
    const criar = (d, notaId) => gravar(mov + (notaId == null ? '' : '?anotacaoId=' + notaId), 'POST', d);
    const atualizar = (id, d, s) => gravar(mov + '/' + id + escopo(s), 'PUT', d);
    const remover = (id, s) => gravar(mov + '/' + id + escopo(s), 'DELETE');
    const definirPagamento = (id, pago) => gravar(mov + '/' + id + '/pagamento?pago=' + pago, 'PATCH');

    function situacao(m) {
        const e = m.tipo === 'entrada';
        if (m.status === 'pago') return { chave: 'ok', texto: e ? 'Recebido' : 'Pago' };
        if (m.data < hoje()) return { chave: 'atraso', texto: 'Em atraso' };
        return { chave: 'previsto', texto: e ? 'A receber' : 'A pagar' };
    }

    function filtrar(f = {}) {
        const b = norm(f.busca || '');
        return db.movs.filter(m =>
            (!f.mes || m.data.slice(0, 7) === f.mes) &&
            (!f.tipo || m.tipo === f.tipo) &&
            (!f.status || (f.status === 'pendente' ? m.status === 'pendente' : m.status === 'pago')) &&
            (!f.categoriaId || m.categoriaId === f.categoriaId) &&
            (!f.repeticao || (f.repeticao === 'mensal' ? m.recorrente : m.parcelas > 1)) &&
            (!b || norm(m.descricao + ' ' + categoria(m.categoriaId).nome).includes(b))
        ).sort((a, c) => c.data.localeCompare(a.data) || c.id - a.id);
    }
    const recentes = (limite, f) => filtrar(f).filter(m => m.data <= hoje()).slice(0, limite);

    function resumo(ym, aceita = () => true) { // ym vazio = todo o período
        const r = { entradas: 0, saidas: 0, aReceber: 0, aPagar: 0 };
        db.movs.forEach(m => {
            if (!aceita(m) || (ym && m.data.slice(0, 7) !== ym)) return;
            const e = m.tipo === 'entrada';
            r[m.status === 'pago' ? (e ? 'entradas' : 'saidas') : (e ? 'aReceber' : 'aPagar')] += Math.round(m.valor * 100);
        });
        r.saldo = (r.entradas - r.saidas) / 100;
        for (const chave of ['entradas', 'saidas', 'aReceber', 'aPagar']) r[chave] /= 100;
        return r;
    }
    const pendentes = () => db.movs.filter(m => m.status === 'pendente');
    const atrasadas = () => pendentes().filter(m => m.data < hoje()).sort((a, b) => a.data.localeCompare(b.data));
    const proximas = dias => { const h = hoje(), f = somarDias(h, dias); return pendentes().filter(m => m.data >= h && m.data <= f).sort((a, b) => a.data.localeCompare(b.data)); };

    const notas = (f = {}) => db.notas.filter(n => !f.mes || n.data.slice(0, 7) === f.mes).sort((a, b) => a.id - b.id);
    const criarNota = d => gravar(nota, 'POST', d);
    const concluirNota = (id, concluida) => gravar(nota + '/' + id + '?concluida=' + concluida, 'PATCH');
    const removerNota = id => gravar(nota + '/' + id, 'DELETE');

    window.Store = {
        CONFIG, CATEGORIAS, FORMAS, categoria, carregar,
        hoje, somarMeses, somarDias, moeda, dataBR, dataExtenso, mesRotulo, rotuloDia, norm,
        obter, criar, atualizar, remover, definirPagamento, situacao,
        filtrar, recentes, resumo, atrasadas, proximas,
        notas, criarNota, concluirNota, removerNota
    };
})();

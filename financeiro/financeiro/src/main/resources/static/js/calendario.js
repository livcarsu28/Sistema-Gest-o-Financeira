(function () {
    'use strict';
    const S = window.Store, U = window.UI, h = U.h, $ = id => document.getElementById(id);
    let ym = S.hoje().slice(0, 7), sel = S.hoje();
    const cats = new Set(); // categorias filtradas (vazio = todas)

    const passa = x => !cats.size || cats.has(x.categoriaId);
    const pad = n => String(n).padStart(2, '0');

    function celulasDoMes() {
        const [y, m] = ym.split('-').map(Number), dias = new Date(y, m, 0).getDate(), c = [];
        for (let i = 0; i < new Date(y, m - 1, 1).getDay(); i++) c.push(null);
        for (let d = 1; d <= dias; d++) c.push(`${ym}-${pad(d)}`);
        while (c.length % 7) c.push(null);
        return c;
    }
    const doDia = d => ({ movs: S.filtrar({ mes: d.slice(0, 7) }).filter(m => m.data === d && passa(m)), notas: S.notas({ mes: d.slice(0, 7) }).filter(n => n.data === d && passa(n)) });

    function renderGrade() {
        const movs = S.filtrar({ mes: ym }).filter(passa), notas = S.notas({ mes: ym }).filter(passa), hoje = S.hoje();
        const por = {};
        const slot = d => por[d] || (por[d] = { movs: [], notas: [] });
        movs.forEach(m => slot(m.data).movs.push(m)); notas.forEach(n => slot(n.data).notas.push(n));
        $('grade').replaceChildren(...celulasDoMes().map(d => {
            if (!d) return h('span', { class: 'dia-cel vazio-cel', 'aria-hidden': 'true' });
            const e = por[d] || { movs: [], notas: [] }, total = e.movs.length + e.notas.length;
            const atraso = e.movs.some(m => S.situacao(m).chave === 'atraso');
            const pontos = [...e.movs.map(m => h('i', { class: 'ponto ' + (m.status === 'pago' ? '' : 'pend'), style: '--c:' + S.categoria(m.categoriaId).cor })),
                ...e.notas.map(n => h('i', { class: 'ponto nota', style: '--c:' + S.categoria(n.categoriaId).cor }))];
            const etiquetas = [...e.movs.map(m => h('span', { class: 'etq ' + (m.status === 'pago' ? 'feito' : 'pend'), style: '--c:' + S.categoria(m.categoriaId).cor }, m.descricao)),
                ...e.notas.map(n => h('span', { class: 'etq' + (n.concluida ? ' feito' : ''), style: '--c:' + S.categoria(n.categoriaId).cor }, n.descricao))];
            const dia = Number(d.slice(8));
            const rotulo = `${dia} de ${S.mesRotulo(ym).split(' ')[0].toLowerCase()}${d === hoje ? ', hoje' : ''}: ${total ? total + (total === 1 ? ' item' : ' itens') : 'sem itens'}${atraso ? ', com conta em atraso' : ''}`;
            return h('button', { type: 'button', class: 'dia-cel' + (d === hoje ? ' hoje' : '') + (atraso ? ' atraso' : ''), 'data-data': d, 'aria-pressed': String(d === sel), 'aria-label': rotulo, 'aria-current': d === hoje ? 'date' : null,
                    onclick: () => { sel = d; renderGrade(); renderDia(); } },
                h('span', { class: 'dia-num' }, dia),
                e.movs.some(m => m.recorrente) ? h('span', { class: 'dia-rep', 'aria-hidden': 'true' }, U.ic('repetir')) : null,
                h('span', { class: 'pontos' }, pontos.slice(0, 4), pontos.length > 4 ? h('b', { class: 'mais' }, '+' + (pontos.length - 4)) : null),
                h('span', { class: 'etiquetas' }, etiquetas.slice(0, 3), etiquetas.length > 3 ? h('span', { class: 'etq-mais' }, `+${etiquetas.length - 3} mais`) : null));
        }));
    }

    function renderDia() {
        const { movs, notas } = doDia(sel);
        $('diaTitulo').textContent = S.dataExtenso(sel);
        const n = movs.length + notas.length;
        $('diaSub').textContent = (sel === S.hoje() ? 'Hoje · ' : '') + (n ? `${n} ${n === 1 ? 'item' : 'itens'}` : 'Sem itens');
        $('itensDia').replaceChildren(
            ...movs.map(m => U.linha(m, { sub: 'forma', verificar: true })),
            ...notas.map(nota => {
                const cat = S.categoria(nota.categoriaId);
                return h('li', { class: 'nota-item' + (nota.concluida ? ' feita' : ''), style: '--c:' + cat.cor },
                    h('label', { class: 'nota-marca' },
                        h('input', { type: 'checkbox', checked: nota.concluida, 'aria-label': 'Concluir: ' + nota.descricao, onchange: async ev => {
                            const input = ev.currentTarget;
                            if (!await U.executar(input, () => S.concluirNota(nota.id, input.checked))) input.checked = nota.concluida;
                        } }),
                        h('span', null, U.ic('check'))),
                    h('span', { class: 'nota-texto' }, h('span', { class: 't' }, nota.descricao), h('span', { class: 'c', style: 'color:' + cat.cor }, 'Anotação · ' + cat.nome)),
                    h('button', { class: 'icone-btn', type: 'button', title: 'Lançar como conta', 'aria-label': 'Lançar como conta: ' + nota.descricao,
                        onclick: () => U.formulario({ preset: { descricao: nota.descricao, data: nota.data, categoriaId: nota.categoriaId }, notaId: nota.id }) }, U.ic('cifrao')),
                    h('button', { class: 'icone-btn', type: 'button', title: 'Excluir anotação', 'aria-label': 'Excluir anotação: ' + nota.descricao,
                        onclick: ev => U.executar(ev.currentTarget, () => S.removerNota(nota.id), 'Anotação excluída') }, U.ic('lixeira')));
            }));
        $('vazioDia').hidden = n > 0;
    }

    function renderTopo() {
        $('mesNome').textContent = S.mesRotulo(ym);
        const r = S.resumo(ym, passa), atrasadas = S.atrasadas().filter(m => m.data.startsWith(ym) && passa(m));
        const tot = atrasadas.reduce((s, m) => s + m.valor, 0);
        $('stats').replaceChildren(
            h('div', { class: 's-pagar' }, h('dt', null, 'A pagar'), h('dd', null, S.moeda(r.aPagar))),
            h('div', { class: 's-receber' }, h('dt', null, 'A receber'), h('dd', null, S.moeda(r.aReceber))),
            h('div', { class: 's-atraso' + (atrasadas.length ? ' tem' : '') }, h('dt', null, 'Em atraso'), h('dd', null, atrasadas.length ? `${atrasadas.length} · ${S.moeda(tot)}` : 'Nenhuma')));
        $('filtroCats').replaceChildren(...S.CATEGORIAS.map(c => h('button', { class: 'chip', type: 'button', 'aria-pressed': String(cats.has(c.id)), style: '--c:' + c.cor,
            onclick: () => { cats.has(c.id) ? cats.delete(c.id) : cats.add(c.id); render(); } }, h('i'), c.nome)));
    }

    function irParaMes(delta) {
        ym = S.somarMeses(ym + '-01', delta).slice(0, 7);
        sel = ym === S.hoje().slice(0, 7) ? S.hoje() : ym + '-01';
        render();
    }
    function render() { renderTopo(); renderGrade(); renderDia(); U.ligarSino(); }

    $('mesAnt').addEventListener('click', () => irParaMes(-1));
    $('mesProx').addEventListener('click', () => irParaMes(1));
    $('irHoje').addEventListener('click', () => { ym = S.hoje().slice(0, 7); sel = S.hoje(); render(); });
    $('lancarConta').addEventListener('click', () => U.formulario({ preset: { data: sel } }));
    $('btnAnotar').addEventListener('click', () => { $('rapido').scrollIntoView({ behavior: 'smooth', block: 'center' }); $('notaTexto').focus({ preventScroll: true }); });
    function atualizarCategoriasNota() {
        const selecionada = $('notaCat').value;
        $('notaCat').replaceChildren(h('option', { value: '' }, 'Sem categoria'),
            ...S.CATEGORIAS.filter(c => c.ativo).map(c => h('option', { value: c.id, selected: String(c.id) === selecionada }, c.nome)));
    }
    window.addEventListener('financeiro:mudou', atualizarCategoriasNota);
    $('rapido').addEventListener('submit', async e => {
        e.preventDefault();
        const t = $('notaTexto').value.trim(); if (!t) { $('notaTexto').focus(); return; }
        const botao = $('rapido').querySelector('button[type="submit"]');
        if (botao.disabled) return;
        const ok = await U.executar(botao, () => S.criarNota({ descricao: t, data: sel, categoriaId: Number($('notaCat').value) || null }));
        if (ok) { $('notaTexto').value = ''; $('notaTexto').focus(); }
    });
    U.iniciar(render);
})();

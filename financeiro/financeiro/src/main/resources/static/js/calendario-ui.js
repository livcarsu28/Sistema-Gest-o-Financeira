/* Componentes compartilhados: ícones, linha de movimentação, folhas (detalhe,
   formulário, filtros, avisos). Todo texto de usuário entra via textContent. */
(function () {
    'use strict';
    const S = window.Store;

    const P = {
        cima: 'M12 19V5m-6 6 6-6 6 6', baixo: 'M12 5v14m6-6-6 6-6-6',
        sino: 'M6 9a6 6 0 1 1 12 0c0 6 2 7.5 2 7.5H4S6 15 6 9zm4 10a2 2 0 0 0 4 0',
        busca: 'M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14zm9 3-4.3-4.3', filtro: 'M4 6h16M7 12h10M10 18h4',
        mais: 'M12 5v14M5 12h14', fechar: 'M6 6l12 12M18 6 6 18', voltar: 'M15 5l-7 7 7 7',
        esquerda: 'M15 5l-7 7 7 7', direita: 'M9 5l7 7-7 7', chevron: 'm6 9 6 6 6-6',
        repetir: 'M17 2l4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 0 1-3 3H3',
        check: 'M5 12.5l4.5 4.5L19 7.5', lixeira: 'M4 7h16M10 11v6m4-6v6M6 7l1 13h10l1-13M9 7V4h6v3',
        lapis: 'M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4', relatorio: 'M6 3h9l4 4v14H6zM14 3v5h5M10 17v-3m4 3v-6',
        cifrao: 'M12 2v20M17 6H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6'
    };
    const ic = (n, cls) => {
        const d = document.createElement('span');
        d.innerHTML = `<svg class="icone ${cls || ''}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="${P[n]}"/></svg>`;
        return d.firstChild;
    };
    function h(tag, props, ...kids) {
        const e = document.createElement(tag);
        for (const [k, v] of Object.entries(props || {})) {
            if (v == null || v === false) continue;
            if (k === 'class') e.className = v;
            else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
            else e.setAttribute(k, v === true ? '' : v);
        }
        kids.flat(Infinity).forEach(c => { if (c != null && c !== false) e.append(c.nodeType ? c : document.createTextNode(c)); });
        return e;
    }

    /* ---------- folha (dialog nativo: foco preso, ESC fecha) ---------- */
    let dlg;
    function folha(titulo, corpo, aoFechar) {
        if (!dlg) {
            dlg = h('dialog', { class: 'folha', 'aria-labelledby': 'folha-titulo' });
            document.body.append(dlg);
            dlg.addEventListener('click', e => { if (e.target === dlg) dlg.close(); });
            dlg.addEventListener('close', () => { const f = dlg._fim; dlg._fim = null; dlg.replaceChildren(); if (f) f(); });
        }
        dlg._fim = aoFechar;
        dlg.replaceChildren(
            h('header', { class: 'folha-topo' }, h('h2', { id: 'folha-titulo' }, titulo),
                h('button', { class: 'icone-btn', type: 'button', 'aria-label': 'Fechar', onclick: () => dlg.close() }, ic('fechar'))),
            h('div', { class: 'folha-corpo' }, corpo));
        if (!dlg.open) dlg.showModal();
    }
    const fechar = () => { if (dlg && dlg.open) dlg.close(); };
    function aviso(msg) {
        let t = document.getElementById('aviso');
        if (!t) { t = h('div', { id: 'aviso', class: 'aviso', role: 'status', 'aria-live': 'polite' }); document.body.append(t); }
        t.textContent = msg; t.classList.add('visivel');
        clearTimeout(t._t); t._t = setTimeout(() => t.classList.remove('visivel'), 2800);
    }

    async function executar(controle, acao, mensagem) {
        controle.disabled = true;
        try {
            await acao();
            if (mensagem) aviso(mensagem);
        } catch (erro) {
            aviso(erro.message);
            return false;
        } finally {
            controle.disabled = false;
        }
        return true;
    }

    function iniciar(render) {
        const estado = h('p', { class: 'subtitulo estado-dados', role: 'status' });
        const tentar = h('button', { class: 'btn btn-pequeno', type: 'button', hidden: true });
        tentar.textContent = 'Tentar novamente';
        const bloco = h('div', { class: 'dados-feedback' }, estado, tentar);
        document.querySelector('main header').after(bloco);
        const load = async () => {
            bloco.hidden = false;
            estado.textContent = 'Carregando dados…';
            tentar.hidden = true;
            try {
                await S.carregar();
                bloco.hidden = true;
            } catch (erro) {
                estado.textContent = 'Não foi possível carregar os dados. ' + erro.message;
                tentar.hidden = false;
            }
        };
        tentar.addEventListener('click', load);
        window.addEventListener('financeiro:mudou', render);
        window.addEventListener('financeiro:sincronizacao', () => {
            bloco.hidden = false;
            estado.textContent = 'Operação salva. Atualize a lista para ver a mudança.';
            tentar.hidden = false;
        });
        load();
    }

    /* ---------- linha de movimentação ---------- */
    // opts.sub: 'data' | 'forma'; opts.verificar: mostra o botão de pagar/receber (calendário)
    function linha(m, opts = {}) {
        const cat = S.categoria(m.categoriaId), sit = S.situacao(m), e = m.tipo === 'entrada';
        const marcas = [];
        if (m.recorrente) marcas.push(h('span', { class: 'marca-repete', title: 'Recorrente' }, ic('repetir'), h('span', { class: 'sr-only' }, 'Recorrente')));
        if (m.parcelas > 1) marcas.push(h('span', { class: 'marca-parcela', title: `Parcela ${m.parcelaAtual} de ${m.parcelas}` }, `${m.parcelaAtual}/${m.parcelas}`));
        const sub = opts.sub === 'forma' ? m.forma : S.dataBR(m.data);
        const btn = h('button', { class: 'mov-btn', type: 'button', onclick: () => detalhe(m.id) },
            h('span', { class: 'mov-icone ' + m.tipo }, ic(e ? 'cima' : 'baixo')),
            h('span', { class: 'mov-texto' },
                h('span', { class: 'mov-titulo' }, h('span', { class: 'mov-nome' }, m.descricao), marcas),
                h('span', { class: 'mov-sub' }, h('span', { class: 'mov-cat', style: 'color:' + cat.cor }, cat.nome), sub ? ' · ' + sub : '')),
            h('span', { class: 'mov-valor' },
                h('span', { class: 'valor ' + m.tipo }, h('span', { class: 'sr-only' }, e ? 'Entrada ' : 'Saída '), (e ? '+ ' : '- ') + S.moeda(m.valor)),
                sit.chave === 'ok' ? null : h('span', { class: 'mov-situacao ' + sit.chave }, sit.texto)));
        const li = h('li', { class: 'mov' });
        if (opts.verificar) {
            const pago = m.status === 'pago';
            li.append(h('button', {
                class: 'mov-check', type: 'button', 'aria-pressed': String(pago),
                'aria-label': pago ? `Desfazer: ${m.descricao}` : `Marcar como ${e ? 'recebido' : 'pago'}: ${m.descricao}`,
                onclick: ev => executar(ev.currentTarget, () => S.definirPagamento(m.id, !pago), pago ? 'Voltou para pendente' : (e ? 'Recebimento registrado' : 'Pagamento registrado'))
            }, ic('check')));
        }
        li.append(btn);
        return li;
    }

    /* ---------- campos ---------- */
    function seg(nome, opcoes, valor, aoMudar, extra) {
        const g = h('div', { class: 'seg ' + (extra || ''), role: 'radiogroup' });
        opcoes.forEach(([v, r]) => {
            const id = `${nome}-${v}`, inp = h('input', { type: 'radio', name: nome, id, value: v, checked: v === valor });
            inp.addEventListener('change', () => aoMudar && aoMudar(v));
            g.append(inp, h('label', { for: id }, r));
        });
        return g;
    }
    function lerValor(s) {
        s = String(s).trim().replace(/^R\$\s*/, '');
        if (/^(?:\d+|\d{1,3}(?:\.\d{3})+),\d{1,2}$/.test(s)) s = s.replace(/\./g, '').replace(',', '.');
        if (!/^\d+(?:\.\d{1,2})?$/.test(s)) return NaN;
        return Number(s);
    }

    const opcoesCat = atual => [
        h('option', { value: '' }, 'Selecione uma categoria'),
        ...S.CATEGORIAS.filter(c => c.ativo || c.id === atual)
            .map(c => h('option', { value: c.id, selected: c.id === atual }, c.nome))
    ];

    /* ---------- formulário de movimentação ---------- */
    function formulario(opts = {}) {
        const edit = opts.id ? S.obter(opts.id) : null;
        const p = Object.assign({ tipo: 'saida', data: S.hoje(), descricao: '', categoriaId: '', valor: '', formaPagamentoId: null, obs: '', repeticao: 'unica', parcelas: 2 },
            opts.preset || {}, edit ? { tipo: edit.tipo, data: edit.data, descricao: edit.descricao, categoriaId: edit.categoriaId, valor: edit.valor, formaPagamentoId: edit.formaPagamentoId, obs: edit.obs } : {});
        let tipo = p.tipo, rep = p.repeticao, tocouStatus = !!edit;

        const inValor = h('input', { id: 'f-valor', type: 'text', inputmode: 'decimal', autocomplete: 'off', placeholder: '0,00', value: p.valor === '' ? null : String(p.valor).replace('.', ',') });
        const inDesc = h('input', { id: 'f-desc', type: 'text', maxlength: '500', autocomplete: 'off', placeholder: 'Ex.: Doação mensal, conta de luz', value: p.descricao || null });
        const selCat = h('select', { id: 'f-cat' }, opcoesCat(p.categoriaId));
        const selForma = h('select', { id: 'f-forma' }, h('option', { value: '' }, 'Não informada'),
            S.FORMAS.filter(f => f.ativo || f.id === p.formaPagamentoId).map(f => h('option', { value: f.id, selected: f.id === p.formaPagamentoId }, f.nome)));
        const inData = h('input', { id: 'f-data', type: 'date', min: '1000-01-01', max: '9998-12-31', value: p.data });
        const inParc = h('input', { id: 'f-parc', type: 'number', min: '2', max: '60', value: String(p.parcelas) });
        const inObs = h('textarea', { id: 'f-obs', maxlength: '30', rows: '2' }, p.obs);
        const inDiaMes = h('input', { id: 'f-dia-mes', type: 'number', min: '1', max: '31', value: String(Number(p.data.slice(8))) });
        const dias = [['MONDAY', 'Segunda'], ['TUESDAY', 'Terça'], ['WEDNESDAY', 'Quarta'], ['THURSDAY', 'Quinta'],
            ['FRIDAY', 'Sexta'], ['SATURDAY', 'Sábado'], ['SUNDAY', 'Domingo']];
        const checkDias = dias.map(([v]) => h('input', { type: 'checkbox', value: v }));
        const chk = h('input', { type: 'checkbox', id: 'f-pago', checked: edit ? edit.status === 'pago' : p.data <= S.hoje() });
        const chkTxt = h('span');
        const rotuloChave = () => { chkTxt.textContent = tipo === 'entrada' ? 'Já foi recebido' : 'Já foi pago'; labForma.textContent = tipo === 'entrada' ? 'Forma de recebimento' : 'Forma de pagamento'; };
        const labForma = h('label', { for: 'f-forma' });
        chk.addEventListener('change', () => { tocouStatus = true; });
        inData.addEventListener('change', () => {
            if (!inData.value) return;
            if (!tocouStatus) chk.checked = inData.value <= S.hoje();
            inDiaMes.value = Number(inData.value.slice(8));
        });

        const blocoParc = h('div', { class: 'campo', hidden: rep !== 'parcelada' }, h('label', { for: 'f-parc' }, 'Número de parcelas'), inParc, h('p', { class: 'dica' }, 'O valor informado acima é o de cada parcela.'));
        const tipoSeg = seg('tipo', [['entrada', 'Entrada'], ['saida', 'Saída']], tipo, v => {
            tipo = v; rotuloChave();
        }, 'tipo');
        const blocoMensal = h('div', { class: 'campo', hidden: true },
            h('label', { for: 'f-dia-mes' }, 'Dia das próximas ocorrências'), inDiaMes);
        const blocoSemanal = h('fieldset', { class: 'campo', hidden: true, tabindex: '-1' },
            h('legend', null, 'Dias da semana'),
            h('div', { class: 'dias-recorrencia' }, dias.map(([, nome], i) => h('label', null, checkDias[i], nome))));
        const repOpcoes = [['unica', 'Não se repete'], ['mensal', 'Todo mês'], ['semanal', 'Toda semana']];
        if (S.CONFIG.parcelamento) repOpcoes.push(['parcelada', 'Parcelado']);
        const repSeg = seg('rep', repOpcoes, rep, v => {
            rep = v;
            blocoParc.hidden = v !== 'parcelada';
            blocoMensal.hidden = v !== 'mensal';
            blocoSemanal.hidden = v !== 'semanal';
        });

        let escopo = 'esta', blocoEscopo = null;
        if (edit && edit.serie) {
            blocoEscopo = h('fieldset', { class: 'campo' }, h('legend', null, 'Aplicar alterações em'),
                seg('escopo', [['esta', 'Somente esta'], ['proximas', 'Esta e as próximas']], 'esta', v => { escopo = v; }),
                h('p', { class: 'dica' }, 'Lançamentos anteriores a esta data não são alterados.'));
        }
        const erro = h('p', { class: 'erro', role: 'alert', hidden: true });
        const salvar = h('button', { class: 'btn btn-primario', type: 'submit' }, edit ? 'Salvar alterações' : 'Adicionar');
        const f = h('form', { class: 'form', novalidate: true },
            h('fieldset', { class: 'campo' }, h('legend', null, 'Tipo'), tipoSeg),
            h('div', { class: 'campo' }, h('label', { for: 'f-valor' }, 'Valor'), h('div', { class: 'campo-valor' }, h('span', null, 'R$'), inValor)),
            h('div', { class: 'campo' }, h('label', { for: 'f-desc' }, 'Descrição'), inDesc),
            h('div', { class: 'duas-colunas' },
                h('div', { class: 'campo' }, h('label', { for: 'f-cat' }, 'Categoria'), selCat),
                h('div', { class: 'campo' }, h('label', { for: 'f-data' }, 'Data'), inData)),
            h('div', { class: 'campo' }, labForma, selForma),
            h('label', { class: 'chave' }, chkTxt, chk, h('span', { class: 'chave-trilho' })),
            edit ? null : h('fieldset', { class: 'campo' }, h('legend', null, 'Repetição'), repSeg),
            edit ? null : [blocoParc, blocoMensal, blocoSemanal],
            h('div', { class: 'campo' }, h('label', { for: 'f-obs' }, 'Observações (opcional)'), inObs),
            blocoEscopo, erro,
            h('div', { class: 'acoes-folha duplo' },
                h('button', { class: 'btn', type: 'button', onclick: () => fechar() }, 'Cancelar'),
                salvar));
        rotuloChave();
        f.addEventListener('submit', async ev => {
            ev.preventDefault();
            if (salvar.disabled) return;
            const valor = lerValor(inValor.value), desc = inDesc.value.trim();
            const diasSemana = checkDias.filter(dia => dia.checked).map(dia => dia.value);
            const falhas = [
                [!(valor >= 0.01 && valor <= 9999999999.99), inValor, 'Informe um valor com até duas casas decimais, a partir de R$ 0,01.'],
                [!desc, inDesc, 'Descreva a movimentação.'],
                [!selCat.value, selCat, 'Selecione a categoria.'],
                [!inData.value || !inData.validity.valid, inData, 'Escolha uma data válida.'],
                [!edit && rep === 'parcelada' && (!inParc.value || !inParc.validity.valid), inParc, 'Informe de 2 a 60 parcelas inteiras.'],
                [!edit && rep === 'mensal' && (!inDiaMes.value || !inDiaMes.validity.valid), inDiaMes, 'Informe um dia entre 1 e 31.'],
                [!edit && rep === 'semanal' && !diasSemana.length, blocoSemanal, 'Selecione pelo menos um dia da semana.']
            ];
            falhas.forEach(([, campo]) => campo.removeAttribute('aria-invalid'));
            const primeira = falhas.find(x => x[0]);
            if (primeira) {
                falhas.filter(x => x[0]).forEach(x => x[1].setAttribute('aria-invalid', 'true'));
                erro.textContent = primeira[2]; erro.hidden = false; primeira[1].focus(); return;
            }
            const dados = {
                tipo, descricao: desc, categoriaId: Number(selCat.value), valor, data: inData.value,
                formaPagamentoId: selForma.value ? Number(selForma.value) : null, obs: inObs.value.trim(),
                status: chk.checked ? 'pago' : 'pendente', repeticao: rep,
                parcelas: rep === 'parcelada' ? Number(inParc.value) : null,
                diaMes: rep === 'mensal' ? Number(inDiaMes.value) : null,
                diasSemana: rep === 'semanal' ? diasSemana : null
            };
            salvar.disabled = true;
            erro.hidden = true;
            try {
                if (edit) await S.atualizar(edit.id, dados, escopo);
                else await S.criar(dados, opts.notaId);
                if (salvar.isConnected) fechar();
                aviso(edit ? 'Alterações salvas' : 'Movimentação adicionada');
            } catch (falha) {
                erro.textContent = falha.message;
                erro.hidden = false;
            } finally {
                salvar.disabled = false;
            }
        });
        folha(edit ? 'Editar movimentação' : 'Nova movimentação', f);
        if (!edit) inValor.focus();
    }

    /* ---------- detalhe ---------- */
    function detalhe(id) {
        const m = S.obter(id); if (!m) return;
        const cat = S.categoria(m.categoriaId), sit = S.situacao(m), e = m.tipo === 'entrada';
        const rep = m.parcelas > 1 ? `Parcela ${m.parcelaAtual} de ${m.parcelas}` : m.recorrente ? (m.semanal ? 'Toda semana' : 'Todo mês') : 'Não se repete';
        const linhas = [
            ['Descrição', m.descricao],
            ['Categoria', h('span', { class: 'cat-ponto' }, h('i', { style: 'background:' + cat.cor }), cat.nome)],
            ['Data', S.dataExtenso(m.data)],
            ['Situação', h('span', { class: sit.chave === 'ok' ? '' : 'mov-situacao ' + sit.chave }, sit.texto)],
            [e ? 'Forma de recebimento' : 'Forma de pagamento', m.forma || 'Não informada'],
            ['Repetição', rep],
            ...(m.obs ? [['Observações', m.obs]] : [])
        ];
        const pago = m.status === 'pago';
        const alternar = async ev => {
            const botao = ev.currentTarget;
            const ok = await executar(botao, () => S.definirPagamento(id, !pago),
                pago ? 'Voltou para pendente' : (e ? 'Recebimento registrado' : 'Pagamento registrado'));
            if (ok && botao.isConnected) fechar();
        };
        folha('Detalhes', [
            h('div', { class: 'detalhe-topo' }, h('span', { class: 'mov-icone ' + m.tipo }, ic(e ? 'cima' : 'baixo')),
                h('div', null, h('p', { class: 'mov-sub' }, e ? 'Entrada' : 'Saída'), h('p', { class: 'detalhe-valor valor ' + m.tipo }, (e ? '+ ' : '- ') + S.moeda(m.valor)))),
            h('dl', { class: 'detalhes' }, linhas.map(([k, v]) => h('div', null, h('dt', null, k), h('dd', null, v)))),
            h('div', { class: 'acoes-folha' },
                h('button', { class: 'btn btn-primario', type: 'button', onclick: alternar },
                    m.status === 'pago' ? (e ? 'Desfazer recebimento' : 'Desfazer pagamento') : (e ? 'Marcar como recebido' : 'Marcar como pago')),
                h('div', { class: 'acoes-folha duplo' },
                    h('button', { class: 'btn', type: 'button', onclick: () => formulario({ id }) }, ic('lapis'), 'Editar'),
                    h('button', { class: 'btn btn-perigo', type: 'button', onclick: () => confirmarExclusao(m) }, ic('lixeira'), 'Excluir')))
        ]);
    }
    function confirmarExclusao(m) {
        let escopo = 'esta';
        folha('Excluir movimentação?', [
            h('p', { class: 'subtitulo', style: 'margin-bottom:16px' }, `“${m.descricao}” de ${S.dataBR(m.data)} será removida.`),
            m.serie ? h('fieldset', { class: 'campo', style: 'margin-bottom:16px' }, h('legend', null, 'Excluir'),
                seg('escopo-ex', [['esta', 'Somente esta'], ['proximas', 'Esta e as próximas']], 'esta', v => { escopo = v; })) : null,
            h('div', { class: 'acoes-folha duplo' },
                h('button', { class: 'btn', type: 'button', onclick: () => detalhe(m.id) }, 'Voltar'),
                h('button', { class: 'btn btn-perigo', type: 'button', onclick: async ev => {
                    const botao = ev.currentTarget;
                    const ok = await executar(botao, () => S.remover(m.id, escopo), 'Movimentação excluída');
                    if (ok && botao.isConnected) fechar();
                } }, 'Excluir'))
        ]);
    }

    /* ---------- filtros e avisos ---------- */
    function filtros(estado, aoAplicar) {
        const e = Object.assign({}, estado);
        const selCat = h('select', { id: 'flt-cat' }, h('option', { value: '' }, 'Todas as categorias'), S.CATEGORIAS.map(c => h('option', { value: c.id, selected: c.id === e.categoriaId }, c.nome)));
        selCat.addEventListener('change', () => { e.categoriaId = Number(selCat.value) || ''; });
        folha('Filtrar', h('div', { class: 'form' },
            h('fieldset', { class: 'campo' }, h('legend', null, 'Situação'), seg('flt-st', [['', 'Todas'], ['pago', 'Concluídas'], ['pendente', 'Pendentes']], e.status || '', v => { e.status = v; })),
            h('fieldset', { class: 'campo' }, h('legend', null, 'Repetição'), seg('flt-rep', [['', 'Todas'], ['mensal', 'Recorrentes'], ['parcelada', 'Parceladas']], e.repeticao || '', v => { e.repeticao = v; })),
            h('div', { class: 'campo' }, h('label', { for: 'flt-cat' }, 'Categoria'), selCat),
            h('div', { class: 'acoes-folha duplo' },
                h('button', { class: 'btn', type: 'button', onclick: () => { aoAplicar({ status: '', categoriaId: '', repeticao: '' }); fechar(); } }, 'Limpar'),
                h('button', { class: 'btn btn-primario', type: 'button', onclick: () => { aoAplicar(e); fechar(); } }, 'Aplicar'))));
    }
    const lista = ms => h('ul', { class: 'extrato solto' }, ms.map(m => linha(m)));
    function avisos() {
        const at = S.atrasadas(), pr = S.proximas(7);
        folha('Avisos de vencimento', at.length || pr.length ? [
            at.length ? [h('p', { class: 'secao-folha' }, 'Em atraso'), lista(at)] : null,
            pr.length ? [h('p', { class: 'secao-folha' }, 'Nos próximos 7 dias'), lista(pr)] : null
        ] : h('p', { class: 'vazio' }, h('strong', null, 'Tudo em dia'), 'Nenhuma conta ou recebimento pendente nos próximos 7 dias.'));
    }
    function ligarSino() {
        const b = document.getElementById('btnAvisos'); if (!b) return;
        if (!b._ok) { b._ok = true; b.addEventListener('click', avisos); }
        const n = S.atrasadas().length + S.proximas(7).length;
        let c = b.querySelector('.sino-contador');
        if (!n) { if (c) c.remove(); b.setAttribute('aria-label', 'Avisos de vencimento: nenhum'); return; }
        if (!c) { c = h('span', { class: 'sino-contador', 'aria-hidden': 'true' }); b.append(c); }
        c.textContent = n; b.setAttribute('aria-label', `Avisos de vencimento: ${n}`);
    }

    window.UI = { h, ic, iniciar, executar, folha, fechar, aviso, linha, detalhe, formulario, filtros, avisos, ligarSino };
})();

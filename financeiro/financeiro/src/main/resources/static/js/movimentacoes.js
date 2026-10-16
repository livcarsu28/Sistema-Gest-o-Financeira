
const movimentacoes = [
    { descricao: "Doação recebida", categoria: "Doações", data: "2026-10-07", tipo: "entrada", valor: 500 },
    { descricao: "Compra de materiais", categoria: "Materiais", data: "2026-10-06", tipo: "saida", valor: 120 },
    { descricao: "Contribuição mensal", categoria: "Doações", data: "2026-10-05", tipo: "entrada", valor: 350 },
    { descricao: "Conta de energia", categoria: "Despesas", data: "2026-10-03", tipo: "saida", valor: 180 }
];

const $ = id => document.getElementById(id);
const moeda = valor => valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL"
});

let mostrarTudo = false;

function renderizar() {
    const entradas = movimentacoes
        .filter(m => m.tipo === "entrada")
        .reduce((s, m) => s + m.valor, 0);

    const saidas = movimentacoes
        .filter(m => m.tipo === "saida")
        .reduce((s, m) => s + m.valor, 0);

    $("totalEntradas").textContent = moeda(entradas);
    $("totalSaidas").textContent = moeda(saidas);
    $("saldo").textContent = moeda(entradas - saidas);

    const pesquisa = $("pesquisa").value.toLowerCase();
    const tipo = $("filtroTipo").value;
    const data = $("filtroData").value;

    let lista = [...movimentacoes].sort((a, b) =>
        b.data.localeCompare(a.data)
    );

    if (mostrarTudo) {
        lista = lista.filter(m =>
            m.descricao.toLowerCase().includes(pesquisa) &&
            (!tipo || m.tipo === tipo) &&
            (!data || m.data === data)
        );
    } else {
        lista = lista.slice(0, 5);
    }

    $("movimentacoesBody").replaceChildren();

    lista.forEach(m => {
        const tr = document.createElement("tr");
        const valores = [
            m.descricao,
            m.categoria,
            new Date(m.data + "T12:00:00").toLocaleDateString("pt-BR"),
            m.tipo === "entrada" ? "Entrada" : "Saída",
            (m.tipo === "entrada" ? "+ " : "- ") + moeda(m.valor)
        ];

        valores.forEach((valor, i) => {
            const td = document.createElement("td");
            td.textContent = valor;
            if (i === 3) td.className = "tag";
            if (i === 4) td.className = "value-" + m.tipo;
            tr.appendChild(td);
        });

        $("movimentacoesBody").appendChild(tr);
    });

    $("emptyState").hidden = lista.length > 0;
}

function fecharModal() {
    $("modal").hidden = true;
}

$("openModal").addEventListener("click", () => {
    $("movimentacaoForm").reset();
    $("data").value = new Date().toLocaleDateString("en-CA");
    $("modal").hidden = false;
});

$("closeModal").addEventListener("click", fecharModal);
$("cancelar").addEventListener("click", fecharModal);

$("modal").addEventListener("click", e => {
    if (e.target === $("modal")) fecharModal();
});

$("verTudo").addEventListener("click", () => {
    mostrarTudo = !mostrarTudo;
    $("filters").hidden = !mostrarTudo;
    $("listTitle").textContent = mostrarTudo
        ? "Todas as movimentações"
        : "Movimentações recentes";
    $("verTudo").textContent = mostrarTudo ? "Ver recentes" : "Ver tudo";
    renderizar();
});

["pesquisa", "filtroTipo", "filtroData"].forEach(id => {
    $(id).addEventListener("input", renderizar);
});

$("movimentacaoForm").addEventListener("submit", e => {
    e.preventDefault();

    movimentacoes.push({
        tipo: $("tipo").value,
        descricao: $("descricao").value.trim(),
        categoria: $("categoria").value.trim(),
        valor: Number($("valor").value),
        data: $("data").value
    });

    fecharModal();
    renderizar();
});

renderizar();


document.addEventListener("DOMContentLoaded", () => {

    const modal = document.getElementById("modal");
    const abrirModal = document.getElementById("openModal");
    const fecharModal = modal?.querySelector("[data-close]");
    const formulario = modal?.querySelector("form");
    const campoData = document.getElementById("dataMov");

    const pesquisa = document.getElementById("search");
    const botoesFiltro = document.querySelectorAll("[data-filter]");
    const movimentacoes = document.querySelectorAll("#items .item");

    // ABRIR MODAL
    abrirModal?.addEventListener("click", () => {

        formulario?.reset();
        atualizarRecorrencia();

        if (campoData) {
            const hoje = new Date();
            const dataLocal = new Date(
                hoje.getTime() - hoje.getTimezoneOffset() * 60000
            );

            campoData.value = dataLocal.toISOString().slice(0, 10);
        }

        modal.hidden = false;
        document.body.classList.add("modal-open");
    });

    // FECHAR MODAL
    function fechar() {
        if (modal) {
            modal.hidden = true;
            document.body.classList.remove("modal-open");
        }
    }

    fecharModal?.addEventListener("click", fechar);

    modal?.addEventListener("click", (event) => {
        if (event.target === modal) {
            fechar();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            fechar();
        }
    });

    // FILTROS DE MOVIMENTAÇÕES
    let filtroAtual = "TODAS";

    function filtrarMovimentacoes() {
        const texto = pesquisa?.value.toLowerCase().trim() || "";

        movimentacoes.forEach((item) => {
            const tipo = item.dataset.tipo || "";
            const descricao = (
                item.dataset.descricao || ""
            ).toLowerCase();

            const correspondeTipo =
                filtroAtual === "TODAS" || tipo === filtroAtual;

            const correspondePesquisa =
                descricao.includes(texto);

            item.hidden = !(correspondeTipo && correspondePesquisa);
        });
    }

    botoesFiltro.forEach((botao) => {
        botao.addEventListener("click", () => {

            botoesFiltro.forEach((b) => {
                b.classList.remove("selected");
            });

            botao.classList.add("selected");

            filtroAtual = botao.dataset.filter;

            filtrarMovimentacoes();
        });
    });

    pesquisa?.addEventListener("input", filtrarMovimentacoes);

    
    const recorrente = document.getElementById("recorrente");
    const camposRecorrencia = document.getElementById("camposRecorrencia");
    const tipoRecorrencia = document.getElementById("tipoRecorrencia");
    const campoDiaMes = document.getElementById("campoDiaMes");
    const campoDiaSemana = document.getElementById("campoDiaSemana");
    const diaMes = document.getElementById("diaMes");

    function atualizarRecorrencia() {
        const ativo = recorrente.checked;
        const mensal = tipoRecorrencia.value === "MENSAL";

        camposRecorrencia.hidden = !ativo;

        tipoRecorrencia.disabled = !ativo;

        campoDiaMes.hidden = !ativo || !mensal;
        diaMes.disabled = !ativo || !mensal;

        campoDiaSemana.hidden = !ativo || mensal;
        document.querySelectorAll('input[name="diasSemana"]')
        .forEach(checkbox => {
            checkbox.disabled = !ativo || mensal;
        });
    }

    recorrente.addEventListener("change", atualizarRecorrencia);
    tipoRecorrencia.addEventListener("change", atualizarRecorrencia);

    atualizarRecorrencia();

});

/**
 * relatorios.js
 * Comportamento interativo da tela de Relatórios Financeiros.
 * Sistema de Gestão Financeira — Resgate de Vidas.
 */
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    const $ = (id) => document.getElementById(id);

    // =========================================================================
    // 1. GERENCIAMENTO DE ABAS E FILTROS DE PERÍODO
    // =========================================================================
    const botoesAbas = document.querySelectorAll(".aba-btn");
    const paineisAbas = document.querySelectorAll(".aba-painel");
    const inputAba = $("input-aba");
    const selectMes = $("select-mes");
    const selectAno = $("select-ano");
    const formFiltros = $("form-filtros");

    // Submissão automática do filtro ao selecionar novo mês ou ano
    if (formFiltros) {
        if (selectMes) {
            selectMes.addEventListener("change", () => {
                formFiltros.submit();
            });
        }
        if (selectAno) {
            selectAno.addEventListener("change", () => {
                formFiltros.submit();
            });
        }
    }

    botoesAbas.forEach(btn => {
        btn.addEventListener("click", () => {
            const abaAlvo = btn.getAttribute("data-aba");
            if (!abaAlvo) return;

            // Atualiza botões
            botoesAbas.forEach(b => b.classList.remove("selected"));
            btn.classList.add("selected");

            // Atualiza painéis
            paineisAbas.forEach(p => p.classList.remove("ativo"));
            const painelAlvo = $(`aba-conteudo-${abaAlvo}`);
            if (painelAlvo) {
                painelAlvo.classList.add("ativo");
            }

            // Atualiza campo oculto do formulário de filtros
            if (inputAba) {
                inputAba.value = abaAlvo;
            }

            // Atualiza URL sem recarregar a tela
            const url = new URL(window.location.href);
            url.searchParams.set("aba", abaAlvo);
            window.history.replaceState({}, "", url.toString());
        });
    });

    // =========================================================================
    // 2. IMPRESSÃO / EXPORTAÇÃO PDF
    // =========================================================================
    const modalImpressao = $("modal-impressao");
    const formImpressao = modalImpressao?.querySelector("form[method='get']");
    if (formImpressao) {
        const inicio = formImpressao.elements.inicio;
        const fim = formImpressao.elements.fim;
        const ajustarPeriodo = () => {
            fim.min = inicio.value;
            fim.setCustomValidity(fim.value && fim.value < inicio.value ? "A data final deve ser igual ou posterior à inicial." : "");
        };
        $("btn-imprimir")?.addEventListener("click", () => {
            ajustarPeriodo();
            modalImpressao.showModal();
        });
        inicio.addEventListener("change", ajustarPeriodo);
        fim.addEventListener("change", ajustarPeriodo);
    }

    // =========================================================================
    // 3. TOAST DE FEEDBACK
    // =========================================================================
    const toast = $("toast-notificacao");
    const toastMsg = $("toast-mensagem");
    let timerToast = null;

    function exibirToast(mensagem, duracaoMs = 4000) {
        if (!toast || !toastMsg) return;
        clearTimeout(timerToast);
        toastMsg.textContent = mensagem;
        toast.hidden = false;
        toast.style.display = "block";

        timerToast = setTimeout(() => {
            toast.hidden = true;
            toast.style.display = "none";
        }, duracaoMs);
    }

    // =========================================================================
    // 4. MODAL DE PUBLICAÇÃO NO SITE INSTITUCIONAL
    // =========================================================================
    const modalPublicacao = $("modal-publicacao");
    const btnAbrirPublicacao = $("btn-abrir-publicacao");
    const btnPublicarTela = $("btn-publicar-tela");
    const btnFecharModal = $("btn-fechar-modal");
    const btnCancelarModal = $("btn-cancelar-modal");
    const formConfirmarPublicacao = $("form-confirmar-publicacao");

    function abrirModal() {
        if (!modalPublicacao) return;
        modalPublicacao.hidden = false;
        modalPublicacao.setAttribute("aria-hidden", "false");
        modalPublicacao.style.display = "flex";
    }

    function fecharModal() {
        if (!modalPublicacao) return;
        modalPublicacao.hidden = true;
        modalPublicacao.setAttribute("aria-hidden", "true");
        modalPublicacao.style.display = "none";
    }

    if (btnAbrirPublicacao) btnAbrirPublicacao.addEventListener("click", abrirModal);
    if (btnPublicarTela) btnPublicarTela.addEventListener("click", abrirModal);
    if (btnFecharModal) btnFecharModal.addEventListener("click", fecharModal);
    if (btnCancelarModal) btnCancelarModal.addEventListener("click", fecharModal);

    // Fechar ao clicar fora do conteúdo (no backdrop cinza escuro)
    if (modalPublicacao) {
        modalPublicacao.addEventListener("click", (e) => {
            if (e.target === modalPublicacao) {
                fecharModal();
            }
        });

        // Fechar ao pressionar a tecla Escape
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && modalPublicacao.style.display !== "none" && !modalPublicacao.hidden) {
                fecharModal();
            }
        });
    }

    // Submissão da Publicação
    if (formConfirmarPublicacao) {
        formConfirmarPublicacao.addEventListener("submit", async (e) => {
            e.preventDefault();

            const btnConfirmar = $("btn-confirmar-publicacao");
            if (btnConfirmar) {
                btnConfirmar.disabled = true;
                btnConfirmar.textContent = "Publicando...";
            }

            try {
                const formData = new FormData(formConfirmarPublicacao);
                const resposta = await fetch(formConfirmarPublicacao.action, {
                    method: "POST",
                    headers: {
                        "Accept": "application/json"
                    },
                    body: new URLSearchParams(formData)
                });

                if (resposta.ok) {
                    const dados = await resposta.json();

                    // Atualiza os dados visuais no card de status
                    const elMes = $("dado-mes-publicado");
                    const elGastos = $("dado-gastos-publicado");
                    const elData = $("dado-data-publicacao");
                    const badge = $("badge-status-site");

                    if (elMes && dados.periodoFormatado) elMes.textContent = dados.periodoFormatado;
                    if (elGastos && dados.totalGastos !== undefined) {
                        elGastos.textContent = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(dados.totalGastos);
                    }
                    if (elData) {
                        elData.textContent = "Atualizado agora mesmo";
                    }
                    if (badge) {
                        badge.textContent = dados.statusAtualizacao || "Atualizado";
                    }

                    fecharModal();
                    exibirToast("Informações financeiras publicadas com sucesso no site institucional!");
                } else {
                    throw new Error("Erro ao registrar publicação no servidor.");
                }
            } catch (err) {
                console.error("Falha ao publicar dados:", err);
                exibirToast("Não foi possível publicar os dados no momento. Tente novamente.");
            } finally {
                if (btnConfirmar) {
                    btnConfirmar.disabled = false;
                    btnConfirmar.textContent = "Confirmar e Publicar";
                }
            }
        });
    }
});

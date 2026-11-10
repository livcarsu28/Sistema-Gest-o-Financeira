/**
 * configuracoes.js
 * Lógica e comportamentos interativos da tela de Ajustes / Configurações.
 * Sistema de Gestão Financeira — Resgate de Vidas.
 */
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // Utilitários de seleção
    const $ = (id) => document.getElementById(id);

    // =========================================================================
    // TOAST FEEDBACK
    // =========================================================================
    const toast = $("toast-notificacao");
    const toastMsg = $("toast-mensagem");
    let timerToast = null;

    function exibirToast(mensagem, duracaoMs = 3500) {
        if (!toast || !toastMsg) return;
        clearTimeout(timerToast);
        toastMsg.textContent = mensagem;
        toast.hidden = false;

        timerToast = setTimeout(() => {
            toast.hidden = true;
        }, duracaoMs);
    }

    // =========================================================================
    // 5.1. CABEÇALHO - BOTÃO DE SINO DE NOTIFICAÇÕES
    // =========================================================================
    const btnSinoHeader = $("btn-sino-header");
    if (btnSinoHeader) {
        btnSinoHeader.addEventListener("click", () => {
            exibirToast("Nenhuma notificação pendente no momento.");
        });
    }

    // =========================================================================
    // 5.2. PERFIL DE ACESSO
    // =========================================================================
    const perfilSelect = $("perfil-select");
    if (perfilSelect) {
        perfilSelect.addEventListener("change", () => {
            // Conforme regra de arquitetura: permissões são gerenciadas pelo administrador
            perfilSelect.value = "Administrador";
            exibirToast("O perfil de acesso é definido pela administração do sistema.");
        });
    }

    // =========================================================================
    // 5.3. GERENCIAMENTO DE IMAGENS E UPLOAD
    // =========================================================================
    const selectCategoria = $("select-categoria");
    const areaUpload = $("area-upload");
    const inputArquivoImagem = $("input-arquivo-imagem");
    const uploadEstadoInicial = $("upload-estado-inicial");
    const uploadEstadoSelecionado = $("upload-estado-selecionado");
    const uploadPreviewImg = $("upload-preview-img");
    const uploadNomeArquivo = $("upload-nome-arquivo");
    const uploadTamanhoArquivo = $("upload-tamanho-arquivo");
    const btnRemoverImagem = $("btn-remover-imagem");
    const feedbackUpload = $("feedback-upload");

    const TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024; // 10 MB
    const TIPOS_PERMITIDOS = ["image/png", "image/jpeg", "image/jpg"];

    function formatarTamanhoBytes(bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
        return (bytes / (1024 * 1024)).toFixed(2) + " MB";
    }

    function exibirMensagemUpload(texto, tipo = "erro") {
        if (!feedbackUpload) return;
        feedbackUpload.textContent = texto;
        feedbackUpload.className = `configuracoes-mensagem-feedback ${tipo}`;
    }

    function limparMensagemUpload() {
        if (!feedbackUpload) return;
        feedbackUpload.textContent = "";
        feedbackUpload.className = "configuracoes-mensagem-feedback";
    }

    function resetarUpload() {
        if (inputArquivoImagem) inputArquivoImagem.value = "";
        if (uploadPreviewImg) {
            uploadPreviewImg.removeAttribute("src");
            uploadPreviewImg.hidden = true;
        }
        const iconePlaceholder = $("upload-preview-icone-padrao");
        if (iconePlaceholder) iconePlaceholder.hidden = false;
        if (uploadEstadoSelecionado) {
            uploadEstadoSelecionado.hidden = true;
            uploadEstadoSelecionado.style.display = "none";
        }
        if (uploadEstadoInicial) {
            uploadEstadoInicial.hidden = false;
            uploadEstadoInicial.style.display = "flex";
        }
        limparMensagemUpload();
    }

    function processarArquivoImagem(arquivo) {
        if (!arquivo) return;

        limparMensagemUpload();

        // 1. Validar tipo
        const tipoValido = TIPOS_PERMITIDOS.includes(arquivo.type.toLowerCase()) ||
            /\.(jpe?g|png)$/i.test(arquivo.name);

        if (!tipoValido) {
            exibirMensagemUpload("Formato não suportado. Por favor, envie uma imagem PNG ou JPG.", "erro");
            resetarUpload();
            return;
        }

        // 2. Validar tamanho máximo (10 MB)
        if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
            exibirMensagemUpload(`O arquivo excede o limite de 10 MB (tamanho atual: ${formatarTamanhoBytes(arquivo.size)}).`, "erro");
            resetarUpload();
            return;
        }

        // 3. Exibir pré-visualização e dados
        const leitor = new FileReader();
        leitor.onload = (e) => {
            if (uploadPreviewImg) {
                uploadPreviewImg.src = e.target.result;
                uploadPreviewImg.hidden = false;
            }
            const iconePlaceholder = $("upload-preview-icone-padrao");
            if (iconePlaceholder) iconePlaceholder.hidden = true;
            if (uploadNomeArquivo) uploadNomeArquivo.textContent = arquivo.name;
            if (uploadTamanhoArquivo) uploadTamanhoArquivo.textContent = formatarTamanhoBytes(arquivo.size);

            if (uploadEstadoInicial) {
                uploadEstadoInicial.hidden = true;
                uploadEstadoInicial.style.display = "none";
            }
            if (uploadEstadoSelecionado) {
                uploadEstadoSelecionado.hidden = false;
                uploadEstadoSelecionado.style.display = "flex";
            }

            const categoriaSelecionada = selectCategoria ? selectCategoria.value : "Projetos";
            exibirMensagemUpload(`Imagem pronta para a categoria "${categoriaSelecionada}".`, "sucesso");
            exibirToast(`Imagem "${arquivo.name}" validada com sucesso!`);
        };
        leitor.readAsDataURL(arquivo);
    }

    if (areaUpload && inputArquivoImagem) {
        // Clique na área inteira abre o seletor de arquivos
        areaUpload.addEventListener("click", (e) => {
            if (e.target === btnRemoverImagem || btnRemoverImagem?.contains(e.target)) {
                return;
            }
            inputArquivoImagem.click();
        });

        // Acessibilidade por teclado (Enter ou Espaço na área)
        areaUpload.addEventListener("keydown", (e) => {
            if ((e.key === "Enter" || e.key === " ") && e.target === areaUpload) {
                e.preventDefault();
                inputArquivoImagem.click();
            }
        });

        // Mudança no input file
        inputArquivoImagem.addEventListener("change", (e) => {
            const arquivo = e.target.files && e.target.files[0];
            processarArquivoImagem(arquivo);
        });

        // Suporte a Arrastar e Soltar (Drag & Drop)
        ["dragenter", "dragover"].forEach((evento) => {
            areaUpload.addEventListener(evento, (e) => {
                e.preventDefault();
                e.stopPropagation();
                areaUpload.classList.add("dragover");
            });
        });

        ["dragleave", "drop"].forEach((evento) => {
            areaUpload.addEventListener(evento, (e) => {
                e.preventDefault();
                e.stopPropagation();
                areaUpload.classList.remove("dragover");
            });
        });

        areaUpload.addEventListener("drop", (e) => {
            const arquivos = e.dataTransfer && e.dataTransfer.files;
            if (arquivos && arquivos.length > 0) {
                processarArquivoImagem(arquivos[0]);
            }
        });
    }

    if (btnRemoverImagem) {
        btnRemoverImagem.addEventListener("click", (e) => {
            e.stopPropagation();
            resetarUpload();
            exibirToast("Imagem removida.");
        });
    }

    if (selectCategoria) {
        selectCategoria.addEventListener("change", () => {
            if (uploadEstadoSelecionado && !uploadEstadoSelecionado.hidden) {
                exibirMensagemUpload(`Categoria atualizada para "${selectCategoria.value}".`, "sucesso");
            }
        });
    }

    // =========================================================================
    // 5.4. CHAVE PIX (Modal e Edição)
    // =========================================================================
    const textoChavePix = $("texto-chave-pix");
    const btnAbrirModalPix = $("btn-abrir-modal-pix");
    const modalPix = $("modal-pix");
    const btnFecharModalPix = $("btn-fechar-modal-pix");
    const btnCancelarModalPix = $("btn-cancelar-modal-pix");
    const formEditarPix = $("form-editar-pix");
    const inputEditarChavePix = $("input-editar-chave-pix");
    const erroPix = $("erro-pix");

    function abrirModalPix() {
        if (!modalPix) return;
        clearTimeout(timerToast);
        if (toast) toast.hidden = true;
        if (inputEditarChavePix && textoChavePix) {
            inputEditarChavePix.value = textoChavePix.dataset.chave || "";
        }
        if (erroPix) erroPix.textContent = "";
        inputEditarChavePix?.setAttribute("aria-invalid", "false");
        modalPix.hidden = false;
        setTimeout(() => inputEditarChavePix?.focus(), 50);
    }

    function fecharModalPix() {
        if (!modalPix) return;
        modalPix.hidden = true;
        if (erroPix) erroPix.textContent = "";
        btnAbrirModalPix?.focus();
    }

    function validarChavePix(valor) {
        const v = valor.trim();
        if (!v) return "A chave PIX não pode ser vazia.";
        if (v.length < 5 || v.length > 100) {
            return "A chave PIX deve ter entre 5 e 100 caracteres.";
        }
        return null;
    }

    if (btnAbrirModalPix) {
        btnAbrirModalPix.addEventListener("click", abrirModalPix);
    }
    if (btnFecharModalPix) {
        btnFecharModalPix.addEventListener("click", fecharModalPix);
    }
    if (btnCancelarModalPix) {
        btnCancelarModalPix.addEventListener("click", fecharModalPix);
    }
    if (modalPix) {
        modalPix.addEventListener("click", (e) => {
            if (e.target === modalPix) fecharModalPix();
        });
    }

    if (formEditarPix) {
        formEditarPix.addEventListener("submit", async (e) => {
            e.preventDefault();
            const novoValor = inputEditarChavePix ? inputEditarChavePix.value.trim() : "";
            const erro = validarChavePix(novoValor);

            if (erro) {
                if (erroPix) erroPix.textContent = erro;
                inputEditarChavePix?.setAttribute("aria-invalid", "true");
                inputEditarChavePix?.focus();
                return;
            }

            const botaoSalvar = formEditarPix.querySelector('button[type="submit"]');
            if (botaoSalvar.disabled) return;
            botaoSalvar.disabled = true;
            formEditarPix.setAttribute("aria-busy", "true");
            if (erroPix) erroPix.textContent = "";
            inputEditarChavePix.setAttribute("aria-invalid", "false");
            try {
                const resposta = await fetch(formEditarPix.action, {
                    method: "POST",
                    headers: { "Accept": "application/json" },
                    body: new URLSearchParams(new FormData(formEditarPix))
                });
                if (resposta.redirected || !resposta.headers.get("content-type")?.includes("application/json")) {
                    throw new Error("Sua sessão expirou ou a solicitação foi recusada. Recarregue a página e tente novamente.");
                }
                const dados = await resposta.json();
                if (!resposta.ok) throw new Error(dados.mensagem || "Não foi possível salvar a chave Pix.");
                if (textoChavePix) {
                    textoChavePix.textContent = dados.chave;
                    textoChavePix.dataset.chave = dados.chave;
                }
                const mensagemPix = $("mensagem-pix");
                if (mensagemPix) mensagemPix.textContent = "Chave Pix cadastrada para a instituição.";
                fecharModalPix();
                exibirToast(dados.mensagem);
            } catch (erroSalvar) {
                if (erroPix) erroPix.textContent = erroSalvar.message || "Não foi possível salvar a chave Pix. Tente novamente.";
                inputEditarChavePix.setAttribute("aria-invalid", "true");
                inputEditarChavePix.focus();
            } finally {
                botaoSalvar.disabled = false;
                formEditarPix.removeAttribute("aria-busy");
            }
        });
    }

    // =========================================================================
    // 5.5. ENDEREÇO DA INSTITUIÇÃO (Modal e Edição)
    // =========================================================================
    const textoEnderecoLinha1 = $("texto-endereco-linha1");
    const textoEnderecoLinha2 = $("texto-endereco-linha2");
    const btnAbrirModalEndereco = $("btn-abrir-modal-endereco");
    const modalEndereco = $("modal-endereco");
    const btnFecharModalEndereco = $("btn-fechar-modal-endereco");
    const btnCancelarModalEndereco = $("btn-cancelar-modal-endereco");
    const formEditarEndereco = $("form-editar-endereco");
    const inputLogradouro = $("input-logradouro");
    const inputNumero = $("input-numero");
    const inputBairro = $("input-bairro");
    const inputCidade = $("input-cidade");
    const inputUf = $("input-uf");
    const inputCep = $("input-cep");
    const erroEndereco = $("erro-endereco");

    // Estado padrão do endereço
    let dadosEndereco = {
        logradouro: "Rua da Esperança",
        numero: "128",
        bairro: "Centro",
        cidade: "São Paulo",
        uf: "SP",
        cep: "01010-001"
    };

    // Restaurar endereço do localStorage se existir
    const enderecoSalvo = localStorage.getItem("config_endereco");
    if (enderecoSalvo) {
        try {
            dadosEndereco = JSON.parse(enderecoSalvo);
            atualizarVisualizacaoEndereco();
        } catch {
            // Mantém os padrões
        }
    }

    function atualizarVisualizacaoEndereco() {
        if (textoEnderecoLinha1) {
            textoEnderecoLinha1.textContent = `${dadosEndereco.logradouro}, ${dadosEndereco.numero} - ${dadosEndereco.bairro}`;
        }
        if (textoEnderecoLinha2) {
            textoEnderecoLinha2.textContent = `${dadosEndereco.cidade} - ${dadosEndereco.uf} · CEP ${dadosEndereco.cep}`;
        }
    }

    function abrirModalEndereco() {
        if (!modalEndereco) return;
        if (inputLogradouro) inputLogradouro.value = dadosEndereco.logradouro;
        if (inputNumero) inputNumero.value = dadosEndereco.numero;
        if (inputBairro) inputBairro.value = dadosEndereco.bairro;
        if (inputCidade) inputCidade.value = dadosEndereco.cidade;
        if (inputUf) inputUf.value = dadosEndereco.uf;
        if (inputCep) inputCep.value = dadosEndereco.cep;
        if (erroEndereco) erroEndereco.textContent = "";

        modalEndereco.hidden = false;
        setTimeout(() => inputLogradouro?.focus(), 50);
    }

    function fecharModalEndereco() {
        if (!modalEndereco) return;
        modalEndereco.hidden = true;
        if (erroEndereco) erroEndereco.textContent = "";
        btnAbrirModalEndereco?.focus();
    }

    if (btnAbrirModalEndereco) {
        btnAbrirModalEndereco.addEventListener("click", abrirModalEndereco);
    }
    if (btnFecharModalEndereco) {
        btnFecharModalEndereco.addEventListener("click", fecharModalEndereco);
    }
    if (btnCancelarModalEndereco) {
        btnCancelarModalEndereco.addEventListener("click", fecharModalEndereco);
    }
    if (modalEndereco) {
        modalEndereco.addEventListener("click", (e) => {
            if (e.target === modalEndereco) fecharModalEndereco();
        });
    }

    // Máscara simples de CEP ao digitar
    if (inputCep) {
        inputCep.addEventListener("input", (e) => {
            let v = e.target.value.replace(/\D/g, "");
            if (v.length > 8) v = v.slice(0, 8);
            if (v.length > 5) {
                v = v.slice(0, 5) + "-" + v.slice(5);
            }
            e.target.value = v;
        });
    }

    if (formEditarEndereco) {
        formEditarEndereco.addEventListener("submit", (e) => {
            e.preventDefault();

            const logradouro = inputLogradouro?.value.trim() || "";
            const numero = inputNumero?.value.trim() || "";
            const bairro = inputBairro?.value.trim() || "";
            const cidade = inputCidade?.value.trim() || "";
            const uf = inputUf?.value.trim().toUpperCase() || "";
            const cep = inputCep?.value.trim() || "";

            if (!logradouro || !numero || !bairro || !cidade || !uf || !cep) {
                if (erroEndereco) erroEndereco.textContent = "Por favor, preencha todos os campos do endereço.";
                return;
            }

            if (uf.length !== 2) {
                if (erroEndereco) erroEndereco.textContent = "A UF deve conter exatamente 2 letras (ex.: SP).";
                inputUf?.focus();
                return;
            }

            dadosEndereco = { logradouro, numero, bairro, cidade, uf, cep };
            localStorage.setItem("config_endereco", JSON.stringify(dadosEndereco));
            atualizarVisualizacaoEndereco();
            fecharModalEndereco();
            exibirToast("Endereço oficial atualizado com sucesso!");
        });
    }

    // Fechar modais ao pressionar tecla Escape
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            if (modalPix && !modalPix.hidden) fecharModalPix();
            if (modalEndereco && !modalEndereco.hidden) fecharModalEndereco();
        }
    });

    // =========================================================================
    // 5.6. NOTIFICAÇÕES (Switches)
    // =========================================================================
    const switchesNotificacao = [
        { id: "switch-notif-lancamentos", chaveStorage: "notif_lancamentos", nome: "Lançamentos", padrao: true },
        { id: "switch-notif-lembretes", chaveStorage: "notif_lembretes", nome: "Lembretes", padrao: true },
        { id: "switch-notif-relatorios", chaveStorage: "notif_relatorios", nome: "Relatórios", padrao: false }
    ];

    switchesNotificacao.forEach(({ id, chaveStorage, nome, padrao }) => {
        const elementoSwitch = $(id);
        if (!elementoSwitch) return;

        // Recuperar preferência salva se houver
        const estadoSalvo = localStorage.getItem(chaveStorage);
        if (estadoSalvo !== null) {
            elementoSwitch.checked = estadoSalvo === "true";
        } else {
            elementoSwitch.checked = padrao;
        }
        elementoSwitch.setAttribute("aria-checked", String(elementoSwitch.checked));

        // Evento de alteração
        elementoSwitch.addEventListener("change", () => {
            const ativo = elementoSwitch.checked;
            elementoSwitch.setAttribute("aria-checked", String(ativo));
            localStorage.setItem(chaveStorage, String(ativo));
            exibirToast(`Notificações de ${nome} ${ativo ? "ativadas" : "desativadas"}.`);
        });
    });
});


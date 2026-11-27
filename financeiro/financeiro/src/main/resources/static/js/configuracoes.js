/**
 * configuracoes.js
 * Lógica e comportamentos interativos da tela de Ajustes / Configurações.
 * Sistema de Gestão Financeira — Resgate de Vidas.
 */
document.addEventListener("DOMContentLoaded", () => {
    "use strict";

    // Utilitários de seleção
    const $ = (id) => document.getElementById(id);

    function obterCsrf() {
        const metaToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const metaHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        const inputToken = document.querySelector('input[name="_csrf"]')?.value;
        return {
            token: metaToken || inputToken || "",
            headerName: metaHeader || "X-CSRF-TOKEN"
        };
    }

    function montarHeadersComCsrf(headersBase = {}) {
        const csrf = obterCsrf();
        const headers = { ...headersBase };
        if (csrf.token) {
            headers[csrf.headerName] = csrf.token;
        }
        return headers;
    }

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
    // 5.2. PERFIL DE ACESSO E GESTÃO DE USUÁRIOS
    // =========================================================================
    const btnAbrirModalUsuario = $("btn-abrir-modal-usuario");
    const modalUsuario = $("modal-usuario");
    const btnFecharModalUsuario = $("btn-fechar-modal-usuario");
    const btnCancelarModalUsuario = $("btn-cancelar-modal-usuario");
    const formCadastrarUsuario = $("form-cadastrar-usuario");
    const erroUsuario = $("erro-usuario");

    function abrirModalUsuario() {
        if (!modalUsuario) return;
        if (erroUsuario) erroUsuario.textContent = "";
        formCadastrarUsuario?.reset();
        modalUsuario.hidden = false;
        setTimeout(() => $("input-usuario-nome")?.focus(), 50);
    }

    function fecharModalUsuario() {
        if (!modalUsuario) return;
        modalUsuario.hidden = true;
        if (erroUsuario) erroUsuario.textContent = "";
        btnAbrirModalUsuario?.focus();
    }

    if (btnAbrirModalUsuario) {
        btnAbrirModalUsuario.addEventListener("click", abrirModalUsuario);
    }
    if (btnFecharModalUsuario) {
        btnFecharModalUsuario.addEventListener("click", fecharModalUsuario);
    }
    if (btnCancelarModalUsuario) {
        btnCancelarModalUsuario.addEventListener("click", fecharModalUsuario);
    }
    if (modalUsuario) {
        modalUsuario.addEventListener("click", (e) => {
            if (e.target === modalUsuario) fecharModalUsuario();
        });
    }

    if (formCadastrarUsuario) {
        formCadastrarUsuario.addEventListener("submit", async (e) => {
            e.preventDefault();
            const nome = $("input-usuario-nome")?.value.trim() || "";
            const email = $("input-usuario-email")?.value.trim() || "";
            const senha = $("input-usuario-senha")?.value.trim() || "";

            if (!nome || !email || !senha) {
                if (erroUsuario) erroUsuario.textContent = "Preencha todos os campos obrigatórios.";
                return;
            }
            if (senha.length < 6) {
                if (erroUsuario) erroUsuario.textContent = "A senha deve ter no mínimo 6 caracteres.";
                return;
            }

            const btnSubmit = formCadastrarUsuario.querySelector('button[type="submit"]');
            if (btnSubmit) btnSubmit.disabled = true;
            if (erroUsuario) erroUsuario.textContent = "";

            try {
                const params = new URLSearchParams();
                params.append("nome", nome);
                params.append("email", email);
                params.append("senha", senha);

                const headers = montarHeadersComCsrf({
                    "Content-Type": "application/x-www-form-urlencoded",
                    "Accept": "application/json"
                });

                const resposta = await fetch(formCadastrarUsuario.action, {
                    method: "POST",
                    credentials: "same-origin",
                    headers: headers,
                    body: params
                });

                const dados = await resposta.json().catch(() => ({}));
                if (!resposta.ok) {
                    if (resposta.status === 403 && !dados.mensagem) {
                        throw new Error("Acesso negado: apenas administradores podem cadastrar usuários.");
                    }
                    throw new Error(dados.mensagem || "Não foi possível cadastrar o usuário.");
                }

                fecharModalUsuario();
                exibirToast(`Usuário "${dados.nomeCompleto || nome}" cadastrado com sucesso!`);
            } catch (err) {
                if (erroUsuario) erroUsuario.textContent = err.message || "Erro ao cadastrar usuário.";
            } finally {
                if (btnSubmit) btnSubmit.disabled = false;
            }
        });
    }

    // =========================================================================
    // 5.3. GERENCIAMENTO DE IMAGENS E UPLOAD (LIMITE DE 10 IMAGENS)
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
    const btnConfirmarUpload = $("btn-confirmar-upload");
    const feedbackUpload = $("feedback-upload");
    const contadorImagensTexto = $("contador-imagens-texto");
    const avisoLimiteAtingido = $("aviso-limite-atingido");
    const galeriaImagensLista = $("galeria-imagens-lista");

    const LIMITE_MAXIMO_IMAGENS = 10;
    const TAMANHO_MAXIMO_BYTES = 10 * 1024 * 1024; // 10 MB
    const TIPOS_PERMITIDOS = ["image/png", "image/jpeg", "image/jpg"];

    let totalImagensAtuais = 0;
    let arquivoSelecionado = null;

    function atualizarEstadoContador(total) {
        totalImagensAtuais = total;
        if (contadorImagensTexto) {
            contadorImagensTexto.textContent = `${total} de ${LIMITE_MAXIMO_IMAGENS} imagens adicionadas`;
        }
        const atingiuLimite = total >= LIMITE_MAXIMO_IMAGENS;
        if (avisoLimiteAtingido) {
            avisoLimiteAtingido.hidden = !atingiuLimite;
        }
        if (areaUpload) {
            if (atingiuLimite) {
                areaUpload.classList.add("upload-bloqueado");
                areaUpload.style.pointerEvents = "none";
                areaUpload.style.opacity = "0.5";
                const rotulo = $("rotulo-upload-texto");
                if (rotulo) rotulo.textContent = "Limite de 10 fotos atingido";
                const ajuda = $("ajuda-upload-texto");
                if (ajuda) ajuda.textContent = "Remova uma foto abaixo para adicionar outra";
            } else {
                areaUpload.classList.remove("upload-bloqueado");
                areaUpload.style.pointerEvents = "auto";
                areaUpload.style.opacity = "1";
                const rotulo = $("rotulo-upload-texto");
                if (rotulo) rotulo.textContent = "Fazer upload";
                const ajuda = $("ajuda-upload-texto");
                if (ajuda) ajuda.textContent = "PNG ou JPG de até 10 MB";
            }
        }
    }

    async function carregarGaleriaImagens() {
        try {
            const resp = await fetch("/configuracoes/imagens", { credentials: "same-origin" });
            if (!resp.ok) return;
            const dados = await resp.json();
            atualizarEstadoContador(dados.total || 0);

            if (galeriaImagensLista && dados.imagens) {
                galeriaImagensLista.innerHTML = "";
                if (dados.imagens.length === 0) {
                    galeriaImagensLista.innerHTML = '<p style="color: #475569; font-size: 13px; grid-column: 1 / -1;">Nenhuma imagem cadastrada no momento.</p>';
                } else {
                    dados.imagens.forEach((img) => {
                        const item = document.createElement("div");
                        item.style.cssText = "background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px; text-align: center; position: relative;";
                        item.innerHTML = `
                            <div style="height: 70px; background: #e2e8f0; border-radius: 6px; display: flex; align-items: center; justify-content: center; overflow: hidden; margin-bottom: 6px;">
                                <img alt="" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.onerror=null; this.src='/img/logo-resgate-de-vidas.png';">
                            </div>
                            <span style="font-size: 11px; font-weight: 600; color: #475569; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"></span>
                            <button type="button" class="btn-remover-foto-galeria"  style="margin-top: 6px; width: 100%; font-size: 11px; padding: 4px; background: #fee2e2; color: #991b1b; border: none; border-radius: 4px; cursor: pointer;">
                                Remover
                            </button>
                        `;
                        const miniatura = item.querySelector("img");
                        const url = new URL(img.urlImagem, location.origin);
                        if (url.origin === location.origin && url.pathname.startsWith("/configuracoes/imagens/arquivo/")) {
                            miniatura.src = url.href;
                        }
                        miniatura.alt = "Foto " + img.categoria;
                        item.querySelector("span").textContent = img.categoria;
                        const remover = item.querySelector(".btn-remover-foto-galeria");
                        if ($("publicar-imagem-site")) remover.dataset.id = img.id;
                        else remover.remove();
                        galeriaImagensLista.appendChild(item);
                    });

                    galeriaImagensLista.querySelectorAll(".btn-remover-foto-galeria").forEach((btn) => {
                        btn.addEventListener("click", async (e) => {
                            const idFoto = e.currentTarget.getAttribute("data-id");
                            await excluirImagem(idFoto);
                        });
                    });
                }
            }
        } catch {
            // Em slice tests ou offline, mantém o padrão
        }
    }

    async function excluirImagem(idFoto) {
        try {
            const headers = montarHeadersComCsrf();
            const resp = await fetch(`/configuracoes/imagens/${idFoto}`, {
                method: "DELETE",
                credentials: "same-origin",
                headers: headers
            });
            const dados = await resp.json().catch(() => ({}));
            if (resp.ok) {
                exibirToast(dados.mensagem || "Imagem removida com sucesso!");
                await carregarGaleriaImagens();
            } else {
                exibirToast(dados.mensagem || "Não foi possível remover a imagem.");
            }
        } catch {
            exibirToast("Erro de comunicação ao remover imagem.");
        }
    }

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
        arquivoSelecionado = null;
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
        arquivoSelecionado = arquivo;

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
        areaUpload.addEventListener("click", (e) => {
            if (totalImagensAtuais >= LIMITE_MAXIMO_IMAGENS) return;
            if (e.target === btnRemoverImagem || btnRemoverImagem?.contains(e.target) ||
                e.target === btnConfirmarUpload || btnConfirmarUpload?.contains(e.target)) {
                return;
            }
            inputArquivoImagem.click();
        });

        areaUpload.addEventListener("keydown", (e) => {
            if (totalImagensAtuais >= LIMITE_MAXIMO_IMAGENS) return;
            if ((e.key === "Enter" || e.key === " ") && e.target === areaUpload) {
                e.preventDefault();
                inputArquivoImagem.click();
            }
        });

        inputArquivoImagem.addEventListener("change", (e) => {
            const arquivo = e.target.files && e.target.files[0];
            processarArquivoImagem(arquivo);
        });

        ["dragenter", "dragover"].forEach((evento) => {
            areaUpload.addEventListener(evento, (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (totalImagensAtuais < LIMITE_MAXIMO_IMAGENS) {
                    areaUpload.classList.add("dragover");
                }
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
            if (totalImagensAtuais >= LIMITE_MAXIMO_IMAGENS) return;
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
            exibirToast("Seleção cancelada.");
        });
    }

    if (btnConfirmarUpload) {
        btnConfirmarUpload.addEventListener("click", async (e) => {
            e.stopPropagation();
            if (!arquivoSelecionado) {
                exibirMensagemUpload("Selecione um arquivo de imagem para enviar.", "erro");
                return;
            }
            if (totalImagensAtuais >= LIMITE_MAXIMO_IMAGENS) {
                exibirMensagemUpload("Limite de 10 imagens atingido. Remova uma foto antes.", "erro");
                return;
            }

            btnConfirmarUpload.disabled = true;
            try {
                const formData = new FormData();
                formData.append("arquivo", arquivoSelecionado);
                formData.append("categoria", selectCategoria ? selectCategoria.value : "Projetos");
                const publicar = $("publicar-imagem-site");
                formData.append("publicar", String(Boolean(publicar?.checked)));

                const headers = montarHeadersComCsrf();

                const resp = await fetch("/configuracoes/imagens", {
                    method: "POST",
                    credentials: "same-origin",
                    headers: headers,
                    body: formData
                });

                const dados = await resp.json().catch(() => ({}));
                if (!resp.ok) {
                    throw new Error(dados.mensagem || "Erro ao salvar imagem.");
                }

                if (publicar) publicar.checked = false;
                resetarUpload();
                exibirToast(dados.mensagem || "Imagem adicionada com sucesso!");
                await carregarGaleriaImagens();
            } catch (err) {
                exibirMensagemUpload(err.message || "Erro no envio da imagem.", "erro");
            } finally {
                btnConfirmarUpload.disabled = false;
            }
        });
    }

    carregarGaleriaImagens();

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
                const params = new URLSearchParams();
                params.append("chavePix", novoValor);

                const headers = montarHeadersComCsrf({
                    "Accept": "application/json",
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                });

                const resposta = await fetch(formEditarPix.action, {
                    method: "POST",
                    credentials: "same-origin",
                    headers: headers,
                    body: params
                });

                if (resposta.redirected || !resposta.headers.get("content-type")?.includes("application/json")) {
                    throw new Error("Sua sessão expirou ou a solicitação foi recusada. Recarregue a página e tente novamente.");
                }

                const dados = await resposta.json().catch(() => ({}));
                if (!resposta.ok) {
                    if (resposta.status === 403 && !dados.mensagem) {
                        throw new Error("Acesso negado (403). Verifique se sua conta é Administrador ou recarregue a página.");
                    }
                    throw new Error(dados.mensagem || "Não foi possível salvar a chave Pix.");
                }

                if (textoChavePix) {
                    textoChavePix.textContent = dados.chave;
                    textoChavePix.dataset.chave = dados.chave;
                }
                const mensagemPix = $("mensagem-pix");
                if (mensagemPix) mensagemPix.textContent = "Chave Pix cadastrada para a instituição.";
                fecharModalPix();
                exibirToast(dados.mensagem || "Chave Pix salva com sucesso!");
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

    let dadosEndereco = {
        logradouro: "Rua da Esperança",
        numero: "128",
        bairro: "Centro",
        cidade: "São Paulo",
        uf: "SP",
        cep: "01010-001"
    };

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

    // Documentos legais: mesmo conteúdo usado nas páginas públicas
    const legalModal = $("modal-documentos-legais");
    const legalFrame = $("legal-document-frame");
    const legalTitle = $("titulo-modal-documentos-legais");
    const legalOpenExternal = $("legal-open-new-tab");
    const legalClose = $("fechar-modal-documentos-legais");
    const legalUrls = {
        "/termos#termos-uso": "Termos de Uso",
        "/privacidade#politica-privacidade": "Política de Privacidade",
        "/termos-e-politicas#politica-cookies": "Política de Cookies"
    };
    let legalTrigger = null;
    function legalSelect(url) {
        if (!Object.hasOwn(legalUrls, url)) return;
        legalTitle.textContent = legalUrls[url];
        legalFrame.src = url;
        legalOpenExternal.href = url;
        document.querySelectorAll(".legal-tab").forEach(btn => {
            if (btn.dataset.legalUrl === url) btn.setAttribute("aria-current", "page");
            else btn.removeAttribute("aria-current");
        });
    }
    function fecharDocumentosLegais() {
        if (!legalModal || legalModal.hidden) return;
        legalModal.hidden = true;
        legalFrame.removeAttribute("src");
        document.body.style.overflow = "";
        legalTrigger?.focus();
    }
    document.querySelectorAll(".legal-open").forEach(button => {
        button.addEventListener("click", () => {
            legalTrigger = button;
            legalModal.hidden = false;
            document.body.style.overflow = "hidden";
            legalSelect(button.dataset.legalUrl);
            legalClose.focus();
        });
    });
    document.querySelectorAll(".legal-tab").forEach(button => {
        button.addEventListener("click", () => legalSelect(button.dataset.legalUrl));
    });
    legalClose?.addEventListener("click", fecharDocumentosLegais);
    legalModal?.addEventListener("click", (e) => {
        if (e.target === legalModal) fecharDocumentosLegais();
    });
    legalModal?.addEventListener("keydown", (e) => {
        if (e.key !== "Tab") return;
        const focusables = [...legalModal.querySelectorAll('button:not([disabled]), a[href]')];
        if (!focusables.length) return;
        if (e.shiftKey && document.activeElement === focusables[0]) {
            e.preventDefault(); focusables[focusables.length - 1].focus();
        } else if (!e.shiftKey && document.activeElement === focusables[focusables.length - 1]) {
            e.preventDefault(); focusables[0].focus();
        }
    });

    // Fechar modais ao pressionar tecla Escape
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            fecharDocumentosLegais();
            if (modalPix && !modalPix.hidden) fecharModalPix();
            if (modalEndereco && !modalEndereco.hidden) fecharModalEndereco();
            if (modalUsuario && !modalUsuario.hidden) fecharModalUsuario();
        }
    });
});
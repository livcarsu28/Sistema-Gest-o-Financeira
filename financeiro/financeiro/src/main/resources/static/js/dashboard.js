const botaoCopiar = document.getElementById("botao-copiar");
const chavePix = document.getElementById("chave-pix");
const statusCopia = document.getElementById("status-copia");
let temporizador;

botaoCopiar.addEventListener("click", async () => {
    clearTimeout(temporizador);

    try {
        await navigator.clipboard.writeText(chavePix.textContent.trim());
        botaoCopiar.textContent = "Copiada";
        botaoCopiar.classList.add("botao-copiar-sucesso");
        statusCopia.textContent = "Chave Pix copiada.";
    } catch {
        statusCopia.textContent = "Não foi possível copiar. Selecione a chave e copie manualmente.";
    }

    temporizador = setTimeout(() => {
        botaoCopiar.textContent = "Copiar";
        botaoCopiar.classList.remove("botao-copiar-sucesso");
        statusCopia.textContent = "";
    }, 3000);
});

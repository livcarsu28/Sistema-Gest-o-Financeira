const botaoCopiar = document.getElementById("botao-copiar");
const chavePix = document.getElementById("chave-pix");
const statusCopia = document.getElementById("status-copia");
const rotuloCopiar = document.getElementById("rotulo-copiar");
let temporizador;

if (botaoCopiar && chavePix && statusCopia && rotuloCopiar) {
    botaoCopiar.addEventListener("click", async () => {
        const chave = chavePix.dataset.chave?.trim();
        if (botaoCopiar.disabled || !chave) return;

        clearTimeout(temporizador);
        try {
            await navigator.clipboard.writeText(chave);
            rotuloCopiar.textContent = "Chave copiada";
            statusCopia.textContent = "Chave Pix copiada.";
        } catch {
            statusCopia.textContent = "Não foi possível copiar. Selecione a chave e copie manualmente.";
        }

        temporizador = setTimeout(() => {
            rotuloCopiar.textContent = "Copiar chave Pix";
            statusCopia.textContent = "";
        }, 3000);
    });
}

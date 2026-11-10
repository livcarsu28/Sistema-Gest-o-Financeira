package financeiro.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.InstituicaoRepository;

@Service
public class PixService {

    private final InstituicaoRepository repository;

    public PixService(InstituicaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DadosPix consultar() {
        var instituicoes = repository.consultar(false);
        if (instituicoes.isEmpty()) {
            return new DadosPix(null, "O cadastro da instituição ainda não está disponível.");
        }
        if (instituicoes.size() != 1) {
            return new DadosPix(null, "Há mais de uma instituição cadastrada. Solicite a conferência do cadastro.");
        }
        String chave = instituicoes.get(0).chavePix();
        if (chave == null || chave.isBlank()) {
            return new DadosPix(null, "Nenhuma chave Pix cadastrada. Cadastre em Dados da ONG.");
        }
        if (!chaveValida(chave.trim())) {
            return new DadosPix(null, "Confira e atualize a chave Pix em Dados da ONG.");
        }
        return new DadosPix(chave.trim(), "Chave Pix cadastrada para a instituição.");
    }

    @Transactional
    public DadosPix salvar(String valor) {
        String chave = valor == null ? "" : valor.trim();
        if (!chaveValida(chave)) {
            throw new IllegalArgumentException("Informe uma chave Pix válida: CPF, CNPJ, e-mail, telefone com DDI ou chave aleatória.");
        }
        var instituicoes = repository.consultar(true);
        if (instituicoes.size() != 1) {
            throw new IllegalStateException(instituicoes.isEmpty()
                    ? "Cadastre a instituição antes de salvar a chave Pix."
                    : "Há mais de uma instituição cadastrada. Solicite a conferência do cadastro.");
        }
        if (repository.atualizarChavePix(instituicoes.get(0).id(), chave) != 1) {
            throw new IllegalStateException("O cadastro da instituição foi alterado. Recarregue a página.");
        }
        return new DadosPix(chave, "Chave Pix atualizada com sucesso!");
    }

    private boolean chaveValida(String chave) {
        if (chave.length() > 100 || chave.isBlank()) {
            return false;
        }
        if (chave.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+")
                || chave.matches("\\+[1-9][0-9]{7,14}")
                || chave.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            return true;
        }
        if (!chave.matches("[0-9]{11}|[0-9]{14}") || chave.chars().distinct().count() == 1) {
            return false;
        }
        return digitoValido(chave, chave.length() - 2) && digitoValido(chave, chave.length() - 1);
    }

    private boolean digitoValido(String chave, int posicao) {
        int soma = 0;
        int peso = 2;
        for (int i = posicao - 1; i >= 0; i--) {
            soma += (chave.charAt(i) - '0') * peso++;
            if (chave.length() == 14 && peso > 9) {
                peso = 2;
            }
        }
        int resto = soma % 11;
        return chave.charAt(posicao) - '0' == (resto < 2 ? 0 : 11 - resto);
    }

    public record DadosPix(String chave, String mensagem) {
        public boolean disponivel() {
            return chave != null;
        }
    }
}

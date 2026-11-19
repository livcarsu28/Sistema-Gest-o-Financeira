package financeiro.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import financeiro.repository.CadastrosRepository;
import financeiro.repository.CadastrosRepository.CategoriaMovimentacao;
import financeiro.repository.CadastrosRepository.FormaPagamento;

/**
 * Serviço de leitura para os cadastros-base de movimentações.
 *
 * Encapsula o acesso ao CadastrosRepository e define a semântica de negócio:
 * somente registros ativos são consultados, pois categorias e formas de pagamento
 * inativas não devem ser oferecidas como opção em novos lançamentos.
 */
@Service
public class CadastrosService {

    private final CadastrosRepository repository;

    public CadastrosService(CadastrosRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaMovimentacao> listarCategorias() {
        return repository.listarCategorias();
    }

    @Transactional(readOnly = true)
    public List<FormaPagamento> listarFormasPagamento() {
        return repository.listarFormasPagamento();
    }
}

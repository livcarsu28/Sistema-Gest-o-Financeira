package financeiro.repository;
import financeiro.model.FormaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FormaPagamentoRepository 
extends JpaRepository<FormaPagamento,Integer>{
    List<FormaPagamento> findByAtivoTrueOrderByNomeAsc();
}
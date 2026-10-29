package financeiro.repository;
import financeiro.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoRepository 
    extends JpaRepository<Movimentacao,Integer>{
    List<Movimentacao> findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(LocalDateTime inicio,LocalDateTime fim);
    List<Movimentacao> findTop5ByOrderByDataDesc();
}
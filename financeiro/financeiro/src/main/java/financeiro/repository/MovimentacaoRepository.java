package financeiro.repository;
import financeiro.model.Movimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface MovimentacaoRepository 
    extends JpaRepository<Movimentacao,Integer>{
    List<Movimentacao> findByExcluidaFalseOrderByDataDescIdDesc();
    List<Movimentacao> findByExcluidaFalseAndDataGreaterThanEqualAndDataLessThanOrderByDataDesc(
        LocalDateTime inicio, LocalDateTime fim);
    List<Movimentacao> findTop5ByExcluidaFalseAndDataLessThanOrderByDataDescIdDesc(LocalDateTime limite);

    @Query("select m.serie from Movimentacao m where m.id = :id")
    String serieDe(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Movimentacao findFirstBySerieOrderByOrdemSerieDesc(String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movimentacao m where m.serie = :serie and m.ordemSerie = 1")
    Optional<Movimentacao> bloquearSerie(@Param("serie") String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movimentacao m where m.serie = :serie order by m.ordemSerie")
    List<Movimentacao> bloquearOcorrencias(@Param("serie") String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Movimentacao m where m.id = :id")
    Optional<Movimentacao> bloquear(@Param("id") Integer id);

    @Query("select m.serie from Movimentacao m where m.ordemSerie = 1 and m.recorrente = true and m.serieEncerrada = false order by m.serie")
    List<String> seriesAtivas();
}

package financeiro.service;
import financeiro.dto.MovimentacaoForm;
import financeiro.model.Movimentacao;
import financeiro.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.List;

@Service public class MovimentacaoService {
private final MovimentacaoRepository repo;
private final CategoriaRepository categorias;
private final FormaPagamentoRepository formas;

@Value("${app.dev-user-id:0}") 
private Integer devUserId;
public MovimentacaoService(MovimentacaoRepository r,CategoriaRepository c,FormaPagamentoRepository f){
    repo=r;categorias=c;formas=f;
}

public List<Movimentacao> mes(YearMonth mes){
    return repo.findByDataGreaterThanEqualAndDataLessThanOrderByDataDesc(mes.atDay(1).atStartOfDay(),mes.plusMonths(1).atDay(1).atStartOfDay());
}

public List<Movimentacao> recentes(){
    return repo.findTop5ByOrderByDataDesc();
}

public void salvar(MovimentacaoForm f){
    if(devUserId==null||devUserId<=0)
        throw new IllegalStateException("Configure DEV_USER_ID com um id_usuario existente antes de cadastrar.");
    
    if(!categorias.existsById(f.categoriaId))
        throw new IllegalArgumentException("Categoria inválida");

    if(f.formaPagamentoId!=null&&!formas.existsById(f.formaPagamentoId))
        throw new IllegalArgumentException("Forma de pagamento inválida");
    Movimentacao m=new Movimentacao();
    m.tipo=f.tipo;
    m.valor=f.valor;
    m.descricao=f.descricao.trim();
    m.data=f.data.atStartOfDay();
    m.categoriaId=f.categoriaId;
    m.formaPagamentoId=f.formaPagamentoId;
    m.usuarioId=devUserId;

    m.recorrente = f.recorrente;

    if (f.recorrente) {

        if ("MENSAL".equals(f.tipoRecorrencia)) {

            if (f.diaMes == null || f.diaMes < 1 || f.diaMes > 31) {
                throw new IllegalArgumentException(
                    "Selecione um dia válido do mês."
                );
            }

            m.tipoRecorrencia = "MENSAL";
            m.diaMes = f.diaMes;
            m.diasSemana = null;

        } else if ("SEMANAL".equals(f.tipoRecorrencia)) {

            if (f.diasSemana == null || f.diasSemana.isEmpty()) {
                throw new IllegalArgumentException(
                    "Selecione pelo menos um dia da semana."
                );
            }

            m.tipoRecorrencia = "SEMANAL";
            m.diaMes = null;

            m.diasSemana = f.diasSemana.stream()
                .distinct()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.joining(","));

        } else {
            throw new IllegalArgumentException(
                "Selecione o tipo de recorrência."
            );
        }

    } else {
        m.tipoRecorrencia = null;
        m.diaMes = null;
        m.diasSemana = null;
    }


    repo.save(m);

    }
}
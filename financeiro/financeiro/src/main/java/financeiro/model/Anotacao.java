package financeiro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anotacao")
public class Anotacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anotacao")
    public Integer id;

    @Column(nullable = false)
    public String descricao;

    @Column(name = "data_criacao", nullable = false)
    public LocalDateTime dataCriacao;

    @Column(name = "data_lembrete")
    public LocalDateTime dataLembrete;

    @Column(name = "id_categoria")
    public Integer categoriaId;

    @Column(name = "id_usuario", nullable = false)
    public Integer usuarioId;

    public boolean concluido;
}

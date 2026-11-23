package financeiro.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.time.LocalDate;
@Entity @Table(name="movimentacao") public class Movimentacao {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_movimentacao") public Integer id;
@Column(name="data_movimentacao",nullable=false) public LocalDateTime data;
@Column(nullable=false,length=10) public String tipo;
@Column(nullable=false,precision=12,scale=2) public BigDecimal valor;
@Column(length=500) public String descricao;
@Column(length=30) public String conta;
@Column(length=30) public String observacao;
public Boolean recorrente=false;
@Column(name="numero_parcelas") public Integer numeroParcelas=1;
@Column(name="parcela_atual") public Integer parcelaAtual=1;
@Column(name="status_pagamento") public String statusPagamento="PAGO";
@Column(name="data_vencimento") public LocalDate dataVencimento;
@Column(name="data_pagamento") public LocalDateTime dataPagamento;
@Column(name="id_categoria",nullable=false) public Integer categoriaId;
@Column(name="id_forma_pagamento") public Integer formaPagamentoId;
@Column(name="id_usuario",nullable=false) public Integer usuarioId;
@Column(name="id_movimentacao_origem") public Integer origemId;
@Column(name = "tipo_recorrencia", length = 10)
public String tipoRecorrencia;

@Column(name = "dia_mes")
public Integer diaMes;

@Column(name = "dias_semana", length = 100)
public String diasSemana;

@Column(length = 36)
public String serie;

@Column(name = "ordem_serie")
public Integer ordemSerie;

@Column(name = "data_programada")
public LocalDate dataProgramada;

public boolean excluida;

@Column(name = "serie_encerrada")
public boolean serieEncerrada;
}
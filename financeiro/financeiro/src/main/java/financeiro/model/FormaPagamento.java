package financeiro.model;
import jakarta.persistence.*;
@Entity @Table(name="forma_pagamento") public class FormaPagamento {
@Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_forma_pagamento") public Integer id;
public String nome;
public Boolean ativo;
}
package financeiro.model;
import jakarta.persistence.*;

    @Entity 
    @Table(name="categoria_movimentacao") 
    public class Categoria {
        
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY) 

    @Column(name="id_categoria") 
    public Integer id;
    public String nome;
    public Boolean ativo;
    @Column(length = 7)
    public String cor;
}
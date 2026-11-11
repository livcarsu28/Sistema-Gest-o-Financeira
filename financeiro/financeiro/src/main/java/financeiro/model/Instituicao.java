package financeiro.model; // Ajuste o pacote se o seu for diferente (ex: com.example.financeiro.model)

import jakarta.persistence.*;

@Entity
@Table(name = "instituicao")
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instituicao")
    private Integer idInstituicao;

    @Column(name = "chave_pix", length = 100)
    private String chavePix;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 50)
    private String bairro;

    @Column(length = 50)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 9)
    private String cep;

    @Column(name = "qr_code_pix", length = 255)
    private String qrCodePix;

    // Construtor vazio obrigatório para o JPA
    public Instituicao() {
    }

    // Getters e Setters
    public Integer getIdInstituicao() { return idInstituicao; }
    public void setIdInstituicao(Integer idInstituicao) { this.idInstituicao = idInstituicao; }

    public String getChavePix() { return chavePix; }
    public void setChavePix(String chavePix) { this.chavePix = chavePix; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getQrCodePix() { return qrCodePix; }
    public void setQrCodePix(String qrCodePix) { this.qrCodePix = qrCodePix; }
}
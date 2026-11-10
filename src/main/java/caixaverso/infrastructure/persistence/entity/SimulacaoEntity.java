package caixaverso.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import caixaverso.infrastructure.cache.CacheEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@EntityListeners(CacheEntityListener.class)
@Table(name = "simulacao")
public class SimulacaoEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clienteId;

    @Column(name = "produto_nome")
    private String produto;

    @Column(name = "valor_investido", precision = 18, scale = 2)
    private BigDecimal valorInvestido;

    @Column(name = "valor_final", precision = 18, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "rentabilidade_efetiva", precision = 5, scale = 2)
    private BigDecimal rentabilidadeEfetiva;

    @Column(name = "prazo_meses")
    private Integer prazoMeses;

    private Instant dataSimulacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getProduto() {
        return produto;
    }

    public void setProduto(String produto) {
        this.produto = produto;
    }

    public BigDecimal getValorInvestido() {
        return valorInvestido;
    }

    public void setValorInvestido(BigDecimal valorInvestido) {
        this.valorInvestido = valorInvestido;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public void setValorFinal(BigDecimal valorFinal) {
        this.valorFinal = valorFinal;
    }

    public BigDecimal getRentabilidadeEfetiva() { return rentabilidadeEfetiva; }

    public void setRentabilidadeEfetiva(BigDecimal rentabilidadeEfetiva) {
        this.rentabilidadeEfetiva = rentabilidadeEfetiva; }

    public Integer getPrazoMeses() {
        return prazoMeses;
    }

    public void setPrazoMeses(Integer prazoMeses) {
        this.prazoMeses = prazoMeses;
    }

    public Instant getDataSimulacao() {
        return dataSimulacao;
    }

    public void setDataSimulacao(Instant dataSimulacao) {
        this.dataSimulacao = dataSimulacao;
    }

    public SimulacaoEntity(Long id, Long clienteId, String produto, BigDecimal valorInvestido,
                           BigDecimal valorFinal, BigDecimal rentabilidadeEfetiva, Integer prazoMeses,
                           Instant dataSimulacao) {
        this.id = id;
        this.clienteId = clienteId;
        this.produto = produto;
        this.valorInvestido = valorInvestido;
        this.valorFinal = valorFinal;
        this.rentabilidadeEfetiva = rentabilidadeEfetiva;
        this.prazoMeses = prazoMeses;
        this.dataSimulacao = dataSimulacao;
    }

    public SimulacaoEntity() {
    }
}

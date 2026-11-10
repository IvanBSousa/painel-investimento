package caixaverso.infrastructure.persistence.entity;

import jakarta.persistence.*;
import caixaverso.infrastructure.cache.CacheEntityListener;
import jakarta.validation.constraints.Positive;
import jdk.jfr.Timestamp;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@EntityListeners(CacheEntityListener.class)
@Table(name = "investimento")
public class InvestimentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    private String tipo;

    @Positive
    private BigDecimal valor;

    @Positive
    private BigDecimal rentabilidade;

    @Timestamp
    private LocalDate data;

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getRentabilidade() {
        return rentabilidade;
    }

    public void setRentabilidade(BigDecimal rentabilidade) {
        this.rentabilidade = rentabilidade;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public InvestimentoEntity(Long id, Long clienteId, String tipo, BigDecimal valor, BigDecimal rentabilidade, LocalDate data) {
        this.id = id;
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.valor = valor;
        this.rentabilidade = rentabilidade;
        this.data = data;
    }

    public InvestimentoEntity() {
    }
}

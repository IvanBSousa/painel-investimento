package caixaverso.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import caixaverso.infrastructure.cache.CacheEntityListener;

import java.time.LocalDate;

@Entity
@EntityListeners(CacheEntityListener.class)
@Table(name = "telemetria")
public class TelemetriaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeServico;

    @Column(name = "data_da_chamada")
    private LocalDate dataChamada;

    @Column(name = "tempo_resposta_ms")
    private Long tempoRespostaMs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeServico() {
        return nomeServico;
    }

    public void setNomeServico(String nomeServico) {
        this.nomeServico = nomeServico;
    }

    public LocalDate getDataChamada() {
        return dataChamada;
    }

    public void setDataChamada(LocalDate dataChamada) {
        this.dataChamada = dataChamada;
    }

    public Long getTempoRespostaMs() {
        return tempoRespostaMs;
    }

    public void setTempoRespostaMs(Long tempoRespostaMs) {
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public TelemetriaEntity(Long id, String nomeServico, LocalDate dataChamada, Long tempoRespostaMs) {
        this.id = id;
        this.nomeServico = nomeServico;
        this.dataChamada = dataChamada;
        this.tempoRespostaMs = tempoRespostaMs;
    }

    public TelemetriaEntity() {
    }
}
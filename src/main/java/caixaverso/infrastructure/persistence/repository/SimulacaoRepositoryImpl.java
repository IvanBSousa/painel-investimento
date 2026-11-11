package caixaverso.infrastructure.persistence.repository;

import caixaverso.application.dto.SimulacaoAgrupadaResponseDTO;
import caixaverso.domain.repository.SimulacaoRepository;
import caixaverso.infrastructure.persistence.entity.SimulacaoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SimulacaoRepositoryImpl implements SimulacaoRepository {

    @Inject
    EntityManager em;

    @Override
    public void salvar(SimulacaoEntity simulacao) {
        if (simulacao.getId() == null) {
            em.persist(simulacao);
            return;
        }
        em.merge(simulacao);
    }

    @Override
    public List<SimulacaoEntity> listarPaginado(int page, int size) {
        return em.createQuery("SELECT s FROM SimulacaoEntity s ORDER BY s.dataSimulacao DESC", SimulacaoEntity.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public List<SimulacaoAgrupadaResponseDTO> agruparPorProdutoDia() {
        String jpql = """
            SELECT s.produto, CAST(s.dataSimulacao AS java.time.LocalDate), COUNT(s), SUM(s.valorInvestido), SUM(s.valorFinal)
            FROM SimulacaoEntity s
            GROUP BY CAST(s.dataSimulacao AS java.time.LocalDate), s.produto
            ORDER BY CAST(s.dataSimulacao AS java.time.LocalDate) DESC
            """;

        Query q = em.createQuery(jpql);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        List<SimulacaoAgrupadaResponseDTO> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Object produtoObj = row[0];
            Object dateObj = row[1];
            Object countObj = row[2];
            Object sumFinalObj = row[4];

            String produto = produtoObj == null ? null : produtoObj.toString();

            LocalDate data;
            if (dateObj instanceof java.time.LocalDate) {
                data = (LocalDate) dateObj;
            } else if (dateObj instanceof Date) {
                data = ((Date) dateObj).toLocalDate();
            } else {
                data = null;
            }

            Long quantidade = countObj == null ? 0L : ((Number) countObj).longValue();

            BigDecimal somaFinal = toBigDecimal(sumFinalObj);
            BigDecimal mediaValorFinal = quantidade == 0 ? BigDecimal.ZERO :
                            somaFinal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);


            result.add(new SimulacaoAgrupadaResponseDTO(produto, data, quantidade, mediaValorFinal));
        }

        return result;
    }



    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return new BigDecimal(obj.toString());
        return BigDecimal.ZERO;
    }
}

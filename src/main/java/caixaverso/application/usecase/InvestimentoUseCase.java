package caixaverso.application.usecase;

import caixaverso.application.dto.InvestimentoDTO;
import caixaverso.infrastructure.mapper.InvestimentoMapper;
import caixaverso.infrastructure.persistence.repository.InvestimentoRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.cache.CacheResult;

import java.util.List;

@ApplicationScoped
public class InvestimentoUseCase {

    private final InvestimentoRepositoryImpl investimentoRepository;
    private final InvestimentoMapper investimentoMapper;

    public InvestimentoUseCase(InvestimentoRepositoryImpl investimentoRepository,
                               InvestimentoMapper investimentoMapper) {
        this.investimentoRepository = investimentoRepository;
        this.investimentoMapper = investimentoMapper;
    }

    @CacheResult(cacheName = "investimentos")
    public List<InvestimentoDTO> listarPorCliente(Long clienteId) {
        return investimentoRepository.listarPorCliente(clienteId)
                .stream()
                .map(investimentoMapper::entityToModel)
                .map(investimentoMapper::modeltoDTO)
                .toList();
    }
}

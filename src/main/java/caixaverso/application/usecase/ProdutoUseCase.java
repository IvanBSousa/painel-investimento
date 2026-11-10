package caixaverso.application.usecase;

import caixaverso.application.dto.ProdutoDTO;
import caixaverso.domain.enums.PerfilRiscoEnum;
import caixaverso.infrastructure.mapper.ProdutoMapper;
import caixaverso.infrastructure.persistence.entity.ProdutoEntity;
import caixaverso.infrastructure.persistence.repository.ProdutoRepositoryImpl;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.cache.CacheResult;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ProdutoUseCase {

    private final ProdutoRepositoryImpl produtoRepository;
    private final ProdutoMapper produtoMapper;

    public ProdutoUseCase(ProdutoRepositoryImpl produtoRepository, ProdutoMapper produtoMapper) {
        this.produtoRepository = produtoRepository;
        this.produtoMapper = produtoMapper;
    }

    public List<ProdutoEntity> listar() {
        return produtoRepository.listAllProdutos();
    }

    public ProdutoEntity buscarPorId(Integer id) {
        return produtoRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }

    public ProdutoEntity criarProduto(ProdutoEntity entity) {
        produtoRepository.persistProduto(entity);
        return entity;
    }

    public ProdutoEntity atualizar(Integer id, ProdutoEntity novo) {
        ProdutoEntity atual = buscarPorId(id);
        atual.setNome(novo.getNome());
        atual.setTipo(novo.getTipo());

        return atual;
    }

    public void deletar(Integer id) {
        ProdutoEntity produto = buscarPorId(id);
        produtoRepository.delete(produto);
    }

    public List<ProdutoEntity> sugerirProdutos(BigDecimal valor, int prazoMeses, String tipoProduto) {
        return produtoRepository.findValidByParams(valor, prazoMeses, tipoProduto);
    }

    @CacheResult(cacheName = "produtos-recomendados")
    public List<ProdutoDTO> buscarProdutosPorPerfil(PerfilRiscoEnum perfil) {
        return produtoRepository.buscarPorPerfil(perfil)
                .stream()
                .map(produtoMapper::toDTO)
                .toList();
    }
}

package br.com.lunacom.ivmmonitorprocessamento.repository;

import br.com.lunacom.comum.domain.MovimentoVenda;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentoVendaRepository extends JpaRepository<MovimentoVenda, Integer>, JpaSpecificationExecutor<MovimentoVenda> {

    @Query("SELECT m FROM MovimentoVenda m " +
            "WHERE m.ativo.codigo = :codigo " +
            "AND (:excluirPrejuizos = 'N' OR m.precoPago < m.precoVenda)" +
            "AND (:dataInicial IS NULL OR m.dataVenda > :dataInicial)" +
            "ORDER BY m.dataVenda DESC")
    List<MovimentoVenda> buscarUltimasCincoVendas(
            @Param("codigo") String codigo,
            @Param("dataInicial") LocalDate dataInicial,
            @Param("excluirPrejuizos") String excluirPrejuizos,
            Pageable pageable);

    Optional<MovimentoVenda> findTopByAtivoCodigoOrderByDataVendaDesc(String ativoCodigo);

}

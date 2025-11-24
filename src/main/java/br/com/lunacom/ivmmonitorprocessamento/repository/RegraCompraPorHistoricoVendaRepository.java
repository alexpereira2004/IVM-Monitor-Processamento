package br.com.lunacom.ivmmonitorprocessamento.repository;

import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.comum.domain.enumeration.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegraCompraPorHistoricoVendaRepository
        extends JpaRepository<RegraCompraPorHistoricoVenda, Integer> {

    @Override
    List<RegraCompraPorHistoricoVenda> findAll();

    @Query(" SELECT r " +
            "  FROM RegraCompraPorHistoricoVenda r " +
            " WHERE r.status = :status " +
            "   AND :validade IS NULL OR r.validade >= :validade ")
    List<RegraCompraPorHistoricoVenda> findByStatusAndValidade(
            Status status,
            LocalDate validade);
}

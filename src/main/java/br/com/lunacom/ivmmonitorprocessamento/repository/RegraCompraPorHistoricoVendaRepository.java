package br.com.lunacom.ivmmonitorprocessamento.repository;

import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegraCompraPorHistoricoVendaRepository
        extends JpaRepository<RegraCompraPorHistoricoVenda, Integer> {

    @Override
    List<RegraCompraPorHistoricoVenda> findAll();
}

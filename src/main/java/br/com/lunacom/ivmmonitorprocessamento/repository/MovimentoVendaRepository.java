package br.com.lunacom.ivmmonitorprocessamento.repository;

import br.com.lunacom.comum.domain.MovimentoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentoVendaRepository extends JpaRepository<MovimentoVenda, Integer>, JpaSpecificationExecutor<MovimentoVenda> {

    List<MovimentoVenda> findAllByAtivoCodigoAndDataVendaAfter(String ativoCodigo, LocalDate dataInicial);

    List<MovimentoVenda> findAllByAtivoCodigo(String ativoCodigo);

    Optional<MovimentoVenda> findTopByAtivoCodigoOrderByDataVendaDesc(String ativoCodigo);

}

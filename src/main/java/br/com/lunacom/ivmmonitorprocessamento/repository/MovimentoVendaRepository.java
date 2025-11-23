package br.com.lunacom.ivmmonitorprocessamento.repository;

import br.com.lunacom.comum.domain.MovimentoVenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentoVendaRepository extends JpaRepository<MovimentoVenda, Integer>, JpaSpecificationExecutor<MovimentoVenda> {

    List<MovimentoVenda> findAllByAtivoCodigo(String codigo);
}

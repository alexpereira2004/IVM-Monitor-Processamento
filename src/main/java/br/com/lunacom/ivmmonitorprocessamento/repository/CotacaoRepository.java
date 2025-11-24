package br.com.lunacom.ivmmonitorprocessamento.repository;


import br.com.lunacom.comum.domain.Cotacao;
import br.com.lunacom.comum.domain.dto.CotacaoAgoraDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CotacaoRepository extends JpaRepository<Cotacao, Integer> {

    @Query(nativeQuery = true)
    List<CotacaoAgoraDto> pesquisarCotacaoAgora();

}

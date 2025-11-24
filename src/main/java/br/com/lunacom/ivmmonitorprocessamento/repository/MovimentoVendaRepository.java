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



    // Método que busca por Código do Ativo E data maior que a data inicial fornecida
    List<MovimentoVenda> findAllByAtivoCodigoAndDataVendaAfter(String ativoCodigo, LocalDate dataInicial);

    // Método que busca APENAS por Código do Ativo (usado em casos que não há filtro temporal)
    List<MovimentoVenda> findAllByAtivoCodigo(String ativoCodigo);

    // --- NOVAS CONSULTAS DIRECIONADAS ---

    // 1. Consulta para ULTIMA_VENDA
    // Busca a venda mais recente para um ativo, ordenando pela data de venda
    // e limitando o resultado a 1 (o Spring Data JPA faz o LIMIT 1 automaticamente).
    Optional<MovimentoVenda> findTopByAtivoCodigoOrderByDataVendaDesc(String ativoCodigo);

    // 2. Consulta para VENDA_ESPECIFICA (assumindo que o ID da venda específica está na Regra)
    // Se o ID da venda específica estiver em um campo da Regra (ex: regra.getMovimentoVendaId()),
    // podemos usar a função padrão findById. Se o código da venda for uma String, use:
//    List<MovimentoVenda> findByCodigo(String codigoVenda);
}

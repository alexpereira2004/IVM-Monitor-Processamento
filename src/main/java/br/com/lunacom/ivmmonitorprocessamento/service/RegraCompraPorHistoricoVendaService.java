package br.com.lunacom.ivmmonitorprocessamento.service;

import br.com.lunacom.comum.domain.Ativo;
import br.com.lunacom.comum.domain.MovimentoVenda;
import br.com.lunacom.comum.domain.dto.CotacaoAgoraDto;
import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.comum.domain.enumeration.Status;
import br.com.lunacom.ivmmonitorprocessamento.repository.CotacaoRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.MovimentoVendaRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.RegraCompraPorHistoricoVendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegraCompraPorHistoricoVendaService {

    public static final String SEM_COTACAO = "Não foi encontrada a cotação para o Ativo %s ao tentar calcular a recomendação";
    private final RegraCompraPorHistoricoVendaRepository repository;
    private final CotacaoRepository cotacaoRepository;
    private final MovimentoVendaRepository movimentoVendaRepository;

    public void processar(String request) throws Exception {

        final List<CotacaoAgoraDto> cotacaoAgoraDtoList = this.buscarCotacaoAgora();

//        this.buscarRegra();

        final List<RegraCompraPorHistoricoVenda> regraList = this
                .buscarMovimentoVendaPassado();

        regraList.forEach( regra -> {
            final List<MovimentoVenda> movimentoVendas = this.buscarVendasPassadas(regra);

            this.calcularRecomendacao(cotacaoAgoraDtoList, movimentoVendas);

            this.salvarRecomendacao();
        });
    }

    public List<MovimentoVenda> buscarVendasPassadas(RegraCompraPorHistoricoVenda regra) {
        String codigoAtivo = regra.getMonitor().getAtivo().getCodigo();
        return switch (regra.getPeriodo()) {

            case ULTIMA_VENDA -> {
                Optional<MovimentoVenda> ultimaVenda = movimentoVendaRepository
                        .findTopByAtivoCodigoOrderByDataVendaDesc(codigoAtivo);
                yield ultimaVenda.map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            }

            case VENDA_ESPECIFICA -> {
                Optional<MovimentoVenda> venda = movimentoVendaRepository
                         .findById(regra.getMovimentoVenda().getId());
                yield venda.map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            }

            default -> {
                Optional<LocalDate> dataFiltroOpcional = switch (regra.getPeriodo()) {
                    case ANO_ATUAL -> Optional.of(LocalDate.now().withDayOfYear(1).minusDays(1));
                    case ULTIMOS_12_MESES -> Optional.of(LocalDate.now().minusYears(1));
                    default -> Optional.empty();
                };
                List<MovimentoVenda> vendas = dataFiltroOpcional
                        .map(dataInicial -> movimentoVendaRepository
                                .findAllByAtivoCodigoAndDataVendaAfter(codigoAtivo, dataInicial)
                ).orElseGet(() -> movimentoVendaRepository
                                .findAllByAtivoCodigo(codigoAtivo)
                );
                yield vendas;
            }
        };

    }

    private List<CotacaoAgoraDto> buscarCotacaoAgora() {
        return cotacaoRepository.pesquisarCotacaoAgora();
    }

    private void buscarRegra() {
    }

    private List<RegraCompraPorHistoricoVenda> buscarMovimentoVendaPassado() {
        final List<RegraCompraPorHistoricoVenda> all = repository.findByStatusAndValidade(
                Status.ATIVO, null
        );

        return all;
//        Monitor monitorProxy = all.get(13).getMonitor();
//
//        final String s = monitorProxy.getAtivo().toString();
//
//        final List<MovimentoVenda> allByAtivoCodigo = movimentoVendaRepository.findAllByAtivoCodigo(monitorProxy.getAtivo().getCodigo());
//
//        log.info(all.get(0).getMonitor().getAtivo().toString());
    }

    private void calcularRecomendacao(
            List<CotacaoAgoraDto> cotacaoAgoraDtoList,
            List<MovimentoVenda> movimentoVendas) {

        final Ativo ativo = movimentoVendas.get(0).getAtivo();
        final CotacaoAgoraDto cotacao = cotacaoAgoraDtoList.stream()
                .filter(i -> i.getCodigo().equals(ativo.getCodigo()))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(String.format(SEM_COTACAO, ativo.getCodigo())));

        switch (movimentoVendas.size()) {
            case 1:
                System.out.println("1");
                break;
            case 2:
                System.out.println("2");
                break;
            case 3:
                System.out.println("3");
                break;
            case 4:
                System.out.println("4");
                break;
            case 5:
                System.out.println("5");
                break;
            default:
                System.out.println("Mais que 5");
                break;
        }




    }

    private void salvarRecomendacao() {

    }
}

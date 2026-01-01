package br.com.lunacom.ivmmonitorprocessamento.service;

import br.com.lunacom.comum.domain.Ativo;
import br.com.lunacom.comum.domain.MovimentoVenda;
import br.com.lunacom.comum.domain.dto.CotacaoAgoraDto;
import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.comum.domain.enumeration.Status;
import br.com.lunacom.ivmmonitorprocessamento.domain.EscalaRecomendacao;
import br.com.lunacom.ivmmonitorprocessamento.domain.Recomendacao;
import br.com.lunacom.ivmmonitorprocessamento.repository.CotacaoRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.MovimentoVendaRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.RegraCompraPorHistoricoVendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public static final String SEM_VENDAS = "Nenhum movimento de venda fornecido para cálculo. Esse problema aconteceu para a regra %";
    public static final String LOG_CALCULAR_RECOMENDACAO_01 = "Processando recomendação para {} (Volume: {} vendas)";
    public static final String LOG_EXECUTANDO_REGRA = "Executando regra específica para {} venda de {}";
    private final RegraCompraPorHistoricoVendaRepository repository;
    private final CotacaoRepository cotacaoRepository;
    private final MovimentoVendaRepository movimentoVendaRepository;

    private record AtributosParaCalculoRecomendacaoContext(
            Ativo ativo,
            CotacaoAgoraDto cotacao,
            List<MovimentoVenda> vendas
    ) {}

    private record RecomendacaoFinalContext(
            Recomendacao recomendacao,
            EscalaRecomendacao escalaRecomendacao,
            String analise,
            String observacao
    ) {}

    public void processar(String request) throws Exception {

        final List<CotacaoAgoraDto> cotacaoAgoraDtoList = this.buscarCotacaoAgora();

//        this.buscarRegra();

        final List<RegraCompraPorHistoricoVenda> regraList = this.buscarRegrasAtivas();

        regraList.forEach( regra -> {
            final List<MovimentoVenda> movimentoVendas = this.buscarVendasPassadas(regra);

            this.calcularRecomendacao(regra, cotacaoAgoraDtoList, movimentoVendas);

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

    private List<RegraCompraPorHistoricoVenda> buscarRegrasAtivas() {
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
            RegraCompraPorHistoricoVenda regra, List<CotacaoAgoraDto> cotacaoAgoraDtoList,
            List<MovimentoVenda> movimentoVendas)
    {

        if (movimentoVendas == null || movimentoVendas.isEmpty()) {
            log.warn(String.format(SEM_VENDAS, regra.getId()));
            return;
        }

        final Ativo ativo = encontrarAtivo(movimentoVendas);

        final CotacaoAgoraDto cotacao = buscarCotacao(cotacaoAgoraDtoList, ativo);

        final AtributosParaCalculoRecomendacaoContext contexto = new AtributosParaCalculoRecomendacaoContext(ativo, cotacao, movimentoVendas);

        log.info(LOG_CALCULAR_RECOMENDACAO_01, ativo.getCodigo(), movimentoVendas.size());

        switch (movimentoVendas.size()) {
            case 1 -> this.calcularRecomendacaoParaUmaVenda(contexto);
            case 2 -> this.calcularRecomendacaoParaDuasVendas(contexto);
            case 3 -> this.calcularRecomendacaoParaTresVendas(contexto);
            case 4 -> this.calcularRecomendacaoParaQuatroVendas(contexto);
            case 5 -> this.calcularRecomendacaoParaCincoVendas(contexto);
            default -> this.calcularRecomendacaoParaMaisDeCincoVendas(contexto);
        }
    }

    private Ativo encontrarAtivo(List<MovimentoVenda> movimentoVendas) {
        return movimentoVendas.get(0).getAtivo();
    }

    private CotacaoAgoraDto buscarCotacao(List<CotacaoAgoraDto> cotacaoAgoraDtoList, Ativo ativo) {
        return cotacaoAgoraDtoList.stream()
                .filter(i -> i.getCodigo().equals(ativo.getCodigo()))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(String.format(SEM_COTACAO, ativo.getCodigo())));
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaUmaVenda(AtributosParaCalculoRecomendacaoContext contexto) {
        log.debug(LOG_EXECUTANDO_REGRA, 1, contexto.ativo().getCodigo());

        BigDecimal precoAtual = contexto.cotacao.getCotacaoAtual();
        final BigDecimal precoPago = BigDecimal.valueOf(contexto.vendas.get(0).getPrecoPago());

        final BigDecimal relacao = precoAtual.subtract(precoPago)
                .divide(precoPago, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Teste");
        return new RecomendacaoFinalContext(Recomendacao.COMPRA, EscalaRecomendacao.RECOMENDACAO_01, null, null);
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaDuasVendas(AtributosParaCalculoRecomendacaoContext contexto) {
        log.debug(LOG_EXECUTANDO_REGRA, 2, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaTresVendas(AtributosParaCalculoRecomendacaoContext contexto) {
        log.debug(LOG_EXECUTANDO_REGRA, 3, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaQuatroVendas(AtributosParaCalculoRecomendacaoContext contexto) {
        log.debug(LOG_EXECUTANDO_REGRA, 4, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaCincoVendas(AtributosParaCalculoRecomendacaoContext contexto) {
        log.debug(LOG_EXECUTANDO_REGRA, 5, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaMaisDeCincoVendas(AtributosParaCalculoRecomendacaoContext contexto) {
        return null;
    }

    private void salvarRecomendacao() {

    }
}

package br.com.lunacom.ivmmonitorprocessamento.service;

import br.com.lunacom.comum.domain.Ativo;
import br.com.lunacom.comum.domain.MovimentoVenda;
import br.com.lunacom.comum.domain.dto.CotacaoAgoraDto;
import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.comum.domain.enumeration.EscalaRecomendacao;
import br.com.lunacom.comum.domain.enumeration.Recomendacao;
import br.com.lunacom.comum.domain.enumeration.Status;
import br.com.lunacom.ivmmonitorprocessamento.repository.CotacaoRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.MovimentoVendaRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.RegraCompraPorHistoricoVendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static br.com.lunacom.comum.domain.enumeration.EscalaRecomendacao.*;
import static br.com.lunacom.comum.domain.enumeration.Recomendacao.COMPRA;
import static br.com.lunacom.comum.domain.enumeration.Recomendacao.NEUTRO;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegraCompraPorHistoricoVendaService {

    public static final String SEM_COTACAO = "Não foi encontrada a cotação para o Ativo %s ao tentar calcular a recomendação";
    public static final String SEM_VENDAS = "Nenhum movimento de venda fornecido para cálculo. Esse problema aconteceu para a regra %s";
    public static final String LOG_CALCULAR_RECOMENDACAO_01 = "Processando recomendação para {} (Volume: {} vendas)";
    public static final String LOG_EXECUTANDO_REGRA = "Executando regra específica para {} venda de {}";
    public static final String LOG_SALVAR_RECOMENDACAO_01 = "Uma recomendacao não pode ser salva pois ela não existe";
    public static final String LOG_OBSERVACAO_PARA_UMA_VENDA = "O preço atual considerado para o calculo foi de R$ %s. O valor da aquisição na única venda foi de R$ %s. A relação entre os preços está %s%% mais %s ";
    private final RegraCompraPorHistoricoVendaRepository repository;
    private final CotacaoRepository cotacaoRepository;
    private final MovimentoVendaRepository movimentoVendaRepository;

    private record AtributosParaCalculoRecomendacaoContext(
            Ativo ativo,
            CotacaoAgoraDto cotacao,
            List<MovimentoVenda> vendas
    ) {}

    public record RecomendacaoFinalContext(
            Recomendacao recomendacao,
            EscalaRecomendacao escalaRecomendacao,
            String analise,
            String observacao
    ) {}

    private static final NavigableMap<BigDecimal, EscalaRecomendacao> ESCALAS_COMPRA = new TreeMap<>();

    static {
        ESCALAS_COMPRA.put(new BigDecimal("-9.99"), RECOMENDACAO_10);
        ESCALAS_COMPRA.put(new BigDecimal("-9.00"), RECOMENDACAO_09);
        ESCALAS_COMPRA.put(new BigDecimal("-8.00"), RECOMENDACAO_08);
        ESCALAS_COMPRA.put(new BigDecimal("-7.00"), RECOMENDACAO_07);
        ESCALAS_COMPRA.put(new BigDecimal("-6.00"), RECOMENDACAO_06);
        ESCALAS_COMPRA.put(new BigDecimal("-5.00"), RECOMENDACAO_05);
        ESCALAS_COMPRA.put(new BigDecimal("-4.00"), RECOMENDACAO_04);
        ESCALAS_COMPRA.put(new BigDecimal("-3.00"), RECOMENDACAO_03);
        ESCALAS_COMPRA.put(new BigDecimal("-2.00"), RECOMENDACAO_02);
        ESCALAS_COMPRA.put(new BigDecimal("-0.01"), RECOMENDACAO_01);
    }

    public Map<Integer, RecomendacaoFinalContext> processar(String request) {

        Map<Integer, RecomendacaoFinalContext> recomendacaoFinalContextMap = new HashMap<>();

        final List<CotacaoAgoraDto> cotacaoAgoraDtoList = this.buscarCotacaoAgora();

        final List<RegraCompraPorHistoricoVenda> regraList = this.buscarRegrasAtivas();

        regraList.forEach( regra -> {
            final List<MovimentoVenda> movimentoVendas = this.buscarVendasPassadas(regra);

            final RecomendacaoFinalContext recomendacaoFinalContext = this
                    .calcularRecomendacao(regra, cotacaoAgoraDtoList, movimentoVendas);

            this.salvarRecomendacao(regra, recomendacaoFinalContext);
            recomendacaoFinalContextMap.put(
                    regra.getId(),
                    recomendacaoFinalContext);
        });
        return recomendacaoFinalContextMap;
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
                Pageable limiteCinco = PageRequest.of(0, 5);
                List<MovimentoVenda> vendas = movimentoVendaRepository
                        .buscarUltimasCincoVendas(
                                codigoAtivo,
                                dataFiltroOpcional.orElse(null),
                                regra.getExcluirPrejuizos(),
                                limiteCinco);
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
    }

    private RecomendacaoFinalContext calcularRecomendacao(
            RegraCompraPorHistoricoVenda regra, List<CotacaoAgoraDto> cotacaoAgoraDtoList,
            List<MovimentoVenda> movimentoVendas)
    {

        if (movimentoVendas == null || movimentoVendas.isEmpty()) {
            log.warn(String.format(SEM_VENDAS, regra.getId()));
            return null;
        }

        final Ativo ativo = encontrarAtivo(movimentoVendas);

        final Optional<CotacaoAgoraDto> cotacaoAgoraDto = buscarCotacao(cotacaoAgoraDtoList, ativo);

        if (cotacaoAgoraDto.isEmpty()) {
            String observacao = String.format(SEM_COTACAO, ativo.getCodigo());
            log.warn(observacao);
            return new RecomendacaoFinalContext(null, null, null, observacao);
        }

        final AtributosParaCalculoRecomendacaoContext contexto = new AtributosParaCalculoRecomendacaoContext
                (ativo, cotacaoAgoraDto.get(), movimentoVendas);

        log.info(LOG_CALCULAR_RECOMENDACAO_01, ativo.getCodigo(), movimentoVendas.size());

        return switch (movimentoVendas.size()) {
            case 1 -> this.calcularRecomendacaoParaUmaVenda(contexto);
            default -> this.calcularRecomendacaoParaMultiplasVendas(contexto);
        };

    }

    private Ativo encontrarAtivo(List<MovimentoVenda> movimentoVendas) {
        return movimentoVendas.get(0).getAtivo();
    }

    private Optional<CotacaoAgoraDto> buscarCotacao(List<CotacaoAgoraDto> cotacaoAgoraDtoList, Ativo ativo) {
         return cotacaoAgoraDtoList.stream()
                .filter(i -> i.getCodigo().equals(ativo.getCodigo()))
                .findFirst();
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaUmaVenda(
            AtributosParaCalculoRecomendacaoContext contexto)
    {
        log.debug(LOG_EXECUTANDO_REGRA, 1, contexto.ativo().getCodigo());

        BigDecimal precoAtual = contexto.cotacao.getCotacaoAtual();
        BigDecimal precoPago = BigDecimal.valueOf(contexto.vendas.get(0).getPrecoPago());

        BigDecimal relacao = precoAtual.subtract(precoPago)
                .divide(precoPago, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        String ajuste = "cara";
        Recomendacao recomendacaoFinal = NEUTRO;
        EscalaRecomendacao escalaFinal = RECOMENDACAO_00;

        if (relacao.compareTo(BigDecimal.ZERO) <= 0) {
            ajuste = "barata";
            recomendacaoFinal = COMPRA;
            var entry = ESCALAS_COMPRA.higherEntry(relacao);
            if (entry != null) {
                escalaFinal = entry.getValue();
            } else if (relacao.compareTo(BigDecimal.ZERO) < 0) {
                escalaFinal = RECOMENDACAO_01;
            }
        }

        String observacao = String.format(LOG_OBSERVACAO_PARA_UMA_VENDA, precoAtual, precoPago, relacao, ajuste);

        return new RecomendacaoFinalContext(recomendacaoFinal, escalaFinal, null, observacao);
    }


    private String listarVendas(List<MovimentoVenda> vendas) {
        if (vendas == null || vendas.isEmpty()) {
            return "()";
        }

        StringBuilder sb = new StringBuilder("(\n");

        for (MovimentoVenda i : vendas) {
            sb.append("R$ ")
                    .append(i.getPrecoPago())
                    .append(" em ")
                    .append(i.getDataAquisicao())
                    .append("\n");
        }

        sb.append(")");
        return sb.toString();
    }


    private RecomendacaoFinalContext calcularRecomendacaoParaMultiplasVendas(
            AtributosParaCalculoRecomendacaoContext contexto)
    {
        Integer vendasTotal = contexto.vendas.size();

        Map<Integer, EscalaRecomendacao> enquadramento
                = getIntegerEscalaRecomendacaoMap(vendasTotal);

        BigDecimal precoAtual = contexto.cotacao.getCotacaoAtual();

        final List<BigDecimal> objects = new ArrayList<>();
        contexto.vendas
                .stream()
                .map(v -> BigDecimal.valueOf(v.getPrecoPago()))
                .forEach(i -> {
                    if (i.compareTo(precoAtual) >= 0) {
                        objects.add(i);
                    }
                });

        Recomendacao recomendacaoFinal = COMPRA;
        final EscalaRecomendacao escalaFinal = enquadramento.get(objects.size());
        String maiorMenor = "menor ou igual";
        String terceira = String.valueOf(objects.size()).concat(" das");
        if (objects.isEmpty()) {
            recomendacaoFinal = NEUTRO;
            maiorMenor = "maior";
            terceira = "todas as";
        }
        String vendasDescricao = this.listarVendas(contexto.vendas);
        String observacao = String.format("O preço atual considerado para o cálculo foi de R$ %s. O valor atual está %s que %s %s aquisições anteriores. %s",
                precoAtual, maiorMenor, terceira, vendasTotal, vendasDescricao);

        log.debug(LOG_EXECUTANDO_REGRA, 3, contexto.ativo().getCodigo());
        return new RecomendacaoFinalContext(recomendacaoFinal, escalaFinal, null, observacao);

    }

    private Map<Integer, EscalaRecomendacao> getIntegerEscalaRecomendacaoMap(Integer quantidade) {

        return switch (quantidade) {

            case 2 -> {
                Map<Integer, EscalaRecomendacao> enquadramento = new HashMap<>();
                enquadramento.put(2, RECOMENDACAO_10);
                enquadramento.put(1, RECOMENDACAO_05);
                enquadramento.put(0, RECOMENDACAO_00);
                yield enquadramento;
            }
            case 3 -> {
                Map<Integer, EscalaRecomendacao> enquadramento = new HashMap<>();
                enquadramento.put(3, RECOMENDACAO_10);
                enquadramento.put(2, RECOMENDACAO_07);
                enquadramento.put(1, RECOMENDACAO_04);
                enquadramento.put(0, RECOMENDACAO_00);
                yield enquadramento;
            }
            case 4 -> {
                Map<Integer, EscalaRecomendacao> enquadramento = new HashMap<>();
                enquadramento.put(4, RECOMENDACAO_10);
                enquadramento.put(3, RECOMENDACAO_08);
                enquadramento.put(2, RECOMENDACAO_06);
                enquadramento.put(1, RECOMENDACAO_02);
                enquadramento.put(0, RECOMENDACAO_00);
                yield enquadramento;
            }
            default -> {
                Map<Integer, EscalaRecomendacao> enquadramento = new HashMap<>();
                enquadramento.put(5, RECOMENDACAO_10);
                enquadramento.put(4, RECOMENDACAO_08);
                enquadramento.put(3, RECOMENDACAO_06);
                enquadramento.put(2, RECOMENDACAO_04);
                enquadramento.put(1, RECOMENDACAO_02);
                enquadramento.put(0, RECOMENDACAO_00);
                yield enquadramento;
            }
        };

    }

    private RecomendacaoFinalContext calcularRecomendacaoParaQuatroVendas(
            AtributosParaCalculoRecomendacaoContext contexto)
    {
        log.debug(LOG_EXECUTANDO_REGRA, 4, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaCincoVendas(
            AtributosParaCalculoRecomendacaoContext contexto)
    {
        log.debug(LOG_EXECUTANDO_REGRA, 5, contexto.ativo().getCodigo());
        return null;
    }

    private RecomendacaoFinalContext calcularRecomendacaoParaMaisDeCincoVendas(
            AtributosParaCalculoRecomendacaoContext contexto)
    {
        return null;
    }

    private void salvarRecomendacao(
            RegraCompraPorHistoricoVenda regra,
            RecomendacaoFinalContext recomendacaoFinalContext)
    {

        if (Objects.isNull(recomendacaoFinalContext)) {
            log.warn(LOG_SALVAR_RECOMENDACAO_01);
            return;
        }

        final String recomendacao = Optional
                .ofNullable(recomendacaoFinalContext)
                .map( ctx -> ctx.recomendacao)
                .map( rec -> rec.getCodigo())
                .orElse(null);

        final Integer escala = Optional
                .ofNullable(recomendacaoFinalContext)
                .map( ctx -> ctx.escalaRecomendacao)
                .map( esc -> Integer.parseInt(esc.getCodigo()))
                .orElse(null);

        regra.setRecomendacao(recomendacao);
        regra.setRecomendacaoEscala(escala);
        regra.setAnalise(recomendacaoFinalContext.analise);
        regra.setObservacao(recomendacaoFinalContext.observacao);
        repository.save(regra);
    }
}

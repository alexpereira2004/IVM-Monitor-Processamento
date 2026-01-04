package br.com.lunacom.ivmmonitorprocessamento.service;

import br.com.lunacom.comum.domain.Ativo;
import br.com.lunacom.comum.domain.MovimentoVenda;
import br.com.lunacom.comum.domain.dto.CotacaoAgoraDto;
import br.com.lunacom.comum.domain.entity.monitor.Monitor;
import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.comum.domain.enumeration.PeriodoVenda;
import br.com.lunacom.comum.domain.enumeration.Status;
import br.com.lunacom.ivmmonitorprocessamento.repository.CotacaoRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.MovimentoVendaRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.RegraCompraPorHistoricoVendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RegraCompraPorHistoricoVendaServiceTest {


    @Mock
    private RegraCompraPorHistoricoVendaRepository repository;
    @Mock
    private CotacaoRepository cotacaoRepository;
    @Mock
    private MovimentoVendaRepository movimentoVendaRepository;

    @InjectMocks
    private RegraCompraPorHistoricoVendaService service;

    private RegraCompraPorHistoricoVenda regraMock;
    private Ativo ativoMock;

    @BeforeEach
    void setUp() {
        ativoMock = new Ativo();
        ativoMock.setCodigo("BBSE3");

        Monitor monitorMock = new Monitor();
        monitorMock.setAtivo(ativoMock);

        regraMock = new RegraCompraPorHistoricoVenda();
        regraMock.setMonitor(monitorMock);
    }

    @Test
    @DisplayName("Deve buscar vendas passadas quando o periodo for ULTIMA_VENDA")
    void deveBuscarVendasPassadasUltimaVenda() {
        // Arrange
        regraMock.setId(1);
        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);

        MovimentoVenda venda = new MovimentoVenda();
        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc("BBSE3"))
                .thenReturn(Optional.of(venda));

        // Act
        List<MovimentoVenda> resultado = service.buscarVendasPassadas(regraMock);

        // Assert
        assertEquals(1, resultado.size());
        verify(movimentoVendaRepository).findTopByAtivoCodigoOrderByDataVendaDesc("BBSE3");
    }

//    @Test
//    @DisplayName("Deve buscar vendas passadas quando o período for ANO_ATUAL")
//    void deveBuscarVendasPassadasAnoAtual() {
//        // Arrange
//        regraMock.setPeriodo(PeriodoVenda.ANO_ATUAL);
//        when(movimentoVendaRepository.buscarUltimasCincoVendasPorDataVenda(eq("BBSE3"), any(LocalDate.class)))
//                .thenReturn(List.of(new MovimentoVenda(), new MovimentoVenda()));
//
//        // Act
//        List<MovimentoVenda> resultado = service.buscarVendasPassadas(regraMock);
//
//        // Assert
//        assertEquals(2, resultado.size());
//        verify(movimentoVendaRepository).buscarUltimasCincoVendasPorDataVenda(eq("BBSE3"), any(LocalDate.class));
//    }

    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacao")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha UMA venda")
    void deveProcessarComSucesso(BigDecimal precoAtual,
                                 String descEsperada,
                                 String escalaEsperada,
                                 String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("BBSE3");
        cotacao.setCotacaoAtual(precoAtual);

        MovimentoVenda v1 = new MovimentoVenda();
        v1.setAtivo(ativoMock);
        v1.setPrecoPago(10.0);

        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);
        regraMock.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regraMock));

        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc("BBSE3"))
                .thenReturn(Optional.of(v1));

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }



    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacaoDuasVendas")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha DUAS vendas")
    void deveProcessarComSucessoDuasVendas(BigDecimal precoAtual,
                                           String descEsperada,
                                           String escalaEsperada,
                                           String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("BEES4");
        cotacao.setCotacaoAtual(precoAtual);

        final Ativo ativo = new Ativo();
        ativo.setCodigo("BEES4");


        Monitor monitor = new Monitor();
        monitor.setAtivo(ativo);

        final RegraCompraPorHistoricoVenda regra = new RegraCompraPorHistoricoVenda();
        regra.setMonitor(monitor);
        regra.setPeriodo(PeriodoVenda.TODO_HISTORICO);
        regra.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regra));

        final List<MovimentoVenda> vendaList = this.criarListaVendas(ativo, 10.0, 15.0);
        when(movimentoVendaRepository.buscarUltimasCincoVendas("BEES4", null, "S", null))
                .thenReturn(vendaList);

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
//        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }


    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacaoTresVendas")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha TRES vendas")
    void deveProcessarComSucessoTresVendas(BigDecimal precoAtual,
                                           String descEsperada,
                                           String escalaEsperada,
                                           String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("PETR4");
        cotacao.setCotacaoAtual(precoAtual);

        final Ativo ativo = new Ativo();
        ativo.setCodigo("PETR4");


        Monitor monitor = new Monitor();
        monitor.setAtivo(ativo);

        final RegraCompraPorHistoricoVenda regra = new RegraCompraPorHistoricoVenda();
        regra.setMonitor(monitor);
        regra.setPeriodo(PeriodoVenda.TODO_HISTORICO);
        regra.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regra));

        final List<MovimentoVenda> vendaList = this.criarListaVendas(ativo, 10.0, 12.5, 15.0);
        when(movimentoVendaRepository.buscarUltimasCincoVendas("PETR4", null, "S", null))
                .thenReturn(vendaList);

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
//        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }


    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacaoQuatroVendas")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha QUATRO vendas")
    void deveProcessarComSucessoQuatroVendas(BigDecimal precoAtual,
                                           String descEsperada,
                                           String escalaEsperada,
                                           String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("VULC3");
        cotacao.setCotacaoAtual(precoAtual);

        final Ativo ativo = new Ativo();
        ativo.setCodigo("VULC3");


        Monitor monitor = new Monitor();
        monitor.setAtivo(ativo);

        final RegraCompraPorHistoricoVenda regra = new RegraCompraPorHistoricoVenda();
        regra.setMonitor(monitor);
        regra.setPeriodo(PeriodoVenda.TODO_HISTORICO);
        regra.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regra));

        final List<MovimentoVenda> vendaList = this.criarListaVendas(ativo, 10.0, 12.5, 15.0, 17.5);

        when(movimentoVendaRepository.buscarUltimasCincoVendas("VULC3", null, "S", null))
                .thenReturn(vendaList);

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
//        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }


    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacaoCincoVendas")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha CINCO vendas")
    void deveProcessarComSucessoCincoVendas(BigDecimal precoAtual,
                                             String descEsperada,
                                             String escalaEsperada,
                                             String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("BRSR6");
        cotacao.setCotacaoAtual(precoAtual);

        final Ativo ativo = new Ativo();
        ativo.setCodigo("BRSR6");


        Monitor monitor = new Monitor();
        monitor.setAtivo(ativo);

        final RegraCompraPorHistoricoVenda regra = new RegraCompraPorHistoricoVenda();
        regra.setMonitor(monitor);
        regra.setPeriodo(PeriodoVenda.TODO_HISTORICO);
        regra.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regra));

        final List<MovimentoVenda> vendaList = this.criarListaVendas(ativo, 10.0, 12.5, 15.0, 17.5, 20.0);

        when(movimentoVendaRepository.buscarUltimasCincoVendas("BRSR6", null, "S", null))
                .thenReturn(vendaList);

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
//        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }



    @ParameterizedTest
    @MethodSource("providerParaBateriaRecomendacaoCincoMaisVendas")
    @DisplayName("Deve calcular com sucesso recomendacao para acao que tenha MAIS de CINCO vendas")
    void deveProcessarComSucessoCincoMaisVendas(BigDecimal precoAtual,
                                            String descEsperada,
                                            String escalaEsperada,
                                            String ajuste) throws Exception
    {
        // Cenário
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("TAEE3");
        cotacao.setCotacaoAtual(precoAtual);

        final Ativo ativo = new Ativo();
        ativo.setCodigo("TAEE3");


        Monitor monitor = new Monitor();
        monitor.setAtivo(ativo);

        final RegraCompraPorHistoricoVenda regra = new RegraCompraPorHistoricoVenda();
        regra.setMonitor(monitor);
        regra.setPeriodo(PeriodoVenda.TODO_HISTORICO);
        regra.setId(1);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));

        when(repository.findByStatusAndValidade(Status.ATIVO, null))
                .thenReturn(List.of(regra));

        final List<MovimentoVenda> vendaList = this.criarListaVendas(ativo, 9.0, 9.5, 10.0, 12.5, 15.0, 17.5, 20.0);

        when(movimentoVendaRepository.buscarUltimasCincoVendas("TAEE3", null, "S", null))
                .thenReturn(vendaList);

        // Ação
        final Map<Integer, RegraCompraPorHistoricoVendaService.RecomendacaoFinalContext> resposta =
                service.processar("");

        // Verificação
        assertNotNull(resposta);
        assertEquals(descEsperada, resposta.get(1).recomendacao().getDescricao());
        assertEquals(escalaEsperada, resposta.get(1).escalaRecomendacao().getCodigo());
//        assertThat(resposta.get(1).observacao()).contains(ajuste);
    }

    @Test
    @DisplayName("Deve lançar exceção quando não houver cotação para o ativo no processamento")
    void deveLancarExcecaoQuandoSemCotacao() {
        // Arrange
        CotacaoAgoraDto cotacaoDiferente = new CotacaoAgoraDto();
        cotacaoDiferente.setCodigo("VALE3"); // Diferente de BBSE3

        MovimentoVenda v1 = new MovimentoVenda();
        v1.setAtivo(ativoMock);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacaoDiferente));
        when(repository.findByStatusAndValidade(Status.ATIVO, null)).thenReturn(List.of(regraMock));
        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc(anyString())).thenReturn(Optional.of(v1));
        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () -> service.processar("request"));
    }

    private static Stream<Arguments> providerParaBateriaRecomendacao() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(9.09), "Compra", "9", "barata"),
                Arguments.of(BigDecimal.valueOf(9.19), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(9.29), "Compra", "7", "barata"),
                Arguments.of(BigDecimal.valueOf(9.39), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(9.49), "Compra", "5", "barata"),
                Arguments.of(BigDecimal.valueOf(9.59), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(9.69), "Compra", "3", "barata"),
                Arguments.of(BigDecimal.valueOf(9.79), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(9.89), "Compra", "1", "barata"),
                Arguments.of(BigDecimal.valueOf(9.99), "Compra", "1" ,"barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "0", "barata"),
                Arguments.of(BigDecimal.valueOf(10.02), "Neutro", "0", "cara"),
                Arguments.of(BigDecimal.valueOf(11), "Neutro", "0", "cara")
        );
    }

    private static Stream<Arguments> providerParaBateriaRecomendacaoDuasVendas() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(11), "Compra", "5", "barata"),
                Arguments.of(BigDecimal.valueOf(15), "Compra", "5", "barata"),
                Arguments.of(BigDecimal.valueOf(20), "Neutro", "0", "cara")
        );
    }


    private static Stream<Arguments> providerParaBateriaRecomendacaoTresVendas() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(11), "Compra", "7", "barata"),
                Arguments.of(BigDecimal.valueOf(12.5), "Compra", "7", "barata"),
                Arguments.of(BigDecimal.valueOf(13), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(15), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(20), "Neutro", "0", "cara")
        );
    }

    private static Stream<Arguments> providerParaBateriaRecomendacaoQuatroVendas() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(11), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(12.5), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(13), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(15), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(16), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(17.5), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(20), "Neutro", "0", "cara")
        );
    }

    private static Stream<Arguments> providerParaBateriaRecomendacaoCincoVendas() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(11), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(12.5), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(13), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(15), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(16), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(17.5), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(18), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(20), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(21), "Neutro", "0", "cara")
        );
    }

    private static Stream<Arguments> providerParaBateriaRecomendacaoCincoMaisVendas() {
        return Stream.of(

                Arguments.of(BigDecimal.valueOf(8), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(10), "Compra", "10", "barata"),
                Arguments.of(BigDecimal.valueOf(11), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(12.5), "Compra", "8", "barata"),
                Arguments.of(BigDecimal.valueOf(13), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(15), "Compra", "6", "barata"),
                Arguments.of(BigDecimal.valueOf(16), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(17.5), "Compra", "4", "barata"),
                Arguments.of(BigDecimal.valueOf(18), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(20), "Compra", "2", "barata"),
                Arguments.of(BigDecimal.valueOf(21), "Neutro", "0", "cara")
        );
    }

    public List<MovimentoVenda> criarListaVendas(Ativo ativo, Double... precos) {
        final List<LocalDate> dataAquisicaoList = Arrays.asList(
                LocalDate.of(2025,12,1),
                LocalDate.of(2026,1,10),
                LocalDate.of(2026,2,15),
                LocalDate.of(2026,3,20),
                LocalDate.of(2026,4,24),
                LocalDate.of(2026,5,28),
                LocalDate.of(2026,6,30)
        );

        AtomicInteger cont = new AtomicInteger();

        return Arrays.stream(precos)
                .map(preco -> {
                    MovimentoVenda v = new MovimentoVenda();
                    v.setAtivo(ativo);
                    v.setPrecoPago(preco);
                    v.setDataAquisicao(dataAquisicaoList.get(cont.get()));
                    cont.getAndIncrement();
                    return v;
                })
                .collect(Collectors.toList());
    }
}
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        ativoMock.setCodigo("PETR4");

        Monitor monitorMock = new Monitor();
        monitorMock.setAtivo(ativoMock);

        regraMock = new RegraCompraPorHistoricoVenda();
        regraMock.setMonitor(monitorMock);
    }

    @Test
    @DisplayName("Deve buscar vendas passadas quando o periodo for ULTIMA_VENDA")
    void deveBuscarVendasPassadasUltimaVenda() {
        // Arrange
        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);
        MovimentoVenda venda = new MovimentoVenda();
        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc("PETR4"))
                .thenReturn(Optional.of(venda));

        // Act
        List<MovimentoVenda> resultado = service.buscarVendasPassadas(regraMock);

        // Assert
        assertEquals(1, resultado.size());
        verify(movimentoVendaRepository).findTopByAtivoCodigoOrderByDataVendaDesc("PETR4");
    }

    @Test
    @DisplayName("Deve buscar vendas passadas quando o período for ANO_ATUAL")
    void deveBuscarVendasPassadasAnoAtual() {
        // Arrange
        regraMock.setPeriodo(PeriodoVenda.ANO_ATUAL);
        when(movimentoVendaRepository.findAllByAtivoCodigoAndDataVendaAfter(eq("PETR4"), any(LocalDate.class)))
                .thenReturn(List.of(new MovimentoVenda(), new MovimentoVenda()));

        // Act
        List<MovimentoVenda> resultado = service.buscarVendasPassadas(regraMock);

        // Assert
        assertEquals(2, resultado.size());
        verify(movimentoVendaRepository).findAllByAtivoCodigoAndDataVendaAfter(eq("PETR4"), any(LocalDate.class));
    }

    @Test
    @DisplayName("Deve processar regras e invocar cálculo de recomendação")
    void deveProcessarComSucesso() throws Exception {
        // Arrange
        CotacaoAgoraDto cotacao = new CotacaoAgoraDto();
        cotacao.setCodigo("PETR4");

        MovimentoVenda v1 = new MovimentoVenda();
        v1.setAtivo(ativoMock);

        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacao));
        when(repository.findByStatusAndValidade(Status.ATIVO, null)).thenReturn(List.of(regraMock));
        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc("PETR4")).thenReturn(Optional.of(v1));

        // Act
        // Como o método 'processar' é void e apenas loga, verificamos se ele executa sem exceções
        // e se os métodos de busca foram chamados.
        assertDoesNotThrow(() -> service.processar("dummy request"));

        // Assert
        verify(cotacaoRepository).pesquisarCotacaoAgora();
        verify(repository).findByStatusAndValidade(Status.ATIVO, null);
    }

    @Test
    @DisplayName("Deve lançar exceção quando não houver cotação para o ativo no processamento")
    void deveLancarExcecaoQuandoSemCotacao() {
        // Arrange
        CotacaoAgoraDto cotacaoDiferente = new CotacaoAgoraDto();
        cotacaoDiferente.setCodigo("VALE3"); // Diferente de PETR4

        MovimentoVenda v1 = new MovimentoVenda();
        v1.setAtivo(ativoMock);

        when(cotacaoRepository.pesquisarCotacaoAgora()).thenReturn(List.of(cotacaoDiferente));
        when(repository.findByStatusAndValidade(Status.ATIVO, null)).thenReturn(List.of(regraMock));
        when(movimentoVendaRepository.findTopByAtivoCodigoOrderByDataVendaDesc(anyString())).thenReturn(Optional.of(v1));
        regraMock.setPeriodo(PeriodoVenda.ULTIMA_VENDA);

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class, () -> service.processar("request"));
    }
}
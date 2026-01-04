package br.com.lunacom.ivmmonitorprocessamento.resource;

import br.com.lunacom.ivmmonitorprocessamento.service.RegraCompraPorHistoricoVendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value="/processamento-manual")
public class ProcessamentoManual {

    private final RegraCompraPorHistoricoVendaService regraCompraPorHistoricoVendaService;

    @PostMapping("/compra-por-historico-venda")
    public void compraPorHistoricoVenda(String request) {
        regraCompraPorHistoricoVendaService.processar(request);
    }
}

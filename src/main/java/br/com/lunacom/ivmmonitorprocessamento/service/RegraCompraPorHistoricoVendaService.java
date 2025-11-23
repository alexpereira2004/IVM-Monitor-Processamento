package br.com.lunacom.ivmmonitorprocessamento.service;

import br.com.lunacom.comum.domain.MovimentoVenda;
import br.com.lunacom.comum.domain.entity.monitor.Monitor;
import br.com.lunacom.comum.domain.entity.monitor.RegraCompraPorHistoricoVenda;
import br.com.lunacom.ivmmonitorprocessamento.repository.MovimentoVendaRepository;
import br.com.lunacom.ivmmonitorprocessamento.repository.RegraCompraPorHistoricoVendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegraCompraPorHistoricoVendaService {

    private final RegraCompraPorHistoricoVendaRepository repository;
    private final MovimentoVendaRepository movimentoVendaRepository;

    public void processar(String request) throws Exception {
        final List<RegraCompraPorHistoricoVenda> all = repository.findAll();

        Monitor monitorProxy = all.get(13).getMonitor();


        final String s = monitorProxy.getAtivo().toString();

        final List<MovimentoVenda> allByAtivoCodigo = movimentoVendaRepository.findAllByAtivoCodigo(monitorProxy.getAtivo().getCodigo());

        log.info(all.get(0).getMonitor().getAtivo().toString());
    }
}

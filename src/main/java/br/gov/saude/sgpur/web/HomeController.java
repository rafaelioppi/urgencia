package br.gov.saude.sgpur.web;

import br.gov.saude.sgpur.domain.Processo;
import br.gov.saude.sgpur.domain.StatusProcesso;
import br.gov.saude.sgpur.repository.MembroUrgenciaRenalRepository;
import br.gov.saude.sgpur.repository.ProcessoRepository;
import br.gov.saude.sgpur.service.FluxoProcessoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final ProcessoRepository processoRepository;
    private final MembroUrgenciaRenalRepository membroRepository;
    private final FluxoProcessoService fluxoService;

    public HomeController(ProcessoRepository processoRepository,
                          MembroUrgenciaRenalRepository membroRepository,
                          FluxoProcessoService fluxoService) {
        this.processoRepository = processoRepository;
        this.membroRepository = membroRepository;
        this.fluxoService = fluxoService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalProcessos", processoRepository.count());
        model.addAttribute("emAnalise", processoRepository.countByStatus(StatusProcesso.EM_ANALISE));
        model.addAttribute("deferidos", processoRepository.countByStatus(StatusProcesso.DEFERIDO));
        model.addAttribute("indeferidos", processoRepository.countByStatus(StatusProcesso.INDEFERIDO));
        model.addAttribute("cancelados", processoRepository.countByStatus(StatusProcesso.CANCELADO));
        model.addAttribute("membrosAtivos", membroRepository.countByAtivoTrue());
        model.addAttribute("ultimos",
            processoRepository.findAllByOrderByAnoDescSequencialDesc()
                .stream().limit(5).toList());

        // Processos aguardando acao (em analise) + o que falta em cada um
        var pendentes = processoRepository
            .findByStatusOrderByAnoDescSequencialDesc(StatusProcesso.EM_ANALISE);
        Map<Long, String> pendencias = new LinkedHashMap<>();
        for (Processo p : pendentes) {
            pendencias.put(p.getId(), fluxoService.resumoPendencia(p));
        }
        model.addAttribute("pendentes", pendentes);
        model.addAttribute("pendencias", pendencias);
        return "dashboard";
    }
}

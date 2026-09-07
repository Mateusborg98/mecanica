package br.com.techchallenge.mecanica.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.techchallenge.mecanica.application.dto.ordemdeservico.AdicionarItensOrdemDeServicoInput;
import br.com.techchallenge.mecanica.application.dto.ordemdeservico.CriarOrdemDeServicoInput;
import br.com.techchallenge.mecanica.application.usecase.operador.BuscarOperadorPorMatriculaUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.AprovarOrcamentoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.AguardarAprovacaoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.AdicionarItensNaOrdemDeServicoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.BuscarOrdemDeServicoPorIdUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.CalcularTempoMedioServicosUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.CriarOrdemDeServicoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.EntregarOrdemDeServicoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.FinalizarOrdemDeServicoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.IniciarDiagnosticoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.ListarOrdensDeServicoUseCase;
import br.com.techchallenge.mecanica.application.usecase.ordemdeservico.NegarOrcamentoUseCase;
import br.com.techchallenge.mecanica.application.usecase.veiculo.BuscarVeiculoPorPlacaUseCase;
import br.com.techchallenge.mecanica.domain.operador.Operador;
import br.com.techchallenge.mecanica.domain.veiculo.Veiculo;
import br.com.techchallenge.mecanica.infrastructure.security.UsuarioAutenticadoService;
import br.com.techchallenge.mecanica.presentation.dto.ordemDeServico.AddServicoPecaOrdemDeServicoDto;
import br.com.techchallenge.mecanica.presentation.dto.ordemDeServico.AcompanhamentoOrdemResponse;
import br.com.techchallenge.mecanica.presentation.dto.ordemDeServico.CriarOrdemDeServicoRequest;
import br.com.techchallenge.mecanica.presentation.dto.ordemDeServico.OrdemDeServicoResponse;
import br.com.techchallenge.mecanica.presentation.dto.ordemDeServico.TempoMedioServicoResponseDto;
import br.com.techchallenge.mecanica.presentation.mapper.OrdemDeServicoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Ordem de Serviço", description = "Controller referente a entidade Ordem de Serviço.")
@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
public class OrdemDeServicoController {

    private final CriarOrdemDeServicoUseCase criarOrdemDeServicoUseCase;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final BuscarVeiculoPorPlacaUseCase buscarVeiculoPorPlacaUseCase;
    private final BuscarOperadorPorMatriculaUseCase buscarOperadorPorMatriculaUseCase;
    private final BuscarOrdemDeServicoPorIdUseCase buscarOrdemDeServicoPorIdUseCase;
    private final ListarOrdensDeServicoUseCase listarOrdensDeServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final AguardarAprovacaoUseCase aguardarAprovacaoUseCase;
    private final AprovarOrcamentoUseCase aprovarOrcamentoUseCase;
    private final NegarOrcamentoUseCase negarOrcamentoUseCase;
    private final FinalizarOrdemDeServicoUseCase finalizarOrdemDeServicoUseCase;
    private final EntregarOrdemDeServicoUseCase entregarOrdemDeServicoUseCase;
    private final AdicionarItensNaOrdemDeServicoUseCase adicionarItensNaOrdemDeServicoUseCase;
    private final CalcularTempoMedioServicosUseCase calcularTempoMedioServicosUseCase;
    private final OrdemDeServicoPresentationMapper mapper;

    @Operation(summary = "Criar ordem de serviço")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemDeServicoResponse criar(
            @RequestBody @Valid CriarOrdemDeServicoRequest request) {

        UUID clienteId = usuarioAutenticadoService.getClienteId();
        Veiculo veiculo = buscarVeiculoPorPlacaUseCase.executar(request.placa());
        Operador operador = buscarOperadorPorMatriculaUseCase.executar(
                request.matriculaOperador());

        return mapper.toResponse(criarOrdemDeServicoUseCase.executar(
                new CriarOrdemDeServicoInput(
                        clienteId,
                        veiculo.getId(),
                        operador.getId(),
                        servicosDa(request),
                        pecasDa(request))));
    }

    @Operation(summary = "Acompanhar publicamente o status de uma ordem de serviço")
    @GetMapping("/acompanhamento/{id}")
    public AcompanhamentoOrdemResponse acompanhar(@PathVariable UUID id) {
        var ordem = buscarOrdemDeServicoPorIdUseCase.executar(id);
        return new AcompanhamentoOrdemResponse(
                ordem.getId(),
                ordem.getStatus().name(),
                ordem.getValorTotalOs(),
                ordem.getDtInicioOs(),
                ordem.getDtFimOs());
    }

    @Operation(summary = "Listar ondens de serviço")
    @GetMapping
    public List<OrdemDeServicoResponse> listar() {
        return listarOrdensDeServicoUseCase.executar()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Operation(summary = "Colocar ordem de serviço em diagnostico")
    @PostMapping("/{id}/iniciar-diagnostico")
    public OrdemDeServicoResponse iniciarDiagnostico(@PathVariable UUID id) {
        return mapper.toResponse(iniciarDiagnosticoUseCase.executar(id));
    }

    @Operation(summary = "Enviar orçamento para aprovação do cliente")
    @PostMapping("/{id}/aguardar-aprovacao")
    public OrdemDeServicoResponse aguardarAprovacao(@PathVariable UUID id) {
        return mapper.toResponse(aguardarAprovacaoUseCase.executar(id));
    }

    @Operation(summary = "Cliente aprovar orçamento através do ID (UUID)")
    @PostMapping("/{id}/aprovar-orcamento")
    public OrdemDeServicoResponse aprovarOrcamento(@PathVariable UUID id) {
        return mapper.toResponse(aprovarOrcamentoUseCase.executar(id));
    }

    @Operation(summary = "Cliente negar orçamento através do ID (UUID)")
    @PostMapping("/{id}/negar-orcamento")
    public OrdemDeServicoResponse negarOrcamento(@PathVariable UUID id) {
        return mapper.toResponse(negarOrcamentoUseCase.executar(id));
    }

    @Operation(summary = "Finalizar ordem de serviço")
    @PostMapping("/{id}/finalizar")
    public OrdemDeServicoResponse finalizar(@PathVariable UUID id) {
        return mapper.toResponse(finalizarOrdemDeServicoUseCase.executar(id));
    }

    @Operation(summary = "Entregar ordem de serviço")
    @PostMapping("/{id}/entregar")
    public OrdemDeServicoResponse entregar(@PathVariable UUID id) {
        return mapper.toResponse(entregarOrdemDeServicoUseCase.executar(id));
    }

    @Operation(summary = "Consultar tempo médio dos serviços")
    @GetMapping("/tempo-medio-servicos")
    public ResponseEntity<List<TempoMedioServicoResponseDto>> calcularTempoMedioServicos() {
        var response = calcularTempoMedioServicosUseCase.executar().stream()
                .map(item -> new TempoMedioServicoResponseDto(
                        item.servicoId(), item.tempoMedioEmMinutos()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Adicionar serviços e peças/insumos na ordem de serviço")
    @PostMapping("/{id}/adicionar-servicos-pecas")
    public OrdemDeServicoResponse adicionarServicoPeca(
            @PathVariable UUID id,
            @RequestBody @Valid AddServicoPecaOrdemDeServicoDto request) {

        var servicos = request.getServicos() == null ? List.<UUID>of()
                : request.getServicos().stream().map(item -> item.getServicoId()).toList();
        var pecas = request.getPecas() == null ? List.<AdicionarItensOrdemDeServicoInput.PecaInput>of()
                : request.getPecas().stream()
                        .map(item -> new AdicionarItensOrdemDeServicoInput.PecaInput(
                                item.getPecaId(), item.getQuantidade()))
                        .toList();
        var ordem = adicionarItensNaOrdemDeServicoUseCase.executar(
                new AdicionarItensOrdemDeServicoInput(id, servicos, pecas));

        return mapper.toResponse(ordem);
    }

    private List<UUID> servicosDa(CriarOrdemDeServicoRequest request) {
        return request.servicos() == null ? List.of()
                : request.servicos().stream()
                        .map(item -> item.getServicoId())
                        .toList();
    }

    private List<CriarOrdemDeServicoInput.PecaInput> pecasDa(CriarOrdemDeServicoRequest request) {
        return request.pecas() == null ? List.of()
                : request.pecas().stream()
                        .map(item -> new CriarOrdemDeServicoInput.PecaInput(
                                item.getPecaId(),
                                item.getQuantidade()))
                        .toList();
    }
}

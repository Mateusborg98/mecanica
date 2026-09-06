package br.com.techchallenge.mecanica.presentation.dto.ordemDeServico;

import java.util.List;

import br.com.techchallenge.mecanica.presentation.annotation.PlacaValida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarOrdemDeServicoRequest(
    @NotBlank
    @PlacaValida
    String placa,

    @NotNull
    @Positive
    Integer matriculaOperador,

    List<@Valid ServicoRequestDto> servicos,

    List<@Valid PecaRequestDto> pecas
) {
}

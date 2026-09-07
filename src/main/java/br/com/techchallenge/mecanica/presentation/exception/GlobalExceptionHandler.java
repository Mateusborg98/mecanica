package br.com.techchallenge.mecanica.presentation.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.techchallenge.mecanica.domain.exception.ClienteNaoEncontradoException;
import br.com.techchallenge.mecanica.domain.exception.CpfDuplicadoException;
import br.com.techchallenge.mecanica.domain.exception.CpfInvalidoException;
import br.com.techchallenge.mecanica.domain.exception.EstoqueNaoEncontradoException;
import br.com.techchallenge.mecanica.domain.exception.OperadorNaoEncontradoException;
import br.com.techchallenge.mecanica.domain.exception.OrdemDeServicoNaoEncontradaException;
import br.com.techchallenge.mecanica.domain.exception.PecaNaoEncontradaException;
import br.com.techchallenge.mecanica.domain.exception.PlacaInvalidaException;
import br.com.techchallenge.mecanica.domain.exception.QuantidadeEstoqueException;
import br.com.techchallenge.mecanica.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.domain.exception.ServicoNaoEncontradoException;
import br.com.techchallenge.mecanica.domain.exception.VeiculoDuplicadoException;
import br.com.techchallenge.mecanica.domain.exception.VeiculoNaoEncontradoException;
import br.com.techchallenge.mecanica.presentation.dto.erro.ErroResponse;
import br.com.techchallenge.mecanica.infrastructure.observability.OrdemDeServicoMetrics;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final OrdemDeServicoMetrics metrics;

    @ExceptionHandler({
            ClienteNaoEncontradoException.class,
            EstoqueNaoEncontradoException.class,
            OperadorNaoEncontradoException.class,
            OrdemDeServicoNaoEncontradaException.class,
            PecaNaoEncontradaException.class,
            ServicoNaoEncontradoException.class,
            VeiculoNaoEncontradoException.class
    })
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RuntimeException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({ CpfDuplicadoException.class, VeiculoDuplicadoException.class })
    public ResponseEntity<ErroResponse> tratarConflito(RuntimeException ex, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler({
            CpfInvalidoException.class,
            PlacaInvalidaException.class,
            QuantidadeEstoqueException.class,
            RegraNegocioException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RuntimeException ex, HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return resposta(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos", request, campos);
    }

    private ResponseEntity<ErroResponse> resposta(
            HttpStatus status, String mensagem, HttpServletRequest request, Map<String, String> campos) {
        if (request.getRequestURI().startsWith("/ordens-servico")) {
            metrics.registrarFalha(request.getRequestURI(), status.value());
            LOGGER.error(
                    "Falha no processamento da ordem | path={} | status={} | category={}",
                    request.getRequestURI(),
                    status.value(),
                    "HTTP_" + status.value());
        }
        return ResponseEntity.status(status).body(new ErroResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                campos));
    }

}

package org.example.meetingroomreservation.shared.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Traduce excepciones a respuestas {@code application/problem+json}.
 *
 * <p>Los mensajes no revelan trazas, SQL ni nombres de clases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException e, WebRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();

        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.add(new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage())));
        e.getBindingResult().getGlobalErrors().forEach(globalError ->
                errors.add(new FieldErrorDetail(globalError.getObjectName(), globalError.getDefaultMessage())));

        // Orden estable: con varios campos invalidos, el orden de Hibernate Validator no esta
        // garantizado y haria fragiles los tests.
        errors.sort(Comparator.comparing(FieldErrorDetail::field));

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "La peticion contiene campos invalidos", ProblemTypes.VALIDATION_ERROR, request);
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * JSON sintacticamente invalido, cuerpo ausente, o un valor que no encaja con el tipo
     * destino, como un rol fuera del enumerado. En estos casos Jackson falla antes de poder
     * atribuir el fallo a un campo, asi que no se devuelve el array de errores.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException e, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion no se puede interpretar",
                ProblemTypes.MALFORMED_REQUEST, request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailExists(EmailAlreadyExistsException e, WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), ProblemTypes.EMAIL_ALREADY_EXISTS,
                request);
    }

    /**
     * Las invariantes del dominio que el validador declarativo no cubre, por ejemplo un correo
     * que excede la longitud tras normalizar.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), ProblemTypes.VALIDATION_ERROR,
                request);
    }

    private ProblemDetail problem(HttpStatus status, String detail, java.net.URI type,
                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(java.net.URI.create(resolvePath(request)));
        return problem;
    }

    private String resolvePath(WebRequest request) {
        Object path = request.getAttribute(
                "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping",
                WebRequest.SCOPE_REQUEST);
        return path != null ? path.toString() : "";
    }
}

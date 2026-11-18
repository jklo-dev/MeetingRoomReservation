package org.example.meetingroomreservation.shared.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import tools.jackson.core.JacksonException;

import org.example.meetingroomreservation.reservation.domain.exception.ReservationExceptions;
import org.example.meetingroomreservation.room.domain.exception.RoomNameAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.exception.EmailAlreadyExistsException;
import org.example.meetingroomreservation.user.domain.exception.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     * Parametro de consulta invalido. Usa la misma forma que la validacion del body, con el
     * array {@code errors}, para que toda la API tenga un unico formato de error de validacion.
     */
    @ExceptionHandler(InvalidQueryParameterException.class)
    ProblemDetail handleInvalidQueryParameter(InvalidQueryParameterException e,
                                             WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "La peticion contiene parametros invalidos", ProblemTypes.VALIDATION_ERROR,
                request);
        problem.setProperty("errors",
                List.of(new FieldErrorDetail(e.field(), e.getMessage())));
        return problem;
    }

    /**
     * Un parametro de consulta numerico que no se puede convertir, por ejemplo
     * {@code minCapacity=abc}. Spring falla al convertir el tipo antes de llegar al controlador.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "La peticion contiene parametros invalidos", ProblemTypes.VALIDATION_ERROR,
                request);
        problem.setProperty("errors",
                List.of(new FieldErrorDetail(e.getName(), "no tiene un valor valido")));
        return problem;
    }

    /**
     * Cuerpo que Jackson no puede deserializar.
     *
     * <p>Se distinguen dos situaciones, porque no son el mismo problema:
     *
     * <ul>
     *   <li><b>El campo se puede identificar</b> —una fecha sin offset, un UUID mal formado, un
     *       texto donde se espera un entero, un valor fuera de un enumerado—: es un error de
     *       validacion de ese campo, asi que se devuelve {@code validation-error} con el array
     *       {@code errors}, igual que la validacion declarativa.</li>
     *   <li><b>El campo no se puede identificar</b> —JSON sintacticamente roto o cuerpo
     *       ausente—: entonces si corresponde {@code malformed-request}, que el catalogo define
     *       justamente como el caso que no permite atribuir el fallo a un campo.</li>
     * </ul>
     *
     * <p>Diferencia con {@code MethodArgumentNotValidException}: alli se acumulan **todos** los
     * campos invalidos, porque Bean Validation recorre el objeto ya construido. Aqui Jackson
     * aborta en el primer campo que no puede convertir, asi que el array lleva una sola entrada.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadable(HttpMessageNotReadableException e, WebRequest request) {
        return campoCulpable(e)
                .map(campo -> {
                    ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                            "La peticion contiene campos invalidos",
                            ProblemTypes.VALIDATION_ERROR, request);
                    problem.setProperty("errors", List.of(new FieldErrorDetail(campo,
                            "no tiene un formato valido")));
                    return problem;
                })
                .orElseGet(() -> problem(HttpStatus.BAD_REQUEST,
                        "El cuerpo de la peticion no se puede interpretar",
                        ProblemTypes.MALFORMED_REQUEST, request));
    }

    /**
     * Extrae de la excepcion de Jackson el nombre del campo que no pudo deserializarse.
     *
     * <p>Recorre la cadena de causas buscando una {@code JacksonException}, cuyo {@code getPath()}
     * describe la ruta hasta el campo. Se toma el ultimo tramo con nombre: en estructuras
     * anidadas, es el campo concreto que falla y no el objeto que lo contiene.
     */
    private Optional<String> campoCulpable(Throwable e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof JacksonException jackson) {
                return jackson.getPath().stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .filter(nombre -> nombre != null && !nombre.isBlank())
                        .reduce((primero, ultimo) -> ultimo);
            }
            causa = causa.getCause();
        }
        return Optional.empty();
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handleEmailExists(EmailAlreadyExistsException e, WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), ProblemTypes.EMAIL_ALREADY_EXISTS,
                request);
    }

    @ExceptionHandler(RoomNameAlreadyExistsException.class)
    ProblemDetail handleRoomNameExists(RoomNameAlreadyExistsException e, WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(),
                ProblemTypes.ROOM_NAME_ALREADY_EXISTS, request);
    }

    // --- Reservas: cada causa con su propio tipo, aunque compartan codigo HTTP ---

    @ExceptionHandler(ReservationExceptions.InvalidTimeRange.class)
    ProblemDetail handleInvalidTimeRange(ReservationExceptions.InvalidTimeRange e,
                                         WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), ProblemTypes.INVALID_TIME_RANGE,
                request);
    }

    @ExceptionHandler(ReservationExceptions.MustEndInFuture.class)
    ProblemDetail handleMustEndInFuture(ReservationExceptions.MustEndInFuture e,
                                        WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(),
                ProblemTypes.RESERVATION_MUST_END_IN_FUTURE, request);
    }

    @ExceptionHandler(ReservationExceptions.DurationOutOfRange.class)
    ProblemDetail handleDurationOutOfRange(ReservationExceptions.DurationOutOfRange e,
                                           WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(),
                ProblemTypes.RESERVATION_DURATION_OUT_OF_RANGE, request);
    }

    @ExceptionHandler(ReservationExceptions.AttendeesExceedCapacity.class)
    ProblemDetail handleAttendeesExceedCapacity(ReservationExceptions.AttendeesExceedCapacity e,
                                                WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(),
                ProblemTypes.ATTENDEES_EXCEED_CAPACITY, request);
    }

    @ExceptionHandler(ReservationExceptions.RoomNotFound.class)
    ProblemDetail handleRoomNotFound(ReservationExceptions.RoomNotFound e, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), ProblemTypes.ROOM_NOT_FOUND, request);
    }

    @ExceptionHandler(ReservationExceptions.RoomNotAvailable.class)
    ProblemDetail handleRoomNotAvailable(ReservationExceptions.RoomNotAvailable e,
                                         WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), ProblemTypes.ROOM_NOT_AVAILABLE,
                request);
    }

    @ExceptionHandler(ReservationExceptions.Overlap.class)
    ProblemDetail handleOverlap(ReservationExceptions.Overlap e, WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(), ProblemTypes.RESERVATION_OVERLAP,
                request);
    }

    /**
     * Fallo transitorio, de ahi el 503 y no un 409: la peticion es valida y reintentarla tiene
     * sentido.
     */
    @ExceptionHandler(ReservationExceptions.RoomBusy.class)
    ProblemDetail handleRoomBusy(ReservationExceptions.RoomBusy e, WebRequest request) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), ProblemTypes.ROOM_BUSY,
                request);
    }

    @ExceptionHandler(ReservationExceptions.NotFound.class)
    ProblemDetail handleReservationNotFound(ReservationExceptions.NotFound e,
                                            WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), ProblemTypes.RESERVATION_NOT_FOUND,
                request);
    }

    /**
     * Comparte tipo con el 403 de rol insuficiente: desde el punto de vista del cliente es el
     * mismo problema, no tener permiso para la operacion. El {@code detail} si precisa la causa.
     */
    @ExceptionHandler(ReservationExceptions.NotAllowedToCancel.class)
    ProblemDetail handleNotAllowedToCancel(ReservationExceptions.NotAllowedToCancel e,
                                           WebRequest request) {
        return problem(HttpStatus.FORBIDDEN, e.getMessage(), ProblemTypes.INSUFFICIENT_ROLE,
                request);
    }

    @ExceptionHandler(ReservationExceptions.AlreadyCancelled.class)
    ProblemDetail handleAlreadyCancelled(ReservationExceptions.AlreadyCancelled e,
                                         WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(),
                ProblemTypes.RESERVATION_ALREADY_CANCELLED, request);
    }

    @ExceptionHandler(ReservationExceptions.AlreadyFinished.class)
    ProblemDetail handleAlreadyFinished(ReservationExceptions.AlreadyFinished e,
                                        WebRequest request) {
        return problem(HttpStatus.CONFLICT, e.getMessage(),
                ProblemTypes.RESERVATION_ALREADY_FINISHED, request);
    }

    /**
     * Correo inexistente y contrasena incorrecta producen esta misma respuesta, sin distinguir
     * la causa: lo contrario permitiria averiguar que correos estan registrados.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException e, WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales invalidas",
                ProblemTypes.INVALID_CREDENTIALS, request);
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

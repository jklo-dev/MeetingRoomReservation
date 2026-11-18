package org.example.meetingroomreservation.user.infrastructure.adapter.in.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.example.meetingroomreservation.user.domain.model.User;
import org.example.meetingroomreservation.user.domain.port.in.CreateUser;
import org.example.meetingroomreservation.user.domain.port.in.CreateUserCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada HTTP para usuarios.
 *
 * <p>La exigencia del rol ADMIN esta declarada en la cadena de seguridad, no aqui, porque no
 * depende de ningun recurso concreto.
 */
@RestController
@RequestMapping("/v1/users")
class UserController {

    private final CreateUser createUser;

    UserController(CreateUser createUser) {
        this.createUser = createUser;
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = createUser.create(new CreateUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.role()));

        return ResponseEntity
                .created(URI.create("/v1/users/" + created.id()))
                .body(UserResponse.fromDomain(created));
    }
}

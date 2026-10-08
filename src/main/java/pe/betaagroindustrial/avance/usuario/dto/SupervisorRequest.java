package pe.betaagroindustrial.avance.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta/edicion de un supervisor. Solo el nombre es obligatorio: un
 * supervisor normalmente no inicia sesion en el sistema.
 *
 * email y password son opcionales y solo se usan al CREAR, y solo si ese
 * supervisor va a iniciar sesion (deben venir juntos). Al editar se ignoran.
 */
public record SupervisorRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @Email @Size(max = 150) String email,
        @Size(min = 8, max = 100) String password
) {
}

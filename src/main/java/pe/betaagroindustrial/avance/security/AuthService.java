package pe.betaagroindustrial.avance.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import pe.betaagroindustrial.avance.security.dto.LoginRequest;
import pe.betaagroindustrial.avance.security.dto.LoginResponse;
import pe.betaagroindustrial.avance.security.jwt.JwtService;
import pe.betaagroindustrial.avance.usuario.Usuario;
import pe.betaagroindustrial.avance.usuario.UsuarioRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticacion"));

        String token = jwtService.generarToken(usuario, Map.of(
                "userId", usuario.getId(),
                "rol", usuario.getRol().name()
        ));

        return new LoginResponse(
                token,
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                usuario.getRol().name()
        );
    }
}

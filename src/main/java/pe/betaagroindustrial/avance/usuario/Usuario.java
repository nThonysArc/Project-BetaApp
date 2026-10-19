package pe.betaagroindustrial.avance.usuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.betaagroindustrial.avance.common.audit.Auditable;

import java.util.Collection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Permisos marcados en la checklist del usuario. EAGER porque se leen en cada request al
     * armar las autoridades (fuera de cualquier sesion de Hibernate). Para SUPER_ADMIN esta
     * coleccion se ignora: tiene todos los permisos.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permiso", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "permiso", nullable = false, length = 60)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Permiso> permisos = new HashSet<>();

    /** Permisos reales: todos para SUPER_ADMIN, los marcados para el resto. */
    public Set<Permiso> permisosEfectivos() {
        return rol == Rol.SUPER_ADMIN ? EnumSet.allOf(Permiso.class)
                : (permisos.isEmpty() ? EnumSet.noneOf(Permiso.class) : EnumSet.copyOf(permisos));
    }

    // ===== UserDetails =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        permisosEfectivos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p.authority())));
        return autoridades;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return activo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}

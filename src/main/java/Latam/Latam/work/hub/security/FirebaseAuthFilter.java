package Latam.Latam.work.hub.security;

import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    @Autowired
    private FirebaseRoleService firebaseRoleService;

    @Autowired
    private SecurityPathsConfig securityPathsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Verificamos si es una ruta pública
        if (securityPathsConfig.isPublicPath(path)) {
            log.debug("Ruta pública: {}, saltando autenticación", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Obtenemos el token del encabezado Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Token no presente o con formato incorrecto para: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);

        try {
            // Verificamos el token y obtenemos la información del usuario
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(idToken);
            log.debug("Token verificado para usuario: {} con rol: {}", userInfo.getUid(), userInfo.getRole());

            // Creamos la autoridad basada en el rol
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userInfo.getRole());

            // Establecemos la autenticación en el contexto de seguridad
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userInfo.getUid(),
                    null,
                    Collections.singletonList(authority)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Autenticación establecida para usuario: {}", userInfo.getUid());

        } catch (FirebaseAuthException e) {
            log.error("Error al verificar token de Firebase: {}", e.getMessage());
            // No establecemos autenticación en caso de error
        } catch (Exception e) {
            log.error("Error inesperado al procesar autenticación: {}", e.getMessage(), e);
            // No establecemos autenticación en caso de error
        }

        filterChain.doFilter(request, response);
    }
}
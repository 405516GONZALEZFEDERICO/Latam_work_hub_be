package Latam.Latam.work.hub.security;

import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.security.dtos.FirebaseUserInfoDto;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseRoleService firebaseRoleService;
    private final SecurityPathsConfig securityPathsConfig;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.info("Procesando solicitud: " + request.getMethod() + " " + request.getRequestURI());

        String path = request.getRequestURI();

        if (securityPathsConfig.isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);

        if (token == null) {
            sendUnauthorizedResponse(response, "Token no proporcionado");
            return;
        }

        try {
            // Obtener información básica del token
            String email = firebaseRoleService.getEmailFromToken(token);

            // Verificar si es admin en la base de datos
            if (userRepository.existsByEmailAndRole("admin@latam.com", "ADMIN")) {
                // Configurar autenticación para admin sin verificar en Firebase
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("firebaseEmail", email);
                request.setAttribute("firebaseRole", "ADMIN");

                filterChain.doFilter(request, response);
                return;
            }

            // Para otros roles, realizar verificación completa con Firebase
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRol(token);

            request.setAttribute("firebaseEmail", userInfo.getEmail());
            request.setAttribute("firebaseUid", userInfo.getUid());
            request.setAttribute("firebaseRole", userInfo.getRole());

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole()));

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userInfo.getEmail(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException e) {
            log.error("Error de autenticación Firebase: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Token inválido");
        } catch (Exception e) {
            log.error("Error de autenticación: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Error de autenticación");
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "No autorizado");
        errorResponse.put("message", message);

        String jsonResponse = new ObjectMapper().writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
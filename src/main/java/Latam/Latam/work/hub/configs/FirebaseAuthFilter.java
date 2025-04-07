package Latam.Latam.work.hub.configs;


import Latam.Latam.work.hub.dtos.FirebaseUserInfoDto;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseRoleService firebaseRoleService;

    private final List<String> openPaths = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/google",
            "/api/auth/refresh-token",
            "/api/auth/recover-password",
            "/swagger-ui",
            "/v3/api-docs"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filter for open paths
        if (isOpenPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the token from the Authorization header
        String token = getTokenFromRequest(request);

        if (token == null) {
            sendUnauthorizedResponse(response, "Token no proporcionado");
            return;
        }

        try {
            // Verify the token and extract user information
            FirebaseUserInfoDto userInfo = firebaseRoleService.verificarRolYPermisos(token);

            // Set attributes in request for downstream use
            request.setAttribute("firebaseEmail", userInfo.getEmail());
            request.setAttribute("firebaseUid", userInfo.getUid());
            request.setAttribute("firebaseRole", userInfo.getRole());

            // Set up Spring Security context
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // Add role as an authority
            authorities.add(new SimpleGrantedAuthority("ROLE_" + userInfo.getRole()));

            // Add each permission as an authority
            if (userInfo.getPermissions() != null) {
                for (String permission : userInfo.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission));
                }
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userInfo.getEmail(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Continue with the filter chain
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

    private boolean isOpenPath(String path) {
        return openPaths.stream().anyMatch(path::startsWith);
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




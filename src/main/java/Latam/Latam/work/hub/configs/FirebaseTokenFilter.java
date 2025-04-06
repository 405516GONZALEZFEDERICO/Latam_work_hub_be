package Latam.Latam.work.hub.configs;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/aut/verificar-rol",
            "/api/auth/google/login",
            "/api/auth/google/register",
            "/api/auth/google/refresh"

    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        logger.info("Path: {}, isPublic: {}", path, isPublicPath);
        return isPublicPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String idToken = authorizationHeader.substring(7);

            try {
                // Verifica el token con Firebase
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

                // Almacena el token y el email en la solicitud para acceso posterior
                request.setAttribute("firebaseUser", decodedToken);
                request.setAttribute("firebaseEmail", decodedToken.getEmail());

                // Extraer rol desde los claims o usar "USER"
                String role = (String) decodedToken.getClaims().getOrDefault("role", "USER");

                // Extraer permisos si existen
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                Object permissionsObj = decodedToken.getClaims().get("permissions");
                if (permissionsObj instanceof List) {
                    List<String> permissions = (List<String>) permissionsObj;
                    for (String permission : permissions) {
                        authorities.add(new SimpleGrantedAuthority(permission)); // Sin prefijo
                    }
                }

                // Crear Authentication y setear en SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                decodedToken.getEmail(),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Logs de depuración
                logger.info("Token procesado correctamente: {}", decodedToken.getUid());
                logger.info("Claims: {}", decodedToken.getClaims());
                logger.info("Authorities: {}", authorities);

            } catch (FirebaseAuthException e) {
                logger.error("Error al verificar el token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            logger.info("No se encontró token de autorización");
        }

        filterChain.doFilter(request, response);
    }
}


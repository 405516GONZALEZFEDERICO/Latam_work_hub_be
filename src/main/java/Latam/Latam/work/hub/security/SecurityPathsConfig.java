package Latam.Latam.work.hub.security;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SecurityPathsConfig {


    public static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/verificar-rol",
            "/api/auth/google/**",
            "/api/auth/recuperar-contrasenia",
            "/api/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/payments/notifications/**",
            "/api/reports-admin/**"
    );

    public boolean isPublicPath(String path) {
                return PUBLIC_PATHS.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String basePath = pattern.substring(0, pattern.length() - 3);
                return path.startsWith(basePath);
            }
            return path.equals(pattern);
        });
    }
}
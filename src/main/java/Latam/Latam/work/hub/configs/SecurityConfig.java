package Latam.Latam.work.hub.configs;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final CorsConfig corsConfig;

    public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter, CorsConfig corsConfig) {
        this.firebaseTokenFilter = firebaseTokenFilter;
        this.corsConfig = corsConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Utiliza el método moderno para deshabilitar CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Configura CORS
                .cors(Customizer.withDefaults())
                // Configura las reglas de autorización
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas que no requieren autenticación
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/aut/verificar-rol",
                                "/api/auth/google/login", "/api/auth/google/register", "/api/auth/google/refresh",
                                "/api/public/**")
                        .permitAll()
                        // Todas las demás rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                // Agrega el filtro personalizado antes del filtro de autenticación estándar
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

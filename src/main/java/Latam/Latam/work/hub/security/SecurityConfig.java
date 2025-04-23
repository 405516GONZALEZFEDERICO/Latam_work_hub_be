package Latam.Latam.work.hub.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;
    private final SecurityPathsConfig securityPathsConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas que no requieren autenticación
                        .requestMatchers(securityPathsConfig.PUBLIC_PATHS.toArray(new String[0]))
                        .permitAll()
                        .requestMatchers("/api/auth/roles/assign").hasAnyRole("DEFAULT", "ADMIN")

                        // Rutas exclusivas para ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/users/personal-data").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/users/*/upload-img").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/users/get-personal-data").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/location/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/company/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/payments/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/spaces").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/spaces/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/amenities/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

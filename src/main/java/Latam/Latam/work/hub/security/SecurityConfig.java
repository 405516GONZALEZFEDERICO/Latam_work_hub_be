package Latam.Latam.work.hub.security;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private  FirebaseAuthFilter firebaseAuthFilter;
    private final SecurityPathsConfig securityPathsConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityPathsConfig.PUBLIC_PATHS.toArray(new String[0]))
                        .permitAll()
                        .requestMatchers("/api/auth/roles/assign").hasRole("DEFAULT")
                        .requestMatchers("api/users/get-user-list").hasRole("ADMIN")
                        .requestMatchers("/api/users/activate-account/**").hasAnyRole("CLIENTE", "PROVEEDOR", "ADMIN")
                        .requestMatchers("/api/users/activate-account").hasAnyRole("CLIENTE", "PROVEEDOR", "ADMIN")
                        .requestMatchers("api/spaces/spaces-list").hasRole("ADMIN")
                        .requestMatchers("/api/users/personal-data").hasAnyRole("CLIENTE", "PROVEEDOR","ADMIN")
                        .requestMatchers("/api/users/*/upload-img").hasAnyRole("CLIENTE", "PROVEEDOR","ADMIN")
                        .requestMatchers("/api/users/get-personal-data").hasAnyRole("CLIENTE", "PROVEEDOR","ADMIN")
                        .requestMatchers("/api/users/*/get-provider-type").hasRole("PROVEEDOR")
                        .requestMatchers("/api/booking/**").hasRole("CLIENTE")
                        .requestMatchers("/api/rental-contracts/**").hasRole("CLIENTE")
                        .requestMatchers("/api/payments/**").hasRole("CLIENTE")
                        .requestMatchers("/api/location/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/company/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/payments/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/spaces").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/spaces/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/spaces/provider/spaces").hasRole("PROVEEDOR")
                        .requestMatchers("/api/amenities/*").hasAnyRole("CLIENTE", "PROVEEDOR")
                        .requestMatchers("/api/reports").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard-admin/*").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

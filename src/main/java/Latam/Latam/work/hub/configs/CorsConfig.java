package Latam.Latam.work.hub.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost",
                        "http://localhost:80",
                        "http://localhost:4200",
                        "http://localhost:8080",
                        "http://localhost:8081",
                        "http://127.0.0.1:5500",
                        "https://latam-work-hub-fe.vercel.app",
                        "https://latam-work-hub-6l9is20ym-fedes-projects-4260e070.vercel.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("http://localhost:80");
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("http://localhost:8081");
        config.addAllowedOrigin("http://127.0.0.1:5500");
        config.addAllowedOrigin("https://latam-work-hub-fe.vercel.app");
        config.addAllowedOrigin("https://latam-work-hub-6l9is20ym-fedes-projects-4260e070.vercel.app");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
package Latam.Latam.work.hub.configs;

import com.mercadopago.MercadoPagoConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {
    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Bean
    public String initMercadoPagoConfig() {
        MercadoPagoConfig.setAccessToken(accessToken);
        return "MercadoPago configured with access token";
    }
}
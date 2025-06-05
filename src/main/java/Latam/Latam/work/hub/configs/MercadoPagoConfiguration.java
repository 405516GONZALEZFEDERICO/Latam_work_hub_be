package Latam.Latam.work.hub.configs;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.merchantorder.MerchantOrderClient;
import com.mercadopago.client.payment.PaymentCancelRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
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

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }

    @Bean
    public MerchantOrderClient merchantOrderClient() {
        return new MerchantOrderClient();
    }

    @Bean
    public PaymentRefundClient paymentRefundClient() {
        return new PaymentRefundClient();
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }
    @Bean
    public PaymentCancelRequest cancelRequest() {
        return PaymentCancelRequest.builder()
                .build();
    }
}
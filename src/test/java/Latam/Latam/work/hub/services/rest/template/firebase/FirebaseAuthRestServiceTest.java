package Latam.Latam.work.hub.services.rest.template.firebase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class FirebaseAuthRestServiceTest {
    @MockitoBean
    private RestTemplate restTemplate;
    @MockitoSpyBean
    private FirebaseAuthRestService firebaseAuthRestService;

    @Test
    void signInWithEmailAndPassword() {
    }

    @Test
    void sendPasswordResetEmail() {
    }
}
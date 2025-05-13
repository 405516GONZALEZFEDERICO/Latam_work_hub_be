package Latam.Latam.work.hub.security;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.config}")
    private String firebaseConfigJson;

    @PostConstruct
    public void initFirebase() {
        try {
            // Add debugging to see the actual JSON content
            System.out.println("Firebase config JSON (first 100 chars): " +
                    (firebaseConfigJson.length() > 100 ?
                            firebaseConfigJson.substring(0, 100) + "..." :
                            firebaseConfigJson));

            // Remove any potential outer quotes that might be wrapping the JSON
            String cleanJson = firebaseConfigJson;
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1);
            }

            // Replace escaped quotes with actual quotes
            cleanJson = cleanJson.replace("\\\"", "\"");

            ByteArrayInputStream serviceAccount = new ByteArrayInputStream(cleanJson.getBytes());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase inicializado");
            }

        } catch (IOException e) {
            System.err.println("Firebase initialization IO error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
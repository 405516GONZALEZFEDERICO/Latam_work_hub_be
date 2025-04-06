package Latam.Latam.work.hub.services.impl;


import Latam.Latam.work.hub.dtos.AuthResponseDto;
import Latam.Latam.work.hub.dtos.UserDto;
import Latam.Latam.work.hub.entities.UserEntity;
import Latam.Latam.work.hub.repositories.UserRepository;
import Latam.Latam.work.hub.services.AuthService;
import Latam.Latam.work.hub.services.FirebaseRoleService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final FirebaseRoleService firebaseRoleService;
    private final ModelMapper modelMapper;

    @Value("${firebase.api.key}")
    private String firebaseApiKey;



    @Override
    public String syncUser(HttpServletRequest request) {
        String email = (String) request.getAttribute("firebaseEmail");

        if (email == null) {
            throw new RuntimeException("Email no encontrado en el token");
        }

        Optional<UserEntity> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            UserEntity newUser = new UserEntity();
            newUser.setEmail(email);
            newUser.setEnabled(true);
            userRepository.save(newUser);
        }

        return "Usuario sincronizado correctamente";
    }

    @Override
    public UserDto obtenerPerfil(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No autorizado");
        }

        String email = authentication.getName();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return modelMapper.map(user, UserDto.class);
    }





    @Override
    public String registrarUsuario(String email, String password) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password);
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);

            createLocalUserFromFirebase(email, userRecord.getUid());
            firebaseRoleService.asignarRolYPermisosAFirebaseUser(userRecord.getUid(), "CLIENTE");

            return "Usuario registrado con UID: " + userRecord.getUid();
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al registrar usuario: " + e.getMessage());
        }
    }


    @Override
    public AuthResponseDto login(String email, String password) {
        try {
            boolean userExistsLocally = false;
            UserEntity user = null;

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
                userExistsLocally = true;
                if (!user.isEnabled()) {
                    throw new RuntimeException("Usuario deshabilitado");
                }
            }

            RestTemplate restTemplate = new RestTemplate();
            String firebaseAuthUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    firebaseAuthUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();
            String uid = (String) responseBody.get("localId");

            if (!userExistsLocally) {
                user = createLocalUserFromFirebase(email, uid);
            }

            String rol = firebaseRoleService.obtenerRolDeUsuario(uid);

            AuthResponseDto responseDto = new AuthResponseDto();
            responseDto.setIdToken((String) responseBody.get("idToken"));
            responseDto.setRefreshToken((String) responseBody.get("refreshToken"));
            responseDto.setExpiresIn((String) responseBody.get("expiresIn"));
            responseDto.setEmail((String) responseBody.get("email"));
            responseDto.setLocalId(uid);
            responseDto.setRole(rol);

            return responseDto;

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error de autenticación: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Error interno del servidor: " + e.getMessage());
        }
    }

    private UserEntity createLocalUserFromFirebase(String email, String uid) {
        UserEntity newUser = new UserEntity();
        newUser.setEmail(email);
        newUser.setEnabled(true);
        newUser.setFirebaseUid(uid);
        newUser.setLastPasswordUpdateDate(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    @Override
    public String logout(String idToken) {
        try {
            String uid = FirebaseAuth.getInstance().verifyIdToken(idToken).getUid();
            FirebaseAuth.getInstance().revokeRefreshTokens(uid);
            return "Sesión cerrada correctamente (tokens revocados)";
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Error al cerrar sesión: " + e.getMessage());
        }
    }

    @Override
    public String recuperarContrasenia(String email) {
        try {
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("El correo electrónico no está registrado en el sistema");
            }
            UserEntity user = userOpt.get();
            if (!user.isEnabled()) {
                throw new RuntimeException("Esta cuenta está deshabilitada");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("requestType", "PASSWORD_RESET");
            body.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            String url = "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseApiKey;

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return "Correo de recuperación enviado correctamente";
            } else {
                throw new RuntimeException("No se pudo enviar el correo de recuperación");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al enviar correo de recuperación: " + e.getMessage());
        }
    }








}

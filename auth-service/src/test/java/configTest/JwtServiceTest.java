package configTest;

import com.bidly.authservice.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {
    private JwtService jwtService;
    private UserDetails dummyUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        dummyUser = new User("andreiBiddly", "password1234", new ArrayList<>());
    }

    @Test
    void testGenerateAndExtractUsername() {
        String token = jwtService.generateToken(dummyUser);
        assertNotNull(token);
        System.out.println("Generated Token: " + token);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("andreiBiddly", extractedUsername);

        boolean isValid = jwtService.isTokenValid(token, dummyUser);
        assertTrue(isValid);
    }

}

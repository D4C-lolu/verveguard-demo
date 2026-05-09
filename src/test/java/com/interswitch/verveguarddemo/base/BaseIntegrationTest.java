package com.interswitch.verveguarddemo.base;

import com.interswitch.verveguarddemo.config.SyncAsyncConfig;
import com.interswitch.verveguarddemo.config.TestcontainersConfiguration;
import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.entities.User;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import com.interswitch.verveguarddemo.security.MerchantPrincipal;
import com.interswitch.verveguarddemo.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, SyncAsyncConfig.class})
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MerchantRepository merchantRepository;

    protected String uniqueIp() {
        int counter = (int) (System.nanoTime() % 900000) + 100000;
        return "10.0." + (counter / 1000) + "." + (counter % 255);
    }

    protected void authenticateAsMerchant(String email) {
        Merchant merchant = merchantRepository.findByEmail(email).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new MerchantPrincipal((merchant)), null, List.of())
        );
    }

    protected void authenticateAsUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of())
        );
    }

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames()
                .forEach(name -> {
                    var cache = cacheManager.getCache(name);
                    if (cache != null) cache.clear();
                });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    protected void forceFlush() {
        entityManager.flush();
        entityManager.clear();
    }
}

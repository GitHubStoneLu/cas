package org.apereo.cas.config;

import org.apereo.cas.CipherExecutor;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.AuthenticationCredentialTypeMetaDataPopulator;
import org.apereo.cas.authentication.metadata.CacheCredentialsCipherExecutor;
import org.apereo.cas.authentication.metadata.CacheCredentialsMetaDataPopulator;
import org.apereo.cas.authentication.metadata.RememberMeAuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.SuccessfulHandlerMetaDataPopulator;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.clearpass.ClearpassProperties;
import org.apereo.cas.util.cipher.NoOpCipherExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link CasCoreAuthenticationMetadataConfiguration}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 5.1.0
 */
@Configuration("casCoreAuthenticationMetadataConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CasCoreAuthenticationMetadataConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(CasCoreAuthenticationMetadataConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @ConditionalOnMissingBean(name = "successfulHandlerMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator successfulHandlerMetaDataPopulator() {
        return new SuccessfulHandlerMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "rememberMeAuthenticationMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator rememberMeAuthenticationMetaDataPopulator() {
        return new RememberMeAuthenticationMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "cacheCredentialsCipherExecutor")
    @Bean
    public CipherExecutor cacheCredentialsCipherExecutor() {
        final ClearpassProperties cp = casProperties.getClearpass();
        if (cp.isCacheCredential()) {
            if (cp.getCrypto().isEnabled()) {
                return new CacheCredentialsCipherExecutor(cp.getCrypto().getEncryption().getKey(),
                    cp.getCrypto().getSigning().getKey(),
                    cp.getCrypto().getAlg());
            }
            LOGGER.warn("Cas is configured to capture and cache credentials via Clearpass yet crypto operations for the cached password are "
                + "turned off. Consider enabling the crypto configuration in CAS settings that allow the system to sign & encrypt the captured credential.");
        }
        return NoOpCipherExecutor.getInstance();
    }

    @ConditionalOnMissingBean(name = "authenticationCredentialTypeMetaDataPopulator")
    @Bean
    public AuthenticationMetaDataPopulator authenticationCredentialTypeMetaDataPopulator() {
        return new AuthenticationCredentialTypeMetaDataPopulator();
    }

    @ConditionalOnMissingBean(name = "casCoreAuthenticationMetadataAuthenticationEventExecutionPlanConfigurer")
    @Bean
    public AuthenticationEventExecutionPlanConfigurer casCoreAuthenticationMetadataAuthenticationEventExecutionPlanConfigurer() {
        return plan -> {
            plan.registerMetadataPopulator(successfulHandlerMetaDataPopulator());
            plan.registerMetadataPopulator(rememberMeAuthenticationMetaDataPopulator());
            plan.registerMetadataPopulator(authenticationCredentialTypeMetaDataPopulator());

            final ClearpassProperties cp = casProperties.getClearpass();
            if (cp.isCacheCredential()) {
                LOGGER.warn("Cas is configured to capture and cache credentials via Clearpass. Sharing the user credential with other applications "
                    + "is generally NOT recommended and may lead to security vulnerabilities.");
                plan.registerMetadataPopulator(new CacheCredentialsMetaDataPopulator(cacheCredentialsCipherExecutor()));
            }
        };
    }
}

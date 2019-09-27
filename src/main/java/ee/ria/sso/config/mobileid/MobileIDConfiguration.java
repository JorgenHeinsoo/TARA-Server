package ee.ria.sso.config.mobileid;

import ee.ria.sso.service.mobileid.MobileIDAuthenticationClient;
import ee.ria.sso.service.mobileid.MobileIDAuthenticationService;
import ee.ria.sso.service.mobileid.rest.MobileIDRESTAuthClient;
import ee.ria.sso.service.mobileid.soap.MobileIDAuthenticatorWrapper;
import ee.ria.sso.service.mobileid.soap.MobileIDSOAPAuthClient;
import ee.ria.sso.statistics.StatisticsHandler;
import ee.sk.mid.MidClient;
import ee.sk.mid.rest.MidLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty("mobile-id.enabled")
@Configuration
@Slf4j
public class MobileIDConfiguration {

    @Autowired
    private StatisticsHandler statisticsHandler;

    @Autowired
    private MobileIDConfigurationProvider configurationProvider;

    @Bean
    public MobileIDAuthenticationClient constructAuthenticationClient() {
        if (configurationProvider.isUseDdsService()) {
            log.info("Initializing SOAP protocol based authentication client for DDS Mobile-ID service");
            return new MobileIDSOAPAuthClient(mobileIDAuthenticatorWrapper());
        } else {
            log.info("Initializing REST protocol based authentication client for Mobile-ID REST service");
            return new MobileIDRESTAuthClient(configurationProvider, midClient());
        }
    }

    @Bean
    public MobileIDAuthenticationService mobileIDAuthenticationService() {
        return new MobileIDAuthenticationService(
                statisticsHandler, configurationProvider, constructAuthenticationClient());
    }

    private MobileIDAuthenticatorWrapper mobileIDAuthenticatorWrapper() {
        MobileIDAuthenticatorWrapper authenticator = new MobileIDAuthenticatorWrapper();
        authenticator.setDigidocServiceURL(configurationProvider.getHostUrl());
        authenticator.setLoginMessage(configurationProvider.getMessageToDisplay());
        authenticator.setServiceName(configurationProvider.getServiceName());
        return authenticator;
    }

    private MidClient midClient() {
        return MidClient.newBuilder()
                .withHostUrl(configurationProvider.getHostUrl())
                .withRelyingPartyUUID(configurationProvider.getRelyingPartyUuid())
                .withRelyingPartyName(configurationProvider.getRelyingPartyName())
                .withNetworkConnectionConfig(clientConfig())
                .withLongPollingTimeoutSeconds(configurationProvider.getSessionStatusSocketOpenDuration())
                .build();
    }

    private ClientConfig clientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, configurationProvider.getConnectionTimeout());
        clientConfig.property(ClientProperties.READ_TIMEOUT, configurationProvider.getReadTimeout());
        clientConfig.register(new MidLoggingFilter());
        return clientConfig;
    }
}
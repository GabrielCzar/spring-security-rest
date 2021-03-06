package com.gabrielczar.springrestoauth2jwt.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
public class OAuth2ServerConfiguration {
    private final String GRANT_TYPE_AUTH_CODE = "authorization_code";
    private final String GRANT_TYPE_REFRESH = "refresh_token";
    private final String GRANT_TYPE_PASSWORD = "password";
    private final String SCOPE_WRITE = "write";
    private final String SCOPE_READ = "read";

    @Value("${security.oauth2.resource.jwt.signing-key:mw5BG4UXKAZKznRbRd3tHXf7hfjKfPY1}")
    private String signingKey;

    @Value("${security.oauth2.resource.id:DA2D532D1BEjwtresourceid}")
    private String resourceId;

    @Value("${security.oauth2.client.client-id:spring-rest-oauth2-jwt}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret:B6813193F1D7EC8BF5B40}")
    private String clientSecret;

    @Value("${security.oauth2.client.access-token-validity-seconds:604800}")
    private int accessTokenValidity;

    @Value("${security.oauth2.client.refresh-token-validity-seconds:864000}")
    private int refreshTokenValidity;

    @Configuration
    @EnableResourceServer
    class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        public void configure(ResourceServerSecurityConfigurer resource) {
            resource.resourceId(resourceId);
        }

        public void configure(HttpSecurity http) throws Exception {
            http
                    .logout()
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .and().authorizeRequests()
                    .antMatchers("/api/public/**").permitAll()
                    .antMatchers("/api/**").hasAnyRole("ADMIN", "USER")
                    .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().denyAll();
        }
    }

    @Configuration
    @EnableAuthorizationServer
    class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
        private final AuthenticationManager authenticationManager;

        @Autowired
        public AuthorizationServerConfiguration(AuthenticationManager authenticationManager) {
            this.authenticationManager = authenticationManager;
        }

        @Override
        public void configure(ClientDetailsServiceConfigurer configurer) throws Exception {
            configurer
                    .inMemory()
                    .withClient(clientId)
                    .secret(clientSecret)
                    .authorizedGrantTypes(GRANT_TYPE_PASSWORD, GRANT_TYPE_REFRESH, GRANT_TYPE_AUTH_CODE)
                    .scopes(SCOPE_READ, SCOPE_WRITE)
                    .resourceIds(resourceId);
        }

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
            endpoints
                    .authenticationManager(authenticationManager)
                    .tokenServices(tokenServices())
                    .tokenStore(tokenStore())
                    .accessTokenConverter(accessTokenConverter());
        }

        @Bean
        public JwtAccessTokenConverter accessTokenConverter() {
            JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
            converter.setSigningKey(signingKey);
            return converter;
        }

        @Bean
        public TokenStore tokenStore() {
            return new JwtTokenStore(accessTokenConverter());
        }

        @Bean
        @Primary
        public DefaultTokenServices tokenServices() {
            DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
            defaultTokenServices.setTokenStore(tokenStore());
            defaultTokenServices.setSupportRefreshToken(true);
            defaultTokenServices.setTokenEnhancer(accessTokenConverter());
            defaultTokenServices.setAccessTokenValiditySeconds(accessTokenValidity);
            defaultTokenServices.setRefreshTokenValiditySeconds(refreshTokenValidity);
            return defaultTokenServices;
        }
    }
}

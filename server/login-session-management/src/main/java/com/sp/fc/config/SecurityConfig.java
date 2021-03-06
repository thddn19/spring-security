package com.sp.fc.config;

import com.sp.fc.user.service.SpUserService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.*;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.servlet.http.HttpSessionEvent;
import javax.sql.DataSource;
import java.time.LocalDateTime;

@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true) // @PreAuthorize in HomeController
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    RememberMeAuthenticationFilter rememberMeAuthenticationFilter;
    TokenBasedRememberMeServices tokenBasedRememberMeServices;
    PersistentTokenBasedRememberMeServices persistentTokenBasedRememberMeServices;

    private final SpUserService userService;
    private final DataSource dataSource;

    public SecurityConfig(DataSource dataSource, SpUserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService); // user ???????????? ?????? ????????? ??????
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // ?????????????????? ????????? ?????? ??????
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        return roleHierarchy;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests(request -> {
                    request
                            .antMatchers("/").permitAll()
                            .anyRequest().authenticated()
                    ;
                })
                .formLogin(
                        login -> login.loginPage("/login").permitAll()
                                .defaultSuccessUrl("/", false)
                                .failureUrl("/login-error")
                )
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(new CustomDeniedHandler())
                        .authenticationEntryPoint(new CustomEntryPoint())
                ) // auth entrypoint??????
                        //.accessDeniedPage("/access-denied")) // handler??? ???????????? ??????????????? ??????
                .rememberMe(r -> r
                        .rememberMeServices(rememberMeServices())) // persistanceTokenBased??? ???????????? ??????.
                .sessionManagement(s-> s
                        //.sessionCreationPolicy(p-> SessionCreationPolicy.ALWAYS)
                        //.sessionFixation(sessionFixationConfigurer -> sessionFixationConfigurer.none())
                        .maximumSessions(1)  // ????????? ??? ?????? ??????
                        .maxSessionsPreventsLogin(false) // ?????? ????????? ????????? ??????, ?????? ????????? ????????????
                        .expiredUrl("/session-expired")  // ????????? ????????? ?????? ???????????? ?????? ?????????
                )
                ;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                // ???????????? ???????????? ?????? ???????????? ??????
                .antMatchers("/sessions", "/session/expire", "/session-expired")
                .requestMatchers(
                        PathRequest.toStaticResources().atCommonLocations(),
                        PathRequest.toH2Console() // ???????????? ?????? h2 ????????? ?????????
                );
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher() {
            @Override
            public void sessionCreated(HttpSessionEvent event) {
                super.sessionCreated(event);
                System.out.printf("===>> [%s] ?????? ????????? %s \n", LocalDateTime.now(), event.getSession().getId());
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent event) {
                super.sessionDestroyed(event);
                System.out.printf("===>> [%s] ?????? ????????? %s \n", LocalDateTime.now(), event.getSession().getId());
            }

            @Override
            public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
                super.sessionIdChanged(event, oldSessionId);
                System.out.printf("===>> [%s] ?????? ????????? ??????  %s:%s \n", LocalDateTime.now(), oldSessionId, event.getSession().getId());
            }
        });
    }

    @Bean
    PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl(); // datasource??? ?????? ????????? ???
        repository.setDataSource(dataSource);
        try {
            repository.removeUserTokens("1"); // ????????? ?????? ??? 
        } catch (Exception e) {
            repository.setCreateTableOnStartup(true); // ????????? ????????? table??? ???????????? ??? (?????? ????????? ????????? ???)
        }
        return repository;
    }

    @Bean
    PersistentTokenBasedRememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices("hello" // key??? ????????????,
                        , userService
                        , tokenRepository());
        services.setAlwaysRemember(true);  // ???????????? httpSecurity??? ?????????????????? ??????????????? ??????????????? ????????? ?????????..?????? ??????????????? ?????????
        return services;
    }

    @Bean
    SessionRegistry sessionRegistry() {  // Impl??? ????????? ????????????
        SessionRegistryImpl registry = new SessionRegistryImpl();
        return registry;
    }
}

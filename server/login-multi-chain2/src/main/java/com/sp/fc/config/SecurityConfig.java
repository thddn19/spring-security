package com.sp.fc.config;

import com.sp.fc.web.student.StudentManager;
import com.sp.fc.web.teacher.TeacherManager;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Order(2)
@EnableWebSecurity(debug = false) // 세션 오류 때문에
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    UsernamePasswordAuthenticationFilter filter;

    private final StudentManager studentManager;
    private final TeacherManager teacherManager;

    // 두 개 들어가도 상관 없음
    public SecurityConfig(StudentManager studentManager, TeacherManager teacherManager) {
        this.studentManager = studentManager;
        this.teacherManager = teacherManager;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // auth provider를 manager에 등록
        auth.authenticationProvider(studentManager);
        auth.authenticationProvider(teacherManager);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CustomLoginFilter filter = new CustomLoginFilter(authenticationManager());
        http
                .authorizeRequests(request ->
                        request.antMatchers("/").permitAll() // formLogin 주석하고 추가
                                .anyRequest().authenticated()
                )
                .formLogin(
                        login -> login.loginPage("/login").permitAll()
                                .failureUrl("/login-error")
                                .defaultSuccessUrl("/", false)
                )

                .addFilterAt(filter, UsernamePasswordAuthenticationFilter.class) // 필터 교체하기
                .logout(
                        logout -> logout.logoutSuccessUrl("/")
                )
                .exceptionHandling(
                        e -> e.accessDeniedPage("/access-denied")
                )
        ;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
        ;
    }
}

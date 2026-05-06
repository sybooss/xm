package com.user.returnsassistant.config;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        OperatorAnno anno = handlerMethod.getMethodAnnotation(OperatorAnno.class);
        if (anno == null) {
            return true;
        }
        authService.requireAdmin(request.getHeader("Authorization"));
        return true;
    }
}

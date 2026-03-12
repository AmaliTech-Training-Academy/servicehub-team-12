package com.servicehub.controller.view;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies shared model attributes to MVC view controllers so common templates,
 * such as the sidebar, can react to the current request without duplicating the
 * same setup in every controller method.
 */
@ControllerAdvice(basePackages = "com.servicehub.controller.view")
public class GlobalViewModelAttributes {

    @ModelAttribute("requestPath")
    public String requestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}

package club.freecity.cms.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 针对 Thymeleaf 视图的异常处理器
 */
@Slf4j
@ControllerAdvice(basePackages = "club.freecity.cms.controller.view")
public class ViewExceptionHandler {

    private static final String DEFAULT_ERROR_VIEW = "error/error";

    /**
     * 处理 404 异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handle404(HttpServletRequest req, NoHandlerFoundException e) {
        log.warn("页面不存在: URL={}", req.getRequestURL());
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 404);
        mav.addObject("url", req.getRequestURL());
        mav.addObject("message", "您访问的页面不存在或已被删除。");
        mav.setViewName(DEFAULT_ERROR_VIEW);
        return mav;
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusinessException(HttpServletRequest req, BusinessException e) {
        log.warn("视图业务异常: URL={}, message={}", req.getRequestURL(), e.getMessage());
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", e.getCode());
        mav.addObject("url", req.getRequestURL());
        mav.addObject("message", e.getMessage());
        mav.setViewName(DEFAULT_ERROR_VIEW);
        return mav;
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(HttpServletRequest req, Exception e) {
        log.error("视图未知异常: URL={}", req.getRequestURL(), e);
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500);
        mav.addObject("url", req.getRequestURL());
        mav.addObject("message", "系统繁忙，请稍后再试");
        mav.setViewName(DEFAULT_ERROR_VIEW);
        return mav;
    }
}

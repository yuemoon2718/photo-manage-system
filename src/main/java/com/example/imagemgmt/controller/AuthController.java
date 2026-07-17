package com.example.imagemgmt.controller;

import com.example.imagemgmt.entity.User;
import com.example.imagemgmt.entity.Image;
import com.example.imagemgmt.repository.UserRepository;
import com.example.imagemgmt.repository.ImageRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {

    private static final String SESSION_USER_KEY = "LOGIN_USER";
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ImageRepository imageRepository;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        Object user = session.getAttribute(SESSION_USER_KEY);
        System.out.println("[Auth] index() sessionId=" + session.getId() + " LOGIN_USER=" + (user == null ? "null" : user.toString()));
        if (user == null) {
            return "redirect:/login";
        }
        
        // 不传递用户图片到轮播图，轮播图只显示默认图片
        // 用户图片将在白色区域内通过AJAX加载显示
        
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(SESSION_USER_KEY) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute(SESSION_USER_KEY) != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          Model model,
                          HttpSession session) {
        System.out.println("[Auth] doLogin() attempt username=" + username + " sessionId=" + session.getId());
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            model.addAttribute("error", "请输入用户名和密码");
            return "login";
        }

        try {
            // 从数据库查找用户
            Optional<User> userOpt = userRepository.findByUsernameAndPassword(username, password);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // 登录成功，将用户信息存储到session中
                session.setAttribute(SESSION_USER_KEY, user);
                System.out.println("[Auth] doLogin() success username=" + username + " userId=" + user.getUserId());
                return "redirect:/";
            } else {
                model.addAttribute("error", "用户名或密码错误");
                System.out.println("[Auth] doLogin() failed username=" + username + " (not found or wrong pwd)");
                return "login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "登录过程中发生错误，请稍后重试");
            return "login";
        }
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                            @RequestParam String password,
                            @RequestParam String email,
                            @RequestParam String confirm,
                            Model model,
                            HttpSession session) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(email)) {
            model.addAttribute("error", "请填写所有必填字段");
            return "register";
        }

        // 验证密码确认
        if (!password.equals(confirm)) {
            model.addAttribute("error", "两次输入的密码不一致");
            return "register";
        }

        try {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                model.addAttribute("error", "用户名已存在");
                return "register";
            }

            // 检查邮箱是否已存在
            if (userRepository.existsByEmail(email)) {
                model.addAttribute("error", "邮箱已被注册");
                return "register";
            }

            // 创建新用户
            User newUser = new User(username, password, email);
            userRepository.save(newUser);

            // 注册成功，自动登录
            session.setAttribute(SESSION_USER_KEY, newUser);
            return "redirect:/";
            
        } catch (Exception e) {
            model.addAttribute("error", "注册过程中发生错误，请稍后重试");
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}



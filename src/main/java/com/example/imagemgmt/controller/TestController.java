package com.example.imagemgmt.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    
    @Value("${app.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${app.thumbnail.path:thumbnails}")
    private String thumbnailPath;
    
    @GetMapping("/api/test-dirs")
    public ResponseEntity<Map<String, Object>> testDirectories() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 创建测试目录
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadDir = Paths.get(System.getProperty("user.dir"), uploadPath, datePath);
            Path thumbnailDir = Paths.get(System.getProperty("user.dir"), thumbnailPath, datePath);
            
            Files.createDirectories(uploadDir);
            Files.createDirectories(thumbnailDir);
            
            response.put("success", true);
            response.put("uploadDir", uploadDir.toAbsolutePath().toString());
            response.put("thumbnailDir", thumbnailDir.toAbsolutePath().toString());
            response.put("uploadDirExists", Files.exists(uploadDir));
            response.put("thumbnailDirExists", Files.exists(thumbnailDir));
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

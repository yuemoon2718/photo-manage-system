package com.example.imagemgmt.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 文件上传工具类
 */
public class FileUploadUtil {
    
    /**
     * 带超时控制的文件保存方法
     * 
     * @param file 要保存的文件
     * @param filePath 目标文件路径
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否保存成功
     */
    public static boolean saveFileWithTimeout(MultipartFile file, Path filePath, int timeoutSeconds) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 确保父目录存在
                Path parentDir = filePath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }
                
                // 使用绝对路径保存文件，避免 Tomcat 相对路径解析问题
                File dest = filePath.toAbsolutePath().toFile();
                file.transferTo(dest);
                return true;
            } catch (IOException e) {
                System.err.println("文件保存失败: " + e.getMessage());
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                System.err.println("文件保存出现未知错误: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
        
        try {
            // 等待指定时间
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("文件保存被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            System.err.println("文件保存执行异常: " + e.getMessage());
            return false;
        } catch (TimeoutException e) {
            System.err.println("文件保存超时: " + e.getMessage());
            future.cancel(true); // 取消任务
            return false;
        }
    }
    
    /**
     * 带重试机制的文件保存方法
     * 
     * @param file 要保存的文件
     * @param filePath 目标文件路径
     * @param maxRetries 最大重试次数
     * @param retryDelaySeconds 重试间隔（秒）
     * @return 是否保存成功
     */
    public static boolean saveFileWithRetry(MultipartFile file, Path filePath, int maxRetries, int retryDelaySeconds) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 确保父目录存在
                Files.createDirectories(filePath.getParent());
                
                // 保存文件
                file.transferTo(filePath.toFile());
                System.out.println("文件保存成功 (尝试 " + attempt + "/" + maxRetries + ")");
                return true;
            } catch (IOException e) {
                System.err.println("文件保存失败 (尝试 " + attempt + "/" + maxRetries + "): " + e.getMessage());
                
                // 如果是最后一次尝试，返回失败
                if (attempt >= maxRetries) {
                    e.printStackTrace();
                    return false;
                }
                
                // 等待一段时间后重试
                try {
                    TimeUnit.SECONDS.sleep(retryDelaySeconds);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            } catch (Exception e) {
                System.err.println("文件保存出现未知错误: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    
    /**
     * 带超时和重试机制的文件保存方法
     * 
     * @param file 要保存的文件
     * @param filePath 目标文件路径
     * @param timeoutSeconds 超时时间（秒）
     * @param maxRetries 最大重试次数
     * @param retryDelaySeconds 重试间隔（秒）
     * @return 是否保存成功
     */
    public static boolean saveFileWithTimeoutAndRetry(MultipartFile file, Path filePath, 
                                                     int timeoutSeconds, int maxRetries, int retryDelaySeconds) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (saveFileWithTimeout(file, filePath, timeoutSeconds)) {
                return true;
            }
            
            System.err.println("文件保存失败 (尝试 " + attempt + "/" + maxRetries + ")");
            
            // 如果是最后一次尝试，返回失败
            if (attempt >= maxRetries) {
                return false;
            }
            
            // 等待一段时间后重试
            try {
                TimeUnit.SECONDS.sleep(retryDelaySeconds);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
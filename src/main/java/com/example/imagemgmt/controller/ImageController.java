package com.example.imagemgmt.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.lang.GeoLocation;
import com.example.imagemgmt.entity.*;
import com.example.imagemgmt.repository.*;
import com.example.imagemgmt.util.FileUploadUtil;
import com.example.imagemgmt.util.LocationUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class ImageController {
    
    private static final String SESSION_USER_KEY = "LOGIN_USER";
    
    @Value("${app.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${app.thumbnail.path:thumbnails}")
    private String thumbnailPath;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private ThumbnailRepository thumbnailRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ImageCategoryRepository imageCategoryRepository;
    
    /**
     * 获取用户的所有分类
     */
    @GetMapping("/api/categories")
    @ResponseBody
    public ResponseEntity<List<Category>> getUserCategories(HttpSession session) {
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<Category> categories = categoryRepository.findByCreatorIdOrderByCategoryName(user.getUserId());
        return ResponseEntity.ok(categories);
    }

    /**
     * 更新图片分类
     */
    @PostMapping("/api/images/{id}/categories")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> updateImageCategories(
            @PathVariable Integer id,
            @RequestBody Map<String, List<String>> payload,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        Optional<Image> imageOpt = imageRepository.findById(id);
        if (!imageOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "图片不存在");
            return ResponseEntity.status(404).body(response);
        }
        
        Image image = imageOpt.get();
        if (!image.getUploaderId().equals(user.getUserId())) {
            response.put("success", false);
            response.put("message", "无权修改此图片");
            return ResponseEntity.status(403).body(response);
        }
        
        List<String> categoryNames = payload.get("categories");
        
        // 删除旧关联
        imageCategoryRepository.deleteByImageId(id);
        
        // 添加新关联
        if (categoryNames != null && !categoryNames.isEmpty()) {
            for (String name : categoryNames) {
                String trimmedName = name.trim();
                if (!StringUtils.hasText(trimmedName)) continue;
                
                // 查找或创建分类
                Category category = categoryRepository.findByCreatorIdAndCategoryName(user.getUserId(), trimmedName)
                        .orElseGet(() -> categoryRepository.save(new Category(user.getUserId(), trimmedName)));
                
                // 创建关联
                ImageCategory ic = new ImageCategory(id, category.getCategoryId());
                imageCategoryRepository.save(ic);
            }
        }
        
        response.put("success", true);
        response.put("message", "分类更新成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 上传图片
     */
    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "categories", required = false) String categories,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 检查用户登录状态
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "请选择要上传的文件");
            return ResponseEntity.badRequest().body(response);
        }
        
        List<Map<String, Object>> uploadedImages = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        
        try {
            // 创建上传目录 - 使用相对路径
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadDir = Paths.get(uploadPath, datePath);
            Path thumbnailDir = Paths.get(thumbnailPath, datePath);
            
            // 确保目录存在
            Files.createDirectories(uploadDir);
            Files.createDirectories(thumbnailDir);
            
            System.out.println("上传目录: " + uploadDir.toAbsolutePath());
            System.out.println("缩略图目录: " + thumbnailDir.toAbsolutePath());
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                // 验证文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    failedFiles.add(file.getOriginalFilename() + " (文件类型不支持)");
                    continue;
                }
                
                // 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String filename = UUID.randomUUID().toString() + extension;
                
                // 保存原图片（带超时和重试机制）
                Path filePath = uploadDir.resolve(filename);
                System.out.println("保存文件到: " + filePath.toAbsolutePath());
                
                boolean uploadSuccess = FileUploadUtil.saveFileWithTimeoutAndRetry(file, filePath, 30, 3, 2); // 30秒超时，最多重试3次，每次间隔2秒
                if (!uploadSuccess) {
                    failedFiles.add(originalFilename + " (保存失败)");
                    continue;
                }
                
                System.out.println("文件保存成功: " + filePath.toAbsolutePath());
                
                // 读取 EXIF 信息
                LocalDateTime captureDate = null;
                String exifLocation = null;
                
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(filePath.toFile());
                    
                    // 读取拍摄时间
                    ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    if (directory != null) {
                        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                        if (date != null) {
                            captureDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                        }
                    }
                    
                    // 读取 GPS 信息
                    GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                    if (gpsDirectory != null) {
                        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                        if (geoLocation != null && !geoLocation.isZero()) {
                            double lat = geoLocation.getLatitude();
                            double lon = geoLocation.getLongitude();
                            // 调用工具类获取中文地址
                            exifLocation = LocationUtils.getAddress(lat, lon);
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("读取 EXIF 信息失败: " + e.getMessage());
                    // 读取 EXIF 失败不影响图片上传
                }
                
                // 获取图片信息
                BufferedImage originalImage = ImageIO.read(filePath.toFile());
                if (originalImage == null) {
                    throw new IOException("无法读取图片文件: " + filePath.toAbsolutePath());
                }
                String resolution = originalImage.getWidth() + "x" + originalImage.getHeight();
                
                // 确定最终使用的地点和时间
                String finalLocation = StringUtils.hasText(location) ? location : exifLocation;
                
                // 创建图片记录 - 使用相对路径
                String imagePath = "/" + uploadPath + "/" + datePath + "/" + filename;
                Image image = new Image(user.getUserId(), finalLocation, resolution, imagePath);
                
                // 如果有拍摄时间，覆盖默认的创建时间
                if (captureDate != null) {
                    image.setCreateDate(captureDate);
                }
                
                image = imageRepository.save(image);
                System.out.println("图片记录已保存: ID=" + image.getImageId());
                
                // 生成缩略图
                String thumbnailFilename = "thumb_" + filename;
                Path thumbnailFilePath = thumbnailDir.resolve(thumbnailFilename);
                System.out.println("正在生成缩略图: " + thumbnailFilePath.toAbsolutePath());
                generateThumbnail(originalImage, thumbnailFilePath.toFile(), 300, 300);
                
                // 创建缩略图记录 - 使用相对路径
                String thumbnailPathStr = "/" + thumbnailPath + "/" + datePath + "/" + thumbnailFilename;
                Thumbnail thumbnail = new Thumbnail(image.getImageId(), thumbnailPathStr);
                thumbnailRepository.save(thumbnail);
                System.out.println("缩略图记录已保存: " + thumbnailPathStr);
                
                // 处理分类
                if (StringUtils.hasText(categories)) {
                    String[] categoryNames = categories.split(",");
                    for (String categoryName : categoryNames) {
                        final String trimmedCategoryName = categoryName.trim();
                        if (StringUtils.hasText(trimmedCategoryName)) {
                            // 查找或创建分类
                            Category category = categoryRepository
                                .findByCreatorIdAndCategoryName(user.getUserId(), trimmedCategoryName)
                                .orElseGet(() -> {
                                    Category newCategory = new Category(user.getUserId(), trimmedCategoryName);
                                    return categoryRepository.save(newCategory);
                                });
                            
                            // 创建图片分类关联
                            if (!imageCategoryRepository.existsByImageIdAndCategoryId(image.getImageId(), category.getCategoryId())) {
                                ImageCategory imageCategory = new ImageCategory(image.getImageId(), category.getCategoryId());
                                imageCategoryRepository.save(imageCategory);
                            }
                        }
                    }
                }
                
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("imageId", image.getImageId());
                imageInfo.put("filename", originalFilename);
                imageInfo.put("storagePath", image.getStoragePath());
                imageInfo.put("resolution", resolution);
                imageInfo.put("location", location);
                imageInfo.put("createDate", image.getCreateDate());
                uploadedImages.add(imageInfo);
            }
            
            response.put("success", true);
            response.put("message", "成功上传 " + uploadedImages.size() + " 张图片" + 
                       (failedFiles.size() > 0 ? "，失败 " + failedFiles.size() + " 张：" + String.join(", ", failedFiles) : ""));
            response.put("images", uploadedImages);
            if (failedFiles.size() > 0) {
                response.put("failedFiles", failedFiles);
            }
            
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细错误信息
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 搜索用户的图片
     */
    @GetMapping("/api/images/search")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> searchUserImages(
            @RequestParam(value = "keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            List<Image> images = imageRepository.searchByKeyword(user.getUserId(), keyword);
            
            // 分页处理
            int start = page * size;
            int end = Math.min(start + size, images.size());
            List<Image> pagedImages = (start < images.size()) ? images.subList(start, end) : new ArrayList<>();
            
            // 转换为前端需要的格式
            List<Map<String, Object>> imageList = new ArrayList<>();
            for (Image image : pagedImages) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("imageId", image.getImageId());
                imageData.put("storagePath", image.getStoragePath());
                imageData.put("location", image.getLocation());
                imageData.put("resolution", image.getResolution());
                imageData.put("createDate", image.getCreateDate());
                imageData.put("filename", "图片" + image.getImageId()); // 默认文件名
                
                // 获取分类
                List<String> categories = new ArrayList<>();
                if (image.getImageCategories() != null) {
                    for (ImageCategory ic : image.getImageCategories()) {
                        if (ic.getCategory() != null) {
                            categories.add(ic.getCategory().getCategoryName());
                        }
                    }
                }
                imageData.put("categories", categories);
                
                imageList.add(imageData);
            }
            
            response.put("success", true);
            response.put("images", imageList);
            response.put("total", images.size());
            response.put("page", page);
            response.put("size", size);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "搜索图片失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户的图片列表
     */
    @GetMapping("/api/images")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> getUserImages(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            List<Image> images;
            if (year != null) {
                // 按时间过滤
                LocalDateTime startDate;
                LocalDateTime endDate;
                
                if (month != null) {
                    // 按年月过滤
                    startDate = LocalDateTime.of(year, month, 1, 0, 0);
                    endDate = startDate.plusMonths(1).minusNanos(1);
                } else {
                    // 按年过滤
                    startDate = LocalDateTime.of(year, 1, 1, 0, 0);
                    endDate = startDate.plusYears(1).minusNanos(1);
                }
                
                images = imageRepository.findByUploaderIdAndCreateDateBetweenOrderByCreateDateDesc(
                        user.getUserId(), startDate, endDate);
            } else if (StringUtils.hasText(location)) {
                images = imageRepository.findByUploaderIdAndLocationContaining(user.getUserId(), location);
            } else {
                images = imageRepository.findByUploaderIdOrderByCreateDateDesc(user.getUserId());
            }
            
            // 分页处理
            int start = page * size;
            int end = Math.min(start + size, images.size());
            List<Image> pagedImages = (start < images.size()) ? images.subList(start, end) : new ArrayList<>();
            
            // 转换为前端需要的格式
            List<Map<String, Object>> imageList = new ArrayList<>();
            for (Image image : pagedImages) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("imageId", image.getImageId());
                imageData.put("storagePath", image.getStoragePath());
                imageData.put("location", image.getLocation());
                imageData.put("resolution", image.getResolution());
                imageData.put("createDate", image.getCreateDate());
                imageData.put("filename", "图片" + image.getImageId()); // 默认文件名
                
                // 获取分类
                List<String> categories = new ArrayList<>();
                if (image.getImageCategories() != null) {
                    for (ImageCategory ic : image.getImageCategories()) {
                        if (ic.getCategory() != null) {
                            categories.add(ic.getCategory().getCategoryName());
                        }
                    }
                }
                imageData.put("categories", categories);
                
                imageList.add(imageData);
            }
            
            response.put("success", true);
            response.put("images", imageList);
            response.put("total", images.size());
            response.put("page", page);
            response.put("size", size);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取图片列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取图片分组信息（年/月）
     */
    @GetMapping("/api/images/groups")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getImageGroups(
            @RequestParam("type") String type,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            List<Map<String, Object>> groups = new ArrayList<>();
            
            if ("year".equals(type)) {
                List<Integer> years = imageRepository.findDistinctYears(user.getUserId());
                for (Integer year : years) {
                    if (year == null) continue;
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", year.toString());
                    group.put("value", year);
                    // 获取该年份的一张封面图
                    LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
                    LocalDateTime end = start.plusYears(1).minusNanos(1);
                    List<Image> coverImages = imageRepository.findByUploaderIdAndCreateDateBetweenOrderByCreateDateDesc(
                            user.getUserId(), start, end);
                    if (!coverImages.isEmpty()) {
                        group.put("cover", coverImages.get(0).getStoragePath());
                        group.put("count", coverImages.size());
                    }
                    groups.add(group);
                }
            } else if ("month".equals(type)) {
                List<String> months = imageRepository.findDistinctMonths(user.getUserId());
                for (String monthStr : months) {
                    if (monthStr == null) continue;
                    Map<String, Object> group = new HashMap<>();
                    group.put("name", monthStr);
                    group.put("value", monthStr);
                    
                    // 解析年月
                    String[] parts = monthStr.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    
                    // 获取该月份的一张封面图
                    LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
                    LocalDateTime end = start.plusMonths(1).minusNanos(1);
                    List<Image> coverImages = imageRepository.findByUploaderIdAndCreateDateBetweenOrderByCreateDateDesc(
                            user.getUserId(), start, end);
                    if (!coverImages.isEmpty()) {
                        group.put("cover", coverImages.get(0).getStoragePath());
                        group.put("count", coverImages.size());
                    }
                    groups.add(group);
                }
            }
            
            response.put("success", true);
            response.put("groups", groups);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取分组信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    

    
    /**
     * 测试接口 - 获取所有图片（不分页）
     */
    @GetMapping("/api/test-images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testGetAllImages(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            List<Image> images = imageRepository.findByUploaderIdOrderByCreateDateDesc(user.getUserId());
            
            // 转换为前端需要的格式
            List<Map<String, Object>> imageList = new ArrayList<>();
            for (Image image : images) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("imageId", image.getImageId());
                imageData.put("storagePath", image.getStoragePath());
                imageData.put("location", image.getLocation());
                imageData.put("resolution", image.getResolution());
                imageData.put("createDate", image.getCreateDate());
                imageData.put("filename", "图片" + image.getImageId());
                imageList.add(imageData);
            }
            
            response.put("success", true);
            response.put("images", imageList);
            response.put("total", images.size());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取图片列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取轮播图
     */
    @GetMapping("/api/images/carousel")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCarouselImages(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            List<Image> images = imageRepository.findByUploaderIdAndIsCarouselTrueOrderByCreateDateDesc(user.getUserId());
            
            List<Map<String, Object>> imageList = new ArrayList<>();
            for (Image image : images) {
                Map<String, Object> imageData = new HashMap<>();
                imageData.put("imageId", image.getImageId());
                imageData.put("storagePath", image.getStoragePath());
                imageData.put("pcCropData", image.getPcCropData());
                imageData.put("resolution", image.getResolution()); // 添加分辨率信息
                imageList.add(imageData);
            }
            
            response.put("success", true);
            response.put("images", imageList);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "获取轮播图失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 设置轮播图（支持裁剪信息）
     */
    @PostMapping("/api/images/carousel")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> setCarouselImages(@RequestBody Map<String, Object> payload, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        // 兼容旧格式 List<Integer> 和新格式 List<Map<String, Object>>
        List<Map<String, Object>> carouselItems = new ArrayList<>();
        Object imageIdsObj = payload.get("imageIds");
        
        if (imageIdsObj instanceof List) {
            List<?> list = (List<?>) imageIdsObj;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof Integer) {
                    // 旧格式：[1, 2, 3]
                    for (Object id : list) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", id);
                        carouselItems.add(item);
                    }
                } else if (list.get(0) instanceof Map) {
                    // 新格式：[{id: 1, crop: "..."}]
                    carouselItems = (List<Map<String, Object>>) list;
                }
            }
        }
        
        try {
            // 1. 先将该用户所有图片的 isCarousel 设为 false
            List<Image> allCarouselImages = imageRepository.findByUploaderIdAndIsCarouselTrueOrderByCreateDateDesc(user.getUserId());
            for (Image img : allCarouselImages) {
                img.setIsCarousel(false);
                img.setPcCropData(null); // 清除裁剪信息
                imageRepository.save(img);
            }
            
            // 2. 将选中的图片设为 true 并保存裁剪信息
            for (Map<String, Object> item : carouselItems) {
                Integer id = (Integer) item.get("id");
                String cropData = (String) item.get("crop");
                
                Optional<Image> imgOpt = imageRepository.findById(id);
                if (imgOpt.isPresent()) {
                    Image img = imgOpt.get();
                    if (img.getUploaderId().equals(user.getUserId())) {
                        img.setIsCarousel(true);
                        img.setPcCropData(cropData);
                        imageRepository.save(img);
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "轮播图设置成功");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "设置轮播图失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 批量删除图片
     */
    @PostMapping("/api/images/delete")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteImages(@RequestBody Map<String, List<Integer>> payload, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute(SESSION_USER_KEY);
        if (user == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }
        
        List<Integer> imageIds = payload.get("imageIds");
        if (imageIds == null || imageIds.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择要删除的图片");
            return ResponseEntity.badRequest().body(response);
        }
        
        int successCount = 0;
        List<String> failedImages = new ArrayList<>();
        
        try {
            for (Integer imageId : imageIds) {
                Optional<Image> imageOpt = imageRepository.findById(imageId);
                if (imageOpt.isPresent()) {
                    Image image = imageOpt.get();
                    // 检查权限
                    if (!image.getUploaderId().equals(user.getUserId())) {
                        failedImages.add("图片ID " + imageId + ": 无权删除");
                        continue;
                    }
                    
                    // 删除关联数据
                    thumbnailRepository.deleteByOriginalImageId(imageId);
                    imageCategoryRepository.deleteByImageId(imageId);
                    
                    // 删除物理文件
                    try {
                        // 删除原图
                        // 注意：这里假设 storagePath 是相对路径，如 /uploads/2023/10/xxx.jpg
                        // 需要转换为绝对路径
                        String storagePath = image.getStoragePath();
                        if (storagePath != null && storagePath.startsWith("/")) {
                            storagePath = storagePath.substring(1); // 去掉开头的 /
                        }
                        Path originalFile = Paths.get(System.getProperty("user.dir"), storagePath);
                        Files.deleteIfExists(originalFile);
                        
                        // 删除缩略图 (假设缩略图路径规则)
                        // 实际上应该从 Thumbnail 表查，但这里简化处理，或者如果 Thumbnail 表里有路径更好
                        // 这里我们先只删除数据库记录，物理文件删除可能需要更严谨的路径查找
                        // 如果 Thumbnail 实体有路径，应该查出来删。
                        // 暂时只删原图物理文件和数据库记录
                        
                    } catch (Exception e) {
                        System.err.println("删除物理文件失败: " + e.getMessage());
                        // 物理文件删除失败不阻止数据库记录删除
                    }
                    
                    // 删除图片记录
                    imageRepository.delete(image);
                    successCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "成功删除 " + successCount + " 张图片");
            if (!failedImages.isEmpty()) {
                response.put("failedDetails", failedImages);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 生成缩略图
     */
    private void generateThumbnail(BufferedImage originalImage, File thumbnailFile, int maxWidth, int maxHeight) throws IOException {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 计算缩略图尺寸
        double scaleX = (double) maxWidth / originalWidth;
        double scaleY = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);
        
        int thumbnailWidth = (int) (originalWidth * scale);
        int thumbnailHeight = (int) (originalHeight * scale);
        
        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
        g2d.dispose();
        
        // 保存缩略图
        String formatName = "jpg";
        if (thumbnailFile.getName().toLowerCase().endsWith(".png")) {
            formatName = "png";
        }
        boolean writeSuccess = ImageIO.write(thumbnail, formatName, thumbnailFile);
        if (!writeSuccess) {
            throw new IOException("ImageIO.write 失败，无法写入缩略图: " + thumbnailFile.getAbsolutePath());
        }
        System.out.println("缩略图文件已写入: " + thumbnailFile.getAbsolutePath());
    }
}

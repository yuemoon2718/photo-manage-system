package com.example.imagemgmt.repository;

import com.example.imagemgmt.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    
    // 根据上传者ID查找图片
    List<Image> findByUploaderIdOrderByCreateDateDesc(Integer uploaderId);
    
    // 根据上传者ID和创建日期范围查找图片
    List<Image> findByUploaderIdAndCreateDateBetweenOrderByCreateDateDesc(
            Integer uploaderId, LocalDateTime startDate, LocalDateTime endDate);
    
    // 根据地点搜索图片
    @Query("SELECT i FROM Image i WHERE i.uploaderId = :uploaderId AND i.location LIKE %:location% ORDER BY i.createDate DESC")
    List<Image> findByUploaderIdAndLocationContaining(@Param("uploaderId") Integer uploaderId, @Param("location") String location);
    
    // 根据分辨率查找图片
    List<Image> findByUploaderIdAndResolutionOrderByCreateDateDesc(Integer uploaderId, String resolution);
    
    // 查找最新上传的图片
    List<Image> findTop10ByUploaderIdOrderByCreateDateDesc(Integer uploaderId);
    
    // 根据关键词搜索图片（地点、分类等）
    @Query("SELECT DISTINCT i FROM Image i " +
           "LEFT JOIN ImageCategory ic ON i.imageId = ic.imageId " +
           "LEFT JOIN Category c ON ic.categoryId = c.categoryId " +
           "WHERE i.uploaderId = :uploaderId " +
           "AND (i.location LIKE %:keyword% OR c.categoryName LIKE %:keyword%) " +
           "ORDER BY i.createDate DESC")
    List<Image> searchByKeyword(@Param("uploaderId") Integer uploaderId, @Param("keyword") String keyword);

    // 获取用户图片的所有年份
    @Query("SELECT DISTINCT YEAR(i.createDate) FROM Image i WHERE i.uploaderId = :uploaderId ORDER BY YEAR(i.createDate) DESC")
    List<Integer> findDistinctYears(@Param("uploaderId") Integer uploaderId);

    // 获取用户图片的所有月份（格式：YYYY-MM）
    @Query("SELECT DISTINCT DATE_FORMAT(i.createDate, '%Y-%m') FROM Image i WHERE i.uploaderId = :uploaderId ORDER BY DATE_FORMAT(i.createDate, '%Y-%m') DESC")
    List<String> findDistinctMonths(@Param("uploaderId") Integer uploaderId);

    // 查找用户的轮播图
    List<Image> findByUploaderIdAndIsCarouselTrueOrderByCreateDateDesc(Integer uploaderId);
}
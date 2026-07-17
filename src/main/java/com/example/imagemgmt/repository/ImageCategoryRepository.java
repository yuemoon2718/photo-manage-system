package com.example.imagemgmt.repository;

import com.example.imagemgmt.entity.ImageCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageCategoryRepository extends JpaRepository<ImageCategory, Integer> {
    
    // 根据图片ID查找关联的分类
    List<ImageCategory> findByImageId(Integer imageId);
    
    // 根据分类ID查找关联的图片
    List<ImageCategory> findByCategoryId(Integer categoryId);
    
    // 根据图片ID删除所有关联
    void deleteByImageId(Integer imageId);
    
    // 根据分类ID删除所有关联
    void deleteByCategoryId(Integer categoryId);
    
    // 检查图片和分类的关联是否存在
    boolean existsByImageIdAndCategoryId(Integer imageId, Integer categoryId);
    
    // 根据图片ID和分类ID查找关联
    Optional<ImageCategory> findByImageIdAndCategoryId(Integer imageId, Integer categoryId);
    
    // 根据用户ID查找该用户所有图片的分类关联
    @Query("SELECT ic FROM ImageCategory ic JOIN ic.image i WHERE i.uploaderId = :uploaderId")
    List<ImageCategory> findByImageUploaderId(@Param("uploaderId") Integer uploaderId);
}
package com.example.imagemgmt.repository;

import com.example.imagemgmt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // 根据创建者ID查找分类
    List<Category> findByCreatorIdOrderByCategoryName(Integer creatorId);
    
    // 根据创建者ID和分类名称查找分类
    Optional<Category> findByCreatorIdAndCategoryName(Integer creatorId, String categoryName);
    
    // 检查分类是否存在
    boolean existsByCreatorIdAndCategoryName(Integer creatorId, String categoryName);
    
    // 根据分类名称模糊搜索
    @Query("SELECT c FROM Category c WHERE c.creatorId = :creatorId AND c.categoryName LIKE %:categoryName% ORDER BY c.categoryName")
    List<Category> findByCreatorIdAndCategoryNameContaining(@Param("creatorId") Integer creatorId, @Param("categoryName") String categoryName);
}
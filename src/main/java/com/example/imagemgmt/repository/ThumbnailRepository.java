package com.example.imagemgmt.repository;

import com.example.imagemgmt.entity.Thumbnail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThumbnailRepository extends JpaRepository<Thumbnail, Integer> {
    
    // 根据原图片ID查找缩略图
    List<Thumbnail> findByOriginalImageId(Integer originalImageId);
    
    // 根据原图片ID删除缩略图
    void deleteByOriginalImageId(Integer originalImageId);
}
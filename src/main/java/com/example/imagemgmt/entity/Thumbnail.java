package com.example.imagemgmt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "thumbnails")
public class Thumbnail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thumbnail_id")
    private Integer thumbnailId;
    
    @Column(name = "original_image_id", nullable = false)
    private Integer originalImageId;
    
    @Column(name = "thumbnail_path", nullable = false, length = 500)
    private String thumbnailPath;
    
    // 关联原图片
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_image_id", insertable = false, updatable = false)
    private Image originalImage;
    
    // 构造函数
    public Thumbnail() {}
    
    public Thumbnail(Integer originalImageId, String thumbnailPath) {
        this.originalImageId = originalImageId;
        this.thumbnailPath = thumbnailPath;
    }
    
    // Getter 和 Setter 方法
    public Integer getThumbnailId() {
        return thumbnailId;
    }
    
    public void setThumbnailId(Integer thumbnailId) {
        this.thumbnailId = thumbnailId;
    }
    
    public Integer getOriginalImageId() {
        return originalImageId;
    }
    
    public void setOriginalImageId(Integer originalImageId) {
        this.originalImageId = originalImageId;
    }
    
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
    
    public Image getOriginalImage() {
        return originalImage;
    }
    
    public void setOriginalImage(Image originalImage) {
        this.originalImage = originalImage;
    }
    
    @Override
    public String toString() {
        return "Thumbnail{" +
                "thumbnailId=" + thumbnailId +
                ", originalImageId=" + originalImageId +
                ", thumbnailPath='" + thumbnailPath + '\'' +
                '}';
    }
}
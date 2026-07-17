package com.example.imagemgmt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "images")
public class Image {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;
    
    @Column(name = "uploader_id", nullable = false)
    private Integer uploaderId;
    
    @Column(name = "create_date")
    private LocalDateTime createDate;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @Column(name = "resolution", length = 50)
    private String resolution;
    
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "is_carousel")
    private Boolean isCarousel = false;

    @Column(name = "pc_crop_data", length = 100)
    private String pcCropData; // 存储格式: "x,y,width,height" 百分比
    
    // 关联用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", insertable = false, updatable = false)
    private User uploader;
    
    // 关联缩略图
    @OneToMany(mappedBy = "originalImageId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Thumbnail> thumbnails;
    
    // 关联分类
    @OneToMany(mappedBy = "imageId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageCategory> imageCategories;
    
    // 构造函数
    public Image() {}
    
    public Image(Integer uploaderId, String location, String resolution, String storagePath) {
        this.uploaderId = uploaderId;
        this.location = location;
        this.resolution = resolution;
        this.storagePath = storagePath;
        this.createDate = LocalDateTime.now();
    }
    
    // Getter 和 Setter 方法
    public Integer getImageId() {
        return imageId;
    }
    
    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
    
    public Integer getUploaderId() {
        return uploaderId;
    }
    
    public void setUploaderId(Integer uploaderId) {
        this.uploaderId = uploaderId;
    }
    
    public LocalDateTime getCreateDate() {
        return createDate;
    }
    
    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
    
    public String getStoragePath() {
        return storagePath;
    }
    
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Boolean getIsCarousel() {
        return isCarousel;
    }

    public void setIsCarousel(Boolean isCarousel) {
        this.isCarousel = isCarousel;
    }

    public String getPcCropData() {
        return pcCropData;
    }

    public void setPcCropData(String pcCropData) {
        this.pcCropData = pcCropData;
    }
    
    public User getUploader() {
        return uploader;
    }
    
    public void setUploader(User uploader) {
        this.uploader = uploader;
    }
    
    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }
    
    public void setThumbnails(List<Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }
    
    public List<ImageCategory> getImageCategories() {
        return imageCategories;
    }
    
    public void setImageCategories(List<ImageCategory> imageCategories) {
        this.imageCategories = imageCategories;
    }
    
    @Override
    public String toString() {
        return "Image{" +
                "imageId=" + imageId +
                ", uploaderId=" + uploaderId +
                ", createDate=" + createDate +
                ", location='" + location + '\'' +
                ", resolution='" + resolution + '\'' +
                ", storagePath='" + storagePath + '\'' +
                '}';
    }
}
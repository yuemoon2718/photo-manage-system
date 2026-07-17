package com.example.imagemgmt.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "image_categories", 
       uniqueConstraints = @UniqueConstraint(name = "uk_image_category", columnNames = {"image_id", "category_id"}))
public class ImageCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_category_id")
    private Integer imageCategoryId;
    
    @Column(name = "image_id", nullable = false)
    private Integer imageId;
    
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;
    
    // 关联图片
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", insertable = false, updatable = false)
    private Image image;
    
    // 关联分类
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private Category category;
    
    // 构造函数
    public ImageCategory() {}
    
    public ImageCategory(Integer imageId, Integer categoryId) {
        this.imageId = imageId;
        this.categoryId = categoryId;
    }
    
    // Getter 和 Setter 方法
    public Integer getImageCategoryId() {
        return imageCategoryId;
    }
    
    public void setImageCategoryId(Integer imageCategoryId) {
        this.imageCategoryId = imageCategoryId;
    }
    
    public Integer getImageId() {
        return imageId;
    }
    
    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public Image getImage() {
        return image;
    }
    
    public void setImage(Image image) {
        this.image = image;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "ImageCategory{" +
                "imageCategoryId=" + imageCategoryId +
                ", imageId=" + imageId +
                ", categoryId=" + categoryId +
                '}';
    }
}
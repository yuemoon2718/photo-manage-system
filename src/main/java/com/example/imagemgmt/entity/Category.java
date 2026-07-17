package com.example.imagemgmt.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;
    
    @Column(name = "creator_id", nullable = false)
    private Integer creatorId;
    
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    
    // 关联创建用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", insertable = false, updatable = false)
    private User creator;
    
    // 关联图片分类
    @OneToMany(mappedBy = "categoryId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImageCategory> imageCategories;
    
    // 构造函数
    public Category() {}
    
    public Category(Integer creatorId, String categoryName) {
        this.creatorId = creatorId;
        this.categoryName = categoryName;
    }
    
    // Getter 和 Setter 方法
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public Integer getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }
    
    public List<ImageCategory> getImageCategories() {
        return imageCategories;
    }
    
    public void setImageCategories(List<ImageCategory> imageCategories) {
        this.imageCategories = imageCategories;
    }
    
    @Override
    public String toString() {
        return "Category{" +
                "categoryId=" + categoryId +
                ", creatorId=" + creatorId +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }
}
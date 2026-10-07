package com.example.orchid.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orchid_categories")
public class OrchidCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false, unique = true, length = 100)
    private String categoryName;

    // mappedBy là tên field Java bên Orchid, không phải tên cột category_id.
    @OneToMany(mappedBy = "orchidCategory")
    @JsonIgnore // Ngăn vòng lặp JSON: category -> orchids -> category -> ...
    private List<Orchid> orchids = new ArrayList<>();

    public OrchidCategory() {
    }

    public OrchidCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<Orchid> getOrchids() {
        return orchids;
    }

    public void setOrchids(List<Orchid> orchids) {
        this.orchids = orchids;
    }
}

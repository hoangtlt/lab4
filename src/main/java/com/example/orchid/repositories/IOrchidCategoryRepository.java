package com.example.orchid.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orchid.entity.OrchidCategory;

public interface IOrchidCategoryRepository extends JpaRepository<OrchidCategory, Long> {
}

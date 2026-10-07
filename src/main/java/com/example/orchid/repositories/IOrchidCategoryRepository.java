package com.example.orchid.repositories;

import com.example.orchid.pojos.OrchidCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrchidCategoryRepository extends JpaRepository<OrchidCategory, Long> {
}

package com.example.orchid.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orchid.entity.Orchid;

import java.util.List;

public interface IOrchidRepository extends JpaRepository<Orchid, Long> {
    List<Orchid> findByOrchidNameContainingIgnoreCase(String name);
}

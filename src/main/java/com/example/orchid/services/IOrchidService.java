package com.example.orchid.services;

import com.example.orchid.pojos.Orchid;

import java.util.List;
import java.util.Optional;

public interface IOrchidService {
    List<Orchid> getAll();
    List<Orchid> searchByName(String name);
    Optional<Orchid> getById(Long id);
    Orchid create(Orchid orchid);
    Optional<Orchid> update(Long id, Orchid orchid);
    boolean delete(Long id);
}

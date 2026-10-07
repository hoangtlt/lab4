package com.example.orchid.services;

import com.example.orchid.pojos.Orchid;
import com.example.orchid.pojos.OrchidCategory;
import com.example.orchid.repositories.IOrchidCategoryRepository;
import com.example.orchid.repositories.IOrchidRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OrchidService implements IOrchidService {
    private final IOrchidRepository orchidRepository;
    private final IOrchidCategoryRepository categoryRepository;

    public OrchidService(IOrchidRepository orchidRepository,
                         IOrchidCategoryRepository categoryRepository) {
        this.orchidRepository = orchidRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Orchid> getAll() {
        return orchidRepository.findAll();
    }

    @Override
    public List<Orchid> searchByName(String name) {
        return orchidRepository.findByOrchidNameContainingIgnoreCase(name);
    }

    @Override
    public Optional<Orchid> getById(Long id) {
        return orchidRepository.findById(id);
    }

    // Lấy category có thật trong DB, không lưu category do client tự gửi lên.
    private OrchidCategory resolveCategory(Orchid orchid) {
        if (orchid.getOrchidCategory() == null
                || orchid.getOrchidCategory().getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
        }

        Long categoryId = orchid.getOrchidCategory().getCategoryId();
        Optional<OrchidCategory> category = categoryRepository.findById(categoryId);
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Category not found: " + categoryId);
        }
        return category.get();
    }

    private void validateOrchid(Orchid orchid) {
        if (orchid.getOrchidName() == null || orchid.getOrchidName().isBlank()) {
            throw new IllegalArgumentException("orchidName is required");
        }
        if (orchid.getOrchidName().length() > 150) {
            throw new IllegalArgumentException("orchidName must not exceed 150 characters");
        }
        if (orchid.getOrchidDescription() != null
                && orchid.getOrchidDescription().length() > 1000) {
            throw new IllegalArgumentException("orchidDescription must not exceed 1000 characters");
        }
        if (orchid.getOrchidURL() != null && orchid.getOrchidURL().length() > 255) {
            throw new IllegalArgumentException("orchidURL must not exceed 255 characters");
        }
    }

    @Override
    @Transactional
    public Orchid create(Orchid orchid) {
        validateOrchid(orchid);
        OrchidCategory category = resolveCategory(orchid);
        orchid.setOrchidID(null); // ID mới do SQL Server sinh, bỏ qua ID từ client.
        orchid.setOrchidCategory(category);
        return orchidRepository.save(orchid);
    }

    @Override
    @Transactional
    public Optional<Orchid> update(Long id, Orchid input) {
        Optional<Orchid> result = orchidRepository.findById(id);
        if (result.isEmpty()) {
            return Optional.empty();
        }

        // Kiểm tra đầu vào trước khi thay đổi entity đang được Hibernate quản lý.
        validateOrchid(input);
        OrchidCategory category = resolveCategory(input);
        Orchid existing = result.get();
        existing.setOrchidName(input.getOrchidName());
        existing.setIsNatural(input.getIsNatural());
        existing.setOrchidDescription(input.getOrchidDescription());
        existing.setIsAttractive(input.getIsAttractive());
        existing.setOrchidURL(input.getOrchidURL());
        existing.setOrchidCategory(category);
        // PUT thay toàn bộ dữ liệu: field tùy chọn bị thiếu sẽ thành null.
        return Optional.of(orchidRepository.save(existing));
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!orchidRepository.existsById(id)) {
            return false;
        }
        orchidRepository.deleteById(id);
        return true;
    }
}

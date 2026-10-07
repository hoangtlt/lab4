package com.example.orchid;

import com.example.orchid.pojos.Orchid;
import com.example.orchid.pojos.OrchidCategory;
import com.example.orchid.repositories.IOrchidCategoryRepository;
import com.example.orchid.repositories.IOrchidRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrchidApiTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private IOrchidRepository orchidRepository;
    @Autowired private IOrchidCategoryRepository categoryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    private Long cattleyaId;
    private Long dendrobiumId;

    @BeforeEach
    void prepareData() {
        orchidRepository.deleteAll();
        categoryRepository.deleteAll();
        cattleyaId = categoryRepository.save(new OrchidCategory("Cattleya")).getCategoryId();
        dendrobiumId = categoryRepository.save(new OrchidCategory("Dendrobium")).getCategoryId();
    }

    private String body(String name, Long categoryId) throws Exception {
        Orchid input = new Orchid();
        input.setOrchidName(name);
        input.setIsNatural(true);
        input.setIsAttractive(true);
        input.setOrchidDescription("Demo orchid for Slot 18");
        input.setOrchidURL("https://example.com/orchid.jpg");
        OrchidCategory category = new OrchidCategory();
        category.setCategoryId(categoryId);
        input.setOrchidCategory(category);
        return objectMapper.writeValueAsString(input);
    }

    private Long createOrchid() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orchids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Cattleya Queen", cattleyaId)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("orchidID").asLong();
    }

    @Test
    void T01_listReturnsJsonArray() throws Exception {
        createOrchid();
        mockMvc.perform(get("/api/orchids"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void T02_searchIsCaseInsensitiveAndFiltersNames() throws Exception {
        createOrchid();
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content(body("Dendrobium White", dendrobiumId))).andExpect(status().isCreated());
        mockMvc.perform(get("/api/orchids").param("name", "cAt"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orchidName").value("Cattleya Queen"));
        mockMvc.perform(get("/api/orchids").param("name", "no-match"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/orchids").param("name", " "))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void T03_detailIncludesCategoryWithoutRecursiveJson() throws Exception {
        Long id = createOrchid();
        mockMvc.perform(get("/api/orchids/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orchidID").value(id))
                .andExpect(jsonPath("$.orchidCategory.categoryName").value("Cattleya"))
                .andExpect(jsonPath("$.orchidCategory.orchids").doesNotExist());
    }

    @Test
    void T04_missingDetailReturns404() throws Exception {
        mockMvc.perform(get("/api/orchids/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void T05_createGeneratesIdIgnoresClientIdAndResolvesCategory() throws Exception {
        JsonNode request = objectMapper.readTree(body("Cattleya Queen", cattleyaId));
        ((com.fasterxml.jackson.databind.node.ObjectNode) request).put("orchidID", 999999999);
        ((com.fasterxml.jackson.databind.node.ObjectNode) request.get("orchidCategory"))
                .put("categoryName", "Fake client category");
        MvcResult result = mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.orchidCategory.categoryName").value("Cattleya")).andReturn();
        Long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("orchidID").asLong();
        assertThat(id).isPositive().isNotEqualTo(999999999L);
        mockMvc.perform(get("/api/orchids/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orchidName").value("Cattleya Queen"));
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    @Test
    void T06_missingAndUnknownCategoryReturn400WithoutInsertingRows() throws Exception {
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content(body("Invalid", 999999999L))).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Category not found")));
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orchidName\":\"Missing category\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().string("categoryId is required"));
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content(body("Missing ID", null))).andExpect(status().isBadRequest());
        assertThat(orchidRepository.count()).isZero();
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    @Test
    void T07_updateReplacesAllFieldsAndForeignKey() throws Exception {
        Long id = createOrchid();
        String update = """
                {"orchidName":"Cattleya Queen Updated", "isNatural":false,
                 "orchidDescription":"Updated by PUT", "isAttractive":false,
                 "orchidURL":"https://example.com/orchid-updated.jpg",
                 "orchidCategory":{"categoryId":%d}}
                """.formatted(dendrobiumId);
        mockMvc.perform(put("/api/orchids/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(update)).andExpect(status().isOk());
        mockMvc.perform(get("/api/orchids/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orchidID").value(id))
                .andExpect(jsonPath("$.orchidName").value("Cattleya Queen Updated"))
                .andExpect(jsonPath("$.isNatural").value(false))
                .andExpect(jsonPath("$.isAttractive").value(false))
                .andExpect(jsonPath("$.orchidDescription").value("Updated by PUT"))
                .andExpect(jsonPath("$.orchidURL").value("https://example.com/orchid-updated.jpg"))
                .andExpect(jsonPath("$.orchidCategory.categoryId").value(dendrobiumId));
        assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM orchids WHERE orchid_id = ?",
                Long.class, id)).isEqualTo(dendrobiumId);
    }

    @Test
    void T08_updateMissingReturns404WithoutCreatingResource() throws Exception {
        mockMvc.perform(put("/api/orchids/999999999").contentType(MediaType.APPLICATION_JSON)
                .content(body("Missing", cattleyaId))).andExpect(status().isNotFound());
        assertThat(orchidRepository.count()).isZero();
    }

    @Test
    void T09_deleteReturns204ThenGetReturns404AndCategoryRemains() throws Exception {
        Long id = createOrchid();
        mockMvc.perform(delete("/api/orchids/{id}", id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(get("/api/orchids/{id}", id)).andExpect(status().isNotFound());
        assertThat(orchidRepository.existsById(id)).isFalse();
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    @Test
    void T10_deleteMissingReturns404() throws Exception {
        mockMvc.perform(delete("/api/orchids/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void T11_databaseForeignKeyAndInverseCollectionAreCorrect() throws Exception {
        Long id = createOrchid();
        assertThat(jdbcTemplate.queryForObject("SELECT category_id FROM orchids WHERE orchid_id = ?",
                Long.class, id)).isEqualTo(cattleyaId);
        transactionTemplate.executeWithoutResult(transaction -> {
            entityManager.clear();
            OrchidCategory category = categoryRepository.findById(cattleyaId).orElseThrow();
            assertThat(category.getOrchids()).hasSize(1);
            assertThat(category.getOrchids().getFirst().getOrchidID()).isEqualTo(id);
        });
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO orchids (orchid_name, category_id) VALUES (?, ?)", "Bad FK", 999999999L))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void invalidUpdateRollsBackAndKeepsOriginalState() throws Exception {
        Long id = createOrchid();
        mockMvc.perform(put("/api/orchids/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(body("Must not be saved", 999999999L))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/orchids/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orchidName").value("Cattleya Queen"))
                .andExpect(jsonPath("$.orchidCategory.categoryId").value(cattleyaId));
    }

    @Test
    void putClearsMissingOptionalFields() throws Exception {
        Long id = createOrchid();
        String input = "{\"orchidName\":\"Only required fields\",\"orchidCategory\":{\"categoryId\":"
                + cattleyaId + "}}";
        mockMvc.perform(put("/api/orchids/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(input)).andExpect(status().isOk());
        Orchid orchid = orchidRepository.findById(id).orElseThrow();
        assertThat(orchid.getIsNatural()).isNull();
        assertThat(orchid.getIsAttractive()).isNull();
        assertThat(orchid.getOrchidDescription()).isNull();
        assertThat(orchid.getOrchidURL()).isNull();
    }

    @Test
    void invalidNameAndOverlongDescriptionReturn400() throws Exception {
        for (String name : new String[]{"", " ", "x".repeat(151)}) {
            mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                    .content(body(name, cattleyaId))).andExpect(status().isBadRequest());
        }
        String input = body("Long description", cattleyaId).replace("Demo orchid for Slot 18", "x".repeat(1001));
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content(input)).andExpect(status().isBadRequest());
        assertThat(orchidRepository.count()).isZero();
    }

    @Test
    void B07_malformedJsonReturns400ThenCorrectBodyWorks() throws Exception {
        mockMvc.perform(post("/api/orchids").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orchidName\":")).andExpect(status().isBadRequest());
        createOrchid();
    }

    @Test
    void B08_wrongRouteReturns404ThenCorrectRouteWorks() throws Exception {
        mockMvc.perform(get("/api/orchid")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orchids")).andExpect(status().isOk());
    }
}

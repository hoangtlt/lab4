# Giải thích code và luồng request

## 1. JPA, Hibernate, Spring Data JPA

- JPA là đặc tả: quy định entity, relationship và các thao tác persistence.
  Trong code, các annotation nằm trong package `jakarta.persistence`.
- Hibernate là implementation JPA: đọc mapping, quản lý entity và sinh SQL.
- Spring Data JPA tạo implementation cho các interface `JpaRepository`,
  cung cấp `findAll`, `findById`, `save`, `deleteById` và derived query.

Không cần tự viết `OrchidRepositoryImpl`. Spring tạo repository proxy lúc khởi tạo
application context. JDBC driver chuyển lệnh đến SQL Server; database thực thi SQL.

## 2. Quan hệ và owning side

```text
orchid_categories                    orchids
category_id (PK, IDENTITY)  <-----   category_id (FK, NOT NULL)
category_name (UNIQUE)               orchid_id (PK, IDENTITY)
                                    orchid_name, ...
         1                            N
```

`Orchid.orchidCategory` là owning side, có `@ManyToOne` và
`@JoinColumn(name="category_id")`. Gán category ở field này quyết định FK được lưu.

`OrchidCategory.orchids` là inverse side, có `@OneToMany(mappedBy="orchidCategory")`.
`mappedBy` trỏ đến **field Java** `orchidCategory`, không phải cột `category_id`.
Sau khi tải lại category trong transaction, danh sách này chứa các Orchid thuộc nó.
Test T11 kiểm tra cả danh sách Java và FK trong database.

Không khai báo cascade ALL: xóa Orchid không được xóa category dùng chung.
`@JsonIgnore` trên inverse collection ngăn vòng lặp khi serialize JSON.

## 3. POST end-to-end

Request: POST `/api/orchids`, Content-Type `application/json`, body tên
`Cattleya Queen`, categoryId của Cattleya đã được seed.

1. Spring MVC tìm `OrchidController.create()`. `@RequestBody` cùng Jackson
   chuyển JSON thành object Orchid và object category chỉ mang ID.
2. Controller gọi `IOrchidService.create()`. Controller chỉ quyết định HTTP;
   không truy cập repository hoặc viết SQL.
3. Spring mở transaction trước khi chạy `OrchidService.create()` vì method
   có `@Transactional`, ghi đè chế độ readOnly ở class.
4. Service kiểm tra tên/độ dài. `resolveCategory()` kiểm tra categoryId rồi
   gọi `categoryRepository.findById()`. Hibernate SELECT category trong SQL Server.
5. Category không tồn tại: ném `IllegalArgumentException`, transaction rollback,
   controller trả 400. Không có Orchid hoặc category mới được tạo.
6. Category tồn tại: object category đã được quản lý trong persistence context.
   Service bỏ ID client gửi, gán category thật vào owning side của Orchid.
7. `orchidRepository.save()` yêu cầu Hibernate persist Orchid mới. Hibernate
   sinh INSERT vào `orchids`, cột `category_id` là ID category vừa lấy. SQL Server
   kiểm tra FK và sinh `orchid_id` bằng IDENTITY.
8. Transaction commit khi service trả về thành công. SQL/flush có thể diễn ra
   trong `save` hoặc khi commit; với IDENTITY, INSERT cần lấy ID khi persist.
9. Controller trả 201 và object đã có `orchidID`. Jackson serialize category
   thành categoryId/categoryName, bỏ qua danh sách orchids.
10. GET lại ID và SELECT JOIN chứng minh row cùng FK thật sự được lưu.

## 4. GET end-to-end

Request: GET `/api/orchids/{id}`.

1. `@PathVariable Long id` lấy ID từ URL, khác `@RequestParam name` lấy query string.
2. Controller gọi service `getById(id)`. Transaction mặc định readOnly.
3. Repository `findById(id)` gọi JPA. Hibernate SELECT row và category
   (`ManyToOne` mặc định EAGER), SQL Server trả dữ liệu.
4. Có dữ liệu: `Optional` có Orchid, controller trả 200 JSON.
   Không có: `Optional.empty()`, controller trả 404.
5. Database không truy cập được là lỗi infrastructure, khác với SELECT thành công
   nhưng không có row. Không biến lỗi DB thành 404.

GET `/api/orchids?name=cat` dùng
`findByOrchidNameContainingIgnoreCase(name)`. Spring Data đọc tên method:
field `orchidName`, tìm chứa chuỗi, bỏ khác biệt hoa thường, rồi tạo query cho Hibernate.

## 5. PUT và DELETE

PUT: tìm Orchid → validate → lấy category thật → thay mọi field → save → commit.
Field tùy chọn thiếu thành null. Category sai không thay đổi dữ liệu cũ.

DELETE: kiểm tra tồn tại → deleteById → commit → 204 body rỗng. Sau đó GET phải 404,
SELECT không còn row. Không có cascade xóa category. ID không tồn tại trả 404.

## 6. Trả lời nhanh khi giải thích bài

- Service không trả ResponseEntity vì nghiệp vụ không cần biết HTTP.
- `Long` trong repository khớp kiểu field `@Id` của entity.
- `@Entity` khai báo object được map; `@Table` chọn bảng; `@Column` chọn cột/ràng buộc.
- `@Id` là PK; `@GeneratedValue(IDENTITY)` dùng ID tự sinh từ SQL Server.
- 200 là request thành công có dữ liệu; 201 là tạo mới; 204 là thành công không body.
- GET/SELECT lại kiểm chứng trạng thái sau thao tác; response thành công chưa đủ.
- T12 cần dừng hẳn JVM, khởi động lại và đọc cùng ID: chứng minh dữ liệu bền vững,
  không chỉ nằm trong collection Java hoặc cache của process.
- Khi AI đề xuất mapping, so với field Java, chạy startup, kiểm tra schema PK/FK,
  thực hiện CRUD và kiểm tra SQL. Không chỉ đánh giá bằng nhìn code.

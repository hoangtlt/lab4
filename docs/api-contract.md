# API contract

Base URL mặc định: `http://localhost:8080`. POST/PUT dùng `Content-Type: application/json`.
API nhận/trả entity trực tiếp như mẫu đề. Collection `OrchidCategory.orchids`
có `@JsonIgnore` để không tạo vòng lặp JSON.

| Method | Path | Body/query | Thành công | Lỗi |
| --- | --- | --- | --- | --- |
| GET | `/api/orchids` | Không có | 200, array; rỗng là `[]` | Lỗi DB là lỗi server |
| GET | `/api/orchids?name=cat` | Chuỗi tìm kiếm | 200, tên chứa chuỗi, không phân biệt hoa thường | Không khớp trả `[]` |
| GET | `/api/orchids/{id}` | ID kiểu Long | 200, object | 404 không có Orchid; 400 nếu ID không phải số |
| POST | `/api/orchids` | Orchid JSON | 201, ID tự sinh | 400 tên/category không hợp lệ |
| PUT | `/api/orchids/{id}` | Orchid JSON | 200, object mới | 404 Orchid không có; 400 dữ liệu sai |
| DELETE | `/api/orchids/{id}` | Không có | 204, body rỗng | 404 Orchid không có |

`name` bị thiếu, rỗng hoặc toàn khoảng trắng: lấy danh sách đầy đủ.

## Body POST/PUT

```json
{
  "orchidName": "Cattleya Queen",
  "isNatural": true,
  "orchidDescription": "Demo orchid for Slot 18",
  "orchidCategory": { "categoryId": 1 },
  "isAttractive": true,
  "orchidURL": "https://example.com/orchid.jpg"
}
```

- `orchidName`: bắt buộc, không blank, tối đa 150 ký tự.
- `orchidCategory.categoryId`: bắt buộc, phải có trong SQL Server.
- `isNatural`, `isAttractive`: Boolean, có thể null.
- `orchidDescription`: tùy chọn, tối đa 1000 ký tự.
- `orchidURL`: tùy chọn, tối đa 255 ký tự.
- POST bỏ qua `orchidID` do client cung cấp. PUT dùng ID trên path.
- Tên category trong request không cập nhật category: service lấy category thật từ DB.

## Response thành công

```json
{
  "orchidID": 1,
  "orchidName": "Cattleya Queen",
  "isNatural": true,
  "orchidDescription": "Demo orchid for Slot 18",
  "orchidCategory": { "categoryId": 1, "categoryName": "Cattleya" },
  "isAttractive": true,
  "orchidURL": "https://example.com/orchid.jpg"
}
```

ID mẫu chỉ minh họa. ID thật được SQL Server sinh bằng identity.
Lỗi nghiệp vụ trả HTTP 400 với text, ví dụ `categoryId is required`
hoặc `Category not found: 999999999`. JSON sai cú pháp trả 400 do Spring xử lý.
Content-Type không hỗ trợ trả 415. Resource không có trả 404 body rỗng.
Lỗi kết nối DB không bị chuyển thành lỗi category 400.

## PUT thay thế dữ liệu

PUT cập nhật mọi field có thể thay đổi. Các field tùy chọn không có trong body
sẽ thành null; tên và category vẫn bắt buộc. Orchid không tồn tại trả 404,
không tạo mới. Category sai trả 400 và dữ liệu đã lưu không bị thay đổi.

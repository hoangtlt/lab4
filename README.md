# SBA301 - Lab 04: Orchid REST API

Project theo `docs/SBA301_Slot18_Lab04_Guide.pdf`: Java 21, Spring Boot 3.x,
Spring Data JPA, SQL Server, REST API và Postman. Chỉ làm backend theo phạm vi đề.

Code dùng constructor injection, getter/setter viết rõ, `if` và `Optional` đơn giản.
Không dùng Lombok, không thêm các chức năng mở rộng làm phức tạp bài.

## 1. Đọc code theo thứ tự này

1. `pojos/OrchidCategory.java`: một loại lan, có nhiều Orchid.
2. `pojos/Orchid.java`: một cây lan, thuộc một category, giữ khóa ngoại.
3. `repositories/`: interface kế thừa `JpaRepository`, Spring tự tạo implementation.
4. `services/IOrchidService.java`: danh sách chức năng nghiệp vụ.
5. `services/OrchidService.java`: kiểm tra category, CRUD và transaction.
6. `controllers/OrchidController.java`: nhận HTTP, gọi service, trả status.

Luồng chính: **HTTP → Controller → Service → Repository → Hibernate → SQL Server**.
Đọc thêm `docs/concept-trace.md` để hiểu một POST và một GET cụ thể.

## 2. Điều kiện chạy

- JDK 21 và Maven (`java -version`, `mvn -version`).
- SQL Server bật TCP/IP, biết đúng port và có quyền tạo database.
- IntelliJ IDEA để mở `pom.xml`; Postman để import collection.

Trên máy đã kiểm chứng bài này: instance `.\SQL2022`, TCP port `1433`,
đăng nhập được bằng Windows Authentication. Không cần lưu mật khẩu trong project.

## 3. Chạy trên máy hiện tại bằng Windows Authentication

Mở PowerShell trong thư mục `lab4`:

```powershell
# Tạo OrchidDB và tải DLL JDBC chính thức của Microsoft (chỉ cần lần đầu).
powershell -ExecutionPolicy Bypass -File scripts/setup-windows.ps1

# Chạy 16 test tự động với H2, không ảnh hưởng OrchidDB.
mvn clean test

# Build và chạy server với SQL Server thật. Giữ terminal này mở.
powershell -ExecutionPolicy Bypass -File scripts/run-local.ps1 -WindowsAuth
```

Ở terminal thứ hai, chạy seed sau khi server đã khởi động:

```powershell
sqlcmd -S '.\SQL2022' -E -C -b -i sql/02-seed-categories.sql
```

Ghi lại `category_id` thực tế. Seed có thể chạy nhiều lần mà không tạo trùng tên.
Nếu instance khác, truyền `-SqlInstance` cho script setup và dùng instance đó khi gọi sqlcmd.
Nếu port khác, truyền `-SqlPort` cho script run.

DLL nằm trong `tools/native/`, được Git bỏ qua. Nếu đổi version JDBC trong Maven,
cần đổi version DLL tương ứng trong script. Cách cấu hình DLL và `integratedSecurity`
tham khảo [tài liệu Microsoft JDBC](https://learn.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url).

## 4. Cách khác: dùng tài khoản SQL Server

Trong SSMS, chạy `sql/01-create-database.sql`. Trong PowerShell, cấu hình tài khoản
riêng của bạn rồi chạy server:

```powershell
$env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=OrchidDB;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME = 'YOUR_USERNAME'
$env:DB_PASSWORD = 'YOUR_PASSWORD'
mvn spring-boot:run
```

Các biến chỉ dùng trong terminal hiện tại. Khi chạy từ IntelliJ, đặt các biến tương tự
trong Run Configuration của `OrchidApplication`, chọn JDK 21.
Sau khi server chạy, seed bằng SSMS với `sql/02-seed-categories.sql`.

## 5. API và Postman

Base URL: `http://localhost:8080`. Xem chi tiết trong `docs/api-contract.md`.

| Method | Endpoint | Kết quả |
| --- | --- | --- |
| GET | `/api/orchids` | 200, danh sách |
| GET | `/api/orchids?name=cat` | 200, tìm tên không phân biệt hoa thường |
| GET | `/api/orchids/{id}` | 200 hoặc 404 |
| POST | `/api/orchids` | 201 hoặc 400 |
| PUT | `/api/orchids/{id}` | 200, 400 hoặc 404 |
| DELETE | `/api/orchids/{id}` | 204 hoặc 404 |

Import `postman/SBA301-Slot18-Orchid-API.postman_collection.json`.
Đặt collection variables `baseUrl`, `categoryId1` (Cattleya), `categoryId2`
(Dendrobium) theo dữ liệu thật. Chạy Collection Runner theo thứ tự đã sắp xếp.
Collection có 22 request, tự lấy ID từ POST và kiểm tra GET sau POST/PUT/DELETE.
Collection cố ý giữ lại một Orchid cuối cùng để làm T11/T12.

Body POST mẫu (đổi categoryId theo database của bạn):

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

Chạy collection bằng Newman nếu có Node.js:

```powershell
npm exec --yes --package=newman -- newman run postman/SBA301-Slot18-Orchid-API.postman_collection.json
```

## 6. Xác minh SQL Server và dữ liệu sau restart

1. Sau POST, GET ID vừa tạo; chạy `sql/03-verify.sql` để xem row và `category_id`.
2. Sau PUT, GET lại và SELECT, xác nhận mọi field cùng category đã đổi.
3. Sau DELETE, GET phải 404, SELECT không còn row; category vẫn còn.
4. T12: ghi ID `persistedId` từ request cuối collection, dừng server bằng Ctrl+C,
   chạy lại server, GET `/api/orchids/{persistedId}` phải trả 200 và dữ liệu cũ.

```powershell
sqlcmd -S '.\SQL2022' -E -C -b -i sql/03-verify.sql
```

`ddl-auto=update` giữ dữ liệu thật. Không đổi thành `create` hoặc `create-drop`
khi đang làm T12.

## 7. Kiểm thử tích hợp bằng SQL Server thật

`mvn clean test` mặc định dùng H2 ở test scope. Bộ test đó không chứng minh
persistence sau restart. Để chạy cùng 16 test trên SQL Server, dùng database
test **riêng** vì profile này tạo và xóa bảng:

```powershell
sqlcmd -S '.\SQL2022' -E -C -b -Q "IF DB_ID(N'OrchidLab4Test') IS NULL CREATE DATABASE OrchidLab4Test;"
$env:TEST_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=OrchidLab4Test;encrypt=true;trustServerCertificate=true;integratedSecurity=true'
$env:DB_USERNAME = ''
$env:DB_PASSWORD = ''
$env:PATH = (Join-Path (Get-Location) 'tools/native') + ';' + $env:PATH
mvn '-Dspring.profiles.active=sqlserver-test' test
```

Không đặt `TEST_DB_URL` trỏ vào database đang lưu dữ liệu cần giữ.

## 8. Tài liệu và evidence

- `docs/api-contract.md`: body, response, status và quy tắc PUT.
- `docs/test-matrix.md`: T01-T12, actual và đường dẫn evidence.
- `docs/break-it.md`: lỗi có chủ đích, nguyên nhân, sửa và chạy lại.
- `docs/concept-trace.md`: giải thích request từ HTTP đến database.
- `docs/ai-verification-log.md`: đề xuất AI và cách kiểm chứng.
- `evidence/console/`: log build, test, Hibernate SQL, startup/restart.
- `evidence/postman/`: báo cáo Newman thực thi collection Postman.
- `evidence/sqlserver/`: schema PK/FK và trạng thái row.

Evidence tự động là log và báo cáo JSON thật; nếu giảng viên yêu cầu ảnh giao diện
Postman/SSMS, mở các request và truy vấn tương ứng rồi chụp URL, status và dữ liệu.

## 9. Lỗi thường gặp

| Triệu chứng | Kiểm tra |
| --- | --- |
| Login failed | Tài khoản, mật khẩu, quyền truy cập đúng DB |
| TCP timeout | SQL Server đang chạy, TCP/IP bật, đúng port |
| Driver not configured for integrated authentication | Chạy setup Windows, DLL đúng version/x64 |
| POST 400 Category not found | Seed category, dùng ID thật |
| 404 ở mọi request | URL đúng là `/api/orchids`, đúng port |
| PUT mất field tùy chọn | PUT thay toàn bộ: gửi đầy đủ field cần giữ |

## 10. Git checkpoint

Thư mục ban đầu chưa có Git repository. Checkpoint hoàn thành được lưu bằng:

```powershell
git init
git add .
git commit -m "Complete SBA301 Slot18 Lab04 JPA REST"
git tag s18-lab04-complete
```

`target/`, DLL tải về, thư mục tạm và cấu hình local được bỏ qua trong `.gitignore`.

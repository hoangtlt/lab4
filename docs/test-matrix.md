# Test matrix - kết quả thực tế

Kiểm chứng ngày 07/10/2026 (Asia/Saigon), SQL Server instance `.\SQL2022`,
database ứng dụng `OrchidDB`. Category Cattleya=1, Dendrobium=2 tại lần chạy này.
Bộ test tự động SQL Server dùng `OrchidLab4Test`, tách khỏi DB ứng dụng.

Collection chạy từ POST để tạo precondition cho GET/search. ID Orchid chính
trong lần Newman thành công: 4; ID retest JSON: 5; ID giữ lại cho T11/T12: 6.

| ID | Precondition | Request / thao tác | Expected | Actual | Kết quả | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | Đã POST ID 4 | GET `/api/orchids` | 200, array | 200, array có ID 4 | PASS | `evidence/postman/newman-results.json`, request T01 |
| T02 | Có Cattleya Queen | GET `?name=cAt` | 200, tên chứa cat, bỏ hoa thường | 200, array khớp tên; test thêm no-match/blank cũng pass | PASS | Newman request T02; `evidence/console/sqlserver-tests.xml` |
| T03 | ID 4 vừa được tạo | GET `/api/orchids/4` | 200, đủ dữ liệu/category | 200, Cattleya Queen, category=1, không recursion | PASS | Newman T03 |
| T04 | ID 999999999 không có | GET ID không có | 404 | 404, body rỗng | PASS | Newman T04/B6 |
| T05 | Category 1 có thật | POST JSON hợp lệ, GET lại | 201, generated ID, GET 200 | 201 ID 4, GET 200 tên/category khớp | PASS | Newman T05/T03; test T05 kiểm tra bỏ ID client |
| T06 | Category 999999999 không có | POST category sai; POST thiếu category | 400, không insert | 400 `Category not found`; 400 `categoryId is required`; test count không đổi | PASS | Newman T06; SQL Server test T06 |
| T07 | ID 4, category 2 có thật | PUT đầy đủ, GET lại; PUT category sai rồi GET lại | 200 state mới; category sai 400 và không đổi state | 200 tên Updated, isNatural=false, category=2; invalid PUT 400; GET giữ state đúng | PASS | Newman T07; SQL Server test T07 SELECT FK=category mới |
| T08 | ID 999999999 không có | PUT ID không có | 404, không tạo mới | 404; repository count không tăng | PASS | Newman T08; SQL Server test T08 |
| T09 | ID 4 tồn tại | DELETE rồi GET và SELECT | 204 body rỗng; GET 404; không còn row | 204, body rỗng; GET 404; SQL count ID 4=0; category count=2 | PASS | Newman T09; `evidence/sqlserver/T09-T12-postconditions.txt` |
| T10 | ID 999999999 không có | DELETE ID không có | 404 | 404 | PASS | Newman T10 |
| T11 | POST ID 6 thuộc category 1 | GET, SELECT JOIN, xem sys.foreign_keys; kiểm tra inverse collection trong test | FK đúng, relationship Java/DB khớp | Row 6 có category_id=1, tên Cattleya; FK orchids.category_id → orchid_categories.category_id; inverse collection test pass | PASS | `evidence/sqlserver/schema-and-rows.txt`; SQL Server test T11 |
| T12 | ID 6 đã commit ở OrchidDB | GET trước, dừng JVM PID 5216, chạy JVM mới, GET cùng ID | 200, dữ liệu giữ nguyên | 200, toàn bộ JSON trước/sau giống nhau; SELECT ID 6 còn | PASS | `evidence/postman/T12-before-restart.json`, `T12-after-restart.json`; `evidence/console/application-restart.log` |

## Kết quả các bộ kiểm thử

- H2: 16 tests, failures=0, errors=0. `evidence/console/h2-tests.log`, `h2-tests.xml`.
- SQL Server: cùng 16 tests, failures=0, errors=0. `sqlserver-tests.log`, `sqlserver-tests.xml`.
- Newman: 22 requests, 33 assertions, failures=0. `evidence/postman/newman-console.log`.
- Clean build + test + JAR: BUILD SUCCESS, `evidence/console/final-build.log`.
- Break-It B1/B5/B6/B7: đã thực thi; xem `docs/break-it.md`.

Log và JSON là evidence tự động. Nếu yêu cầu nộp ảnh Postman/SSMS, chụp các request
và SQL đã chỉ rõ, không thay ảnh giao diện bằng ảnh tạo lại từ dữ liệu.
T12 không nằm trong 16 test H2: đã được kiểm tra riêng bằng restart ứng dụng thật.

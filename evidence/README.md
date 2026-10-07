# Evidence thực thi

Các file được tạo từ lần chạy thật trên máy hiện tại. Không chứa mật khẩu SQL.
URL/status/response chi tiết nằm trong `postman/newman-results.json`.

| Thư mục | Nội dung |
| --- | --- |
| console | Build, H2 tests, SQL Server tests, Hibernate DDL/DML, lỗi B1, startup/restart |
| postman | Newman runner thực thi Postman collection, JSON trước/sau restart T12 |
| sqlserver | Category seed, columns/identity/PK/FK, row đã lưu và row đã xóa |

Đối chiếu test ID trong `docs/test-matrix.md`. `OrchidLab4Test` chỉ dành cho test.
Schema/dữ liệu của ứng dụng và T12 được kiểm tra trong `OrchidDB`.
File XML Surefire có thể chứa đường dẫn máy cục bộ; không ảnh hưởng code hoặc run.

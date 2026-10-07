# Break-It Lab: lỗi, nguyên nhân, sửa và chạy lại

Các kết quả dưới đây đã thực thi, không chỉ là dự đoán. B1 chạy một JVM riêng
với cấu hình sai, không thay đổi cấu hình SQL Server đang dùng. B5/B6/B7 dùng
request lỗi rồi chạy lại request đúng trong collection. Có đủ 4 case theo đề.

| Case | Thay đổi có chủ đích / expected failure | Actual symptom | Layer / root cause | Fix | Retest và evidence |
| --- | --- | --- | --- | --- | --- |
| B1 | Đặt JDBC port thành 1; expected startup failure | Process exit 1; `TCP/IP connection ... port 1 has failed`, `Connection refused` | Configuration; không có SQL Server lắng nghe port 1; Hibernate không lấy được metadata | Dùng port thực tế 1433 | Startup/restart đúng thành công; GET 200. `evidence/console/B1-bad-port.log`, `application-restart.log`, `evidence/postman/T12-after-restart.json` |
| B5 | POST categoryId=999999999; expected 400 | 400, `Category not found: 999999999` | Service/business; `resolveCategory()` không tìm được category | Dùng categoryId đã seed | POST hợp lệ 201, GET 200, FK=1; `evidence/postman/newman-results.json`, `evidence/sqlserver/schema-and-rows.txt` |
| B6 | GET ID 999999999; expected 404 | 404, body rỗng | Controller/API; repository trả Optional.empty | Dùng ID thực tế nhận từ POST | GET ID được tạo trả 200; report Newman có cả request sai và đúng |
| B7 | Body JSON dở dang `{"orchidName":`; expected 400 | 400 Bad Request | HTTP/Jackson; body không parse được, chưa đến service | Gửi JSON hợp lệ và Content-Type application/json | Request `B7 - Retest valid JSON` trả 201; xóa dữ liệu retest trả 204; report Newman |

Kiểm tra thêm route sai `/api/orchid` trả 404, sửa URL thành `/api/orchids` trả 200.
Đây là kiểm tra URL client, không tuyên bố đã thay annotation RequestMapping.

## Tái hiện B1

Sau khi build project, chạy một process với port JDBC sai:

```powershell
$nativePath = Join-Path (Get-Location) 'tools/native'
java "-Djava.library.path=$nativePath" '-Dspring.datasource.url=jdbc:sqlserver://localhost:1;databaseName=OrchidDB;encrypt=true;trustServerCertificate=true;integratedSecurity=true;loginTimeout=2' '-Dspring.datasource.hikari.connection-timeout=3000' '-Dserver.port=8081' -jar target/lab4-orchid-api-0.0.1-SNAPSHOT.jar
```

Sửa bằng cách chạy lại `scripts/run-local.ps1 -WindowsAuth`, dùng port 1433.
Nếu máy bạn dùng port khác, dùng `-SqlPort` đúng với SQL Server của bạn.

## Regression

16 test tích hợp trên SQL Server đều pass. Collection chạy đủ 22 request,
33 assertion không lỗi. Case B5/B6/B7 đi qua server thật. Case B1 được sửa bằng
startup thành công và GET sau restart. Dữ liệu giữ nguyên sau khi dừng JVM cũ.

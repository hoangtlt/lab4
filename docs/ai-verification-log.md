# AI Verification Log

Log ghi đề xuất đã được kiểm tra bằng thực thi. Sinh viên cần tự chạy lại,
đọc SQL và giải thích bằng lời của mình để hoàn thành Human Verification Gate.

| AI suggestion | Kiểm tra bằng gì? | Kết quả thực tế | Accept/Reject + lý do |
| --- | --- | --- | --- |
| mappedBy phải là field `orchidCategory` | Startup SQL Server và T11 Java object graph | Startup thành công, category có collection Orchid tương ứng | Accept: mappedBy dùng field Java |
| Lấy category có thật trước save | POST category không có + SELECT và test count | 400; không tạo thêm category/Orchid | Accept: quan hệ được resolve ở service |
| JsonIgnore trên collection inverse tránh recursion | GET detail bằng MockMvc và Newman | Category có ID/tên, không serialize orchids | Accept: JSON đọc được, không vòng lặp |
| Không dùng cascade ALL cho quan hệ này | DELETE rồi GET/SELECT categories | Orchid bị xóa, 2 category còn | Accept: giữ category dùng chung |
| PUT invalid category phải giữ state cũ | PUT sai rồi GET và test SQL Server | 400; tên/FK cũ giữ nguyên | Accept: validate trước thay state, write trong transaction |
| H2 pass có thể thay evidence SQL Server | Chạy riêng 16 test SQL Server + Newman + restart + SELECT | Database SQL Server thật có FK và dữ liệu sau restart | Reject: chỉ H2 không chứng minh SQL Server/persistence |
| ID category luôn là 1/2 | SELECT sau seed; test tự dùng ID sinh ra | DB ứng dụng sạch có 1/2; DB test identity tăng | Reject: phải dùng ID thực tế |
| POST/PUT/DELETE status thành công là đủ | GET hậu điều kiện, SELECT FK/row, JVM restart | GET và DB khớp thao tác; ID 6 còn sau restart | Reject: cần hậu điều kiện, không chỉ status |

Một lỗi trong script Postman đã được phát hiện ở lần chạy đầu: biến `data`
trùng binding có sẵn trong sandbox Newman, làm script lấy ID không chạy.
Đã đổi tên biến, chạy lại collection và xác nhận 22 request/33 assertion pass.
Lỗi này nằm ở công cụ kiểm thử, không phải endpoint CRUD.

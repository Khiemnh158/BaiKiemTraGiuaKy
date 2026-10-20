# Bài kiểm tra giữa kỳ - Quản lý món ăn

**Sinh viên:** Nguyễn Đức Khiêm  
**MSSV:** BIT240125  
**Môn học:** Phát triển ứng dụng di động  
**Project:** `BIT240125_FoodManager`

## Nội dung nộp bài

| Nội dung | Đường dẫn |
|---|---|
| Mã nguồn Android Native Java | [BIT240125_FoodManager](BIT240125_FoodManager) |
| Video 1 - Demo ứng dụng, offline và tự đồng bộ | [BIT240125_Video1_Demo.mp4](BIT240125_Video1_Demo.mp4) |
| Video 2 - Thuyết minh mã nguồn và chức năng | [BIT240125_Video2_ThuyetMinh.mp4](BIT240125_Video2_ThuyetMinh.mp4) |
| APK để cài chạy thử | [BIT240125_FoodManager.apk](BIT240125_FoodManager.apk) |
| Hướng dẫn chi tiết 6 trang | [BIT240125_HuongDan.pdf](BIT240125_HuongDan.pdf) |

## Công nghệ và chức năng

- Android Native Java, SQLite và MockAPI REST.
- Danh sách, chi tiết, thêm/sửa/xóa có xác nhận; quản lý loại món.
- Tìm theo tên, lọc phối hợp loại/trạng thái/khoảng giá; sắp xếp và giữ bộ lọc.
- Nhiều ảnh qua HTTPS, đổi ảnh đại diện; Quick khi thời gian <= 15 phút.
- English mặc định, chuyển Tiếng Việt, dịch validation và trạng thái.
- Lưu offline vào SQLite; hàng đợi thay đổi và tự đồng bộ khi có mạng.
- Tên bảng/cột nghiệp vụ có tiền tố `BIT240125_`. Dữ liệu mẫu: 3 loại, 6 món; API có thêm các món minh chứng offline.

## Mở mã nguồn

Mở thư mục **BIT240125_FoodManager** bằng Android Studio. Dự án có Gradle wrapper; yêu cầu Android SDK 34, thiết bị Android 8.0 trở lên. Bản đã biên dịch dùng JBR 25, Gradle 9.6.0 và Android Gradle Plugin 9.4.1.

Hướng dẫn cấu trúc, model, schema, validation và đồng bộ: [README của project](BIT240125_FoodManager/README.md).

## Video

- **Video 1:** 05:43, quay trực tiếp màn hình emulator; phần offline/MockAPI bắt đầu khoảng 02:50. Video không có lời đọc.
- **Video 2:** 07:04, thuyết minh tiếng Việt bằng giọng tổng hợp; phần MockAPI bắt đầu khoảng 05:38. Hình ảnh trích đoạn mã nguồn thực tế có số dòng.

## Kiểm chứng và phạm vi

Đã chạy 67 kiểm tra dữ liệu và 11 kiểm tra giao diện. Các tệp trong [evidence](BIT240125_FoodManager/evidence) lưu log cùng JSON trước/sau đồng bộ: bản ghi tạo/sửa offline lên MockAPI với đúng một UUID và giá 40.000.

Luồng đã kiểm chứng là tạo/sửa từ ứng dụng khi offline rồi đẩy lên API. Chưa triển khai đồng bộ xóa thực hiện trực tiếp trên website MockAPI về local hoặc xử lý xung đột nhiều thiết bị. Worker đồng bộ hoạt động khi ứng dụng đang mở. Ở đoạn bật mạng trong Video 1, ứng dụng được mở lại trong kịch bản kiểm thử; không có thao tác bấm nút đồng bộ.

MockAPI có trường `id` do dịch vụ sinh ra để định tuyến REST; khóa nghiệp vụ là `BIT240125_Id`. Đề cho phép AI hỗ trợ; người học cần đọc hiểu và tự giải thích được mã nguồn. Video 2 sử dụng giọng đọc tổng hợp.

## Nộp bài theo đề

Nộp **link repository và Video 1 trên LMS trong ca học**; **Video 2 chậm nhất 23/10/2026**. Repository này lưu các file để truy cập; việc tải lên GitHub không thay thế thao tác nộp LMS.

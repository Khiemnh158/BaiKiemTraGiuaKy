# BIT240125_FoodManager

**Sinh viên:** Nguyễn Đức Khiêm - **MSSV:** BIT240125  
**Bài giữa kỳ:** Phát triển ứng dụng di động - Quản lý món ăn  
**Ngày hoàn thiện:** 20/10/2026

## 1. Phạm vi và công nghệ

Ứng dụng Android Native viết bằng Java, lưu SQLite trên thiết bị và tự đồng bộ với hai tài nguyên MockAPI thật. Giao diện màu hồng theo yêu cầu của sinh viên; đề không quy định màu giao diện. Mặc định giao diện English, có nút VI/EN trên các màn hình. Dữ liệu tên/mô tả do người dùng nhập giữ nguyên, không dịch tự động.

Bản cài đặt là APK debug dùng cho học tập và demo. Không cần tài khoản để sử dụng ứng dụng. Yêu cầu Android 8.0/API 26 trở lên. Không xóa dữ liệu ứng dụng trong lúc đang có thay đổi offline chưa đồng bộ.

## 2. Mở và chạy trong Android Studio

1. Giải nén `BIT240125_MaNguon.zip`, mở thư mục **BIT240125_FoodManager** bằng Android Studio.
2. Chọn **Gradle JDK** phù hợp với Gradle 9.6/AGP 9.4.1; bản đã kiểm tra dùng JBR 25 đi kèm Android Studio trên máy này. Cài Android SDK Platform 34 nếu máy chưa có.
3. Chờ Gradle Sync; lần đầu trên máy khác cần Internet để tải bộ công cụ. Android Studio tự tạo `local.properties` theo vị trí SDK của máy.
4. Tạo emulator hoặc kết nối điện thoại Android 8 trở lên, chọn module `app`, bấm Run.
5. Có thể cài trực tiếp `BIT240125_FoodManager.apk` để xem ứng dụng mà chưa cần biên dịch source.

Biên dịch từ terminal tại thư mục project:

```powershell
.\gradlew.bat assembleDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

## 3. Cấu trúc mã nguồn

```text
BIT240125_FoodManager/
  settings.gradle              # rootProject.name đúng MSSV
  app/build.gradle             # namespace, SDK, Java và test runner
  app/src/main/
    AndroidManifest.xml        # quyền Internet và kiểm tra mạng
    res/values/styles.xml      # theme hồng, màu nhấn
    assets/food_0.jpg ...       # ảnh mẫu dùng được offline
    java/vn/edu/bit240125/foodmanager/
      MainActivity.java        # danh sách, chi tiết, form, loại món, cài đặt
      Texts.java               # 99 khóa giao diện/thông báo song ngữ
      ImageLoader.java         # tải HTTPS, ảnh thay thế, bộ nhớ đệm
      model/
        DishCategory.java
        Dish.java
        DishImage.java
      data/
        Contract.java          # tên bảng và cột có BIT240125_
        FoodRepository.java    # schema, seed, CRUD, validation, outbox
        SeedImages.java
        ValidationException.java
      sync/
        SyncConfig.java        # 02 endpoint MockAPI của bài
        SyncEngine.java        # tự phát hiện mạng, đẩy/đọc và thử lại
  app/src/androidTest/.../FoodTestRunner.java
```

## 4. Thiết kế dữ liệu và tiền tố MSSV

Database: `BIT240125_FoodManager.db`.

| Bảng | Cột nghiệp vụ |
|---|---|
| BIT240125_DishCategory | BIT240125_Id (PK), BIT240125_Name, BIT240125_Description |
| BIT240125_Dish | BIT240125_Id (PK), BIT240125_Name, BIT240125_NameKey, BIT240125_Price, BIT240125_PreparationTime, BIT240125_IsAvailable, BIT240125_Description, BIT240125_DishCategoryId (FK) |
| BIT240125_DishImage | BIT240125_Id (PK), BIT240125_DishId (FK), BIT240125_ImageUrl, BIT240125_IsThumbnail |
| BIT240125_Outbox | BIT240125_Entity, BIT240125_EntityId, BIT240125_Operation, BIT240125_Revision |

`Contract.P = "BIT240125_"` là nguồn dùng chung để đặt tên. Mỗi bảng và mỗi cột do ứng dụng định nghĩa đều có tiền tố này. Các bảng metadata nội bộ SQLite/Android do hệ thống quản lý không phải model nghiệp vụ của bài.

Quan hệ: **DishCategory 1 - N Dish 1 - N DishImage**. Mỗi món bắt buộc có một loại. Khóa ngoại được bật bằng `setForeignKeyConstraintsEnabled(true)`. Loại đang có món dùng `ON DELETE RESTRICT`; ảnh dùng `ON DELETE CASCADE` khi xóa món.

Giá dùng số nguyên `long` theo đồng Việt Nam, tránh sai số số thực. Id món mới là UUID ổn định. Cột NameKey lưu tên đã trim, gom khoảng trắng, chuẩn hóa Unicode NFC và chuyển chữ thường. `UNIQUE(DishCategoryId, NameKey)` chặn trùng tên trong cùng loại. Chỉ mục duy nhất có điều kiện trên bảng ảnh chặn hơn một ảnh đại diện. Không có ảnh hoặc không chọn đại diện vẫn hợp lệ vì đề quy định **tối đa một**, không bắt buộc đúng một.

## 5. Dữ liệu mẫu

| Món | Loại | Giá | Phút | Trạng thái | Phân loại |
|---|---|---:|---:|---|---|
| Cơm rau củ | Món chính | 65.000 | 20 | Đang bán | Regular |
| Mì Ý rau xanh | Món chính | 55.000 | 15 | Đang bán | Quick |
| Salad rau củ | Món nhẹ | 35.000 | 10 | Đang bán | Quick |
| Súp bí đỏ | Món nhẹ | 30.000 | 18 | Ngừng bán | Regular |
| Trà trái cây | Đồ uống | 25.000 | 5 | Đang bán | Quick |
| Nước cam tươi | Đồ uống | 30.000 | 8 | Đang bán | Quick |

Có 3 loại và 6 món mẫu khi tạo database. Sau khi đồng bộ, có thể thấy thêm món minh chứng offline hiện đang được giữ trên MockAPI (sau hai lần minh chứng có tổng 8 món), hoặc dữ liệu bạn đã thêm. Món đầu có hai ảnh để minh họa gallery. Ảnh minh họa được tải từ các URL Unsplash trong `SeedImages.java`, đóng gói trong assets; ảnh của người dùng nhập chỉ được cache sau khi tải thành công.

## 6. Chức năng và cách dùng

### Danh sách, tìm kiếm và bộ lọc

Gõ một phần tên vào ô Search. Tìm kiếm không phân biệt hoa/thường nhưng có phân biệt dấu tiếng Việt. Nút Filters phối hợp loại món, chỉ đang bán và khoảng giá. Bỏ trống một đầu khoảng giá nghĩa là không giới hạn đầu đó. Giá lọc phải không âm; min không được lớn hơn max. Nút sắp xếp có tên A-Z, giá tăng, giá giảm, thời gian tăng. Bộ lọc và sắp xếp được lưu trong SharedPreferences, nên giữ khi quay lại từ chi tiết/form và khi mở lại ứng dụng. Nếu không có kết quả, có thông báo và nút xóa bộ lọc.

### Chi tiết và ảnh

Bấm một món để xem giá, trạng thái, thời gian, mô tả và ảnh. Nút Set cover đổi ảnh đại diện, đồng thời bỏ cờ ở các ảnh còn lại và ghi lại SQLite. Form cho thêm URL ảnh HTTPS và gỡ từng ảnh. URL trùng trong một món bị chặn. Ảnh rỗng, không tải được hoặc đang offline mà chưa cache hiển thị hình thay thế, không làm ứng dụng dừng.

### Thêm và sửa

Bấm Add dish hoặc Edit dish. Chọn loại từ dữ liệu SQLite. Tên không được rỗng; giá nguyên dương; thời gian nguyên dương; loại còn tồn tại; tên không trùng trong cùng loại. Món được lưu cùng ảnh và outbox trong một transaction. Kết quả hiển thị lại ở chi tiết. Nếu sửa một món không còn tồn tại, ứng dụng thông báo thay vì tạo nhầm món mới.

### Xóa và quản lý loại

Xóa món luôn có xác nhận. Hủy giữ nguyên dữ liệu. Đồng ý xóa cả ảnh của món. Màn hình Categories cho thêm, sửa và xóa loại; loại đang được sử dụng bị chặn xóa ở cả lớp xử lý và khóa ngoại. Món ngừng bán vẫn có trong danh sách quản lý; khi bật bộ lọc đang bán, món đó bị loại ra.

### Ngôn ngữ

Lần đầu là English. Bấm VI để đổi sang tiếng Việt, EN để đổi về tiếng Anh. Nhãn, nút, trạng thái và thông báo lỗi đều được dịch. Lựa chọn được dùng trên danh sách, chi tiết, thêm, sửa và các màn hình phụ; có lưu qua lần mở ứng dụng. Nội dung tên/mô tả của dữ liệu không tự đổi ngôn ngữ.

## 7. Luồng đọc và ghi

- Đọc: MainActivity -> FoodRepository -> SQLite -> model -> hiển thị.
- Ghi: form -> captureDraft -> validate -> transaction (món + ảnh + outbox) -> đọc lại chi tiết.
- Xóa: xác nhận -> kiểm tra tồn tại/ràng buộc -> transaction xóa + tombstone outbox -> danh sách.
- Phân loại thời gian: `PreparationTime <= 15` -> Quick/Nhanh; còn lại -> Regular/Thông thường. Không lưu cột dư.
- Bất kỳ lỗi nghiệp vụ nào dùng ValidationException với khóa thông báo. Giao diện tra Texts theo ngôn ngữ. Lỗi lưu trữ khác có thông báo chung có thể thử lại.

## 8. MockAPI và phần nâng cao

Project đã tạo trong tài khoản MockAPI đang mở theo yêu cầu của bạn:

- Project quản lý: https://mockapi.io/projects/6ad71854f71f154ac487c193
- Loại món: https://6ad71854f71f154ac487c192.mockapi.io/api/v1/BIT240125_DishCategory
- Món ăn: https://6ad71854f71f154ac487c192.mockapi.io/api/v1/BIT240125_Dish

Hai URL được cấu hình mặc định trong SyncConfig; có thể thay ở Settings. Để tắt sử dụng dịch vụ, xóa cả hai URL và lưu; SQLite vẫn hoạt động. Không có nút Đồng bộ. Việc lưu cài đặt chỉ thay cấu hình; các thao tác sau đó đồng bộ tự động.

MockAPI tự có trường `id` làm mã bản ghi của dịch vụ. Ứng dụng chỉ dùng nó làm địa chỉ PUT/DELETE. Mã nghiệp vụ vẫn là `BIT240125_Id`, và toàn bộ trường tự định nghĩa mang tiền tố. Danh sách ảnh trên API là mảng `BIT240125_DishImage`, mỗi phần tử có `BIT240125_ImageUrl` và `BIT240125_IsThumbnail`.

### Thuật toán

1. Ghi local ngay cả khi không mạng. Mỗi entity chỉ có một mục outbox hiện hành, kèm Operation và Revision mới.
2. NetworkCallback theo dõi kết nối Internet đã được Android xác nhận. Khi mất mạng, giữ outbox. Khi có mạng, tự chạy worker. Có thử lại định kỳ mỗi 30 giây trong lúc ứng dụng mở.
3. Worker chạy đơn luồng để tránh hai lần đồng bộ cùng lúc. GET danh mục và món để ánh xạ UUID local sang `id` dịch vụ.
4. Đẩy xóa món trước, thêm/sửa loại trước thêm/sửa món, xóa loại sau. Với upsert, tìm UUID rồi PUT; chỉ POST khi UUID chưa có. Nếu POST thành công nhưng mất phản hồi, lần thử tiếp theo GET thấy UUID và không POST trùng.
5. Chỉ acknowledge mục outbox có cùng Revision đã gửi. Nếu người dùng vừa sửa tiếp, revision mới còn lại để gửi lần sau.
6. Sau khi đẩy, GET mới và nhập dữ liệu hợp lệ từ API về. Bản ghi local còn pending không bị dữ liệu remote ghi đè. Các lỗi HTTP/JSON/validation giữ dữ liệu local và cho phép tự thử lại.

Phạm vi bài là một thiết bị quản lý với chiến lược ưu tiên thay đổi local đang chờ. Worker hoạt động khi ứng dụng đang mở; không triển khai dịch vụ nền sau khi đóng app. Các chỉnh sửa remote hợp lệ được tải về theo chu kỳ. Chưa triển khai hợp nhất xung đột nhiều thiết bị hay truyền xóa thực hiện trực tiếp trên website tới các thiết bị khác; luồng xóa thực hiện từ ứng dụng được đồng bộ bằng tombstone.

### Minh chứng offline

Video 1 có đoạn tắt Wi-Fi/dữ liệu của emulator, tạo món `Mon offline BIT240125 v2`, sửa giá thành 40.000, sau đó bật mạng. Không bấm nút đồng bộ. Ứng dụng tự gửi lên API; kiểm thử xác nhận đúng một bản ghi có UUID tương ứng và giá 40.000. Các tệp JSON trước/sau và log kiểm chứng được kèm trong thư mục evidence của gói nguồn khi hoàn thiện bàn giao.

## 9. Kiểm thử

Đã chạy 67 kiểm tra dữ liệu và 11 kiểm tra giao diện trên emulator. Con số 67 tính riêng từng cột/tên bảng khi kiểm tra tiền tố, không có nghĩa là 67 chức năng. Test runner nằm trong androidTest, không được đóng gói trong APK người dùng.

```powershell
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e mode test vn.edu.bit240125.foodmanager.test/vn.edu.bit240125.foodmanager.FoodTestRunner
```

Chế độ `test` giả định dữ liệu nền ban đầu là 3 loại/6 món. Nếu đã thêm dữ liệu demo hoặc dữ liệu của mình, nên chạy trên emulator mới để không ảnh hưởng dữ liệu đang dùng. Chế độ `ui` kiểm tra thao tác và thông báo của các màn hình. Các chế độ demo/waitSync/syncOnline dùng để tạo video và kiểm chứng dịch vụ thật, không phải tính năng ẩn của ứng dụng.

## 10. Nội dung video và nộp bài

- **Video 1:** quay trực tiếp emulator, thể hiện tên/MSSV, danh sách, chi tiết, gallery, đổi đại diện, thêm/sửa/xóa có xác nhận, bộ lọc kết hợp, sắp xếp, validation, loại đang dùng, đổi ngôn ngữ, offline rồi tự đồng bộ thật.
- **Video 2:** thuyết minh tiếng Việt bằng giọng tổng hợp, kèm đoạn source thực tế và số dòng: cấu trúc, model/quan hệ, công nghệ, SQLite, tiền tố MSSV, CRUD, validation, bộ lọc, ảnh, đa ngôn ngữ và sync.
- Bài sử dụng hỗ trợ AI theo điều khoản cho phép của đề. Bạn vẫn cần đọc hiểu và giải thích được code; video tổng hợp không thay thế việc tự chuẩn bị demo trực tiếp.
- Theo đề: **mã nguồn bằng link Git và Video 1 nộp trong ca học**; **Video 2 chậm nhất 23/10/2026**. Chưa có thao tác nộp LMS hoặc công bố Git thay bạn.

## 11. Đưa source lên GitHub

Mã nguồn được đưa lên repository https://github.com/Khiemnh158/BaiKiemTraGiuaKy. Tạo repository `BIT240125_FoodManager`, tải các file trong gói mã nguồn lên hoặc dùng Git tại máy. Không đưa local.properties, .gradle, build hay .idea; .gitignore đã được chuẩn bị. Giữ nguyên gradle wrapper, source, assets và tài liệu. Nếu repository để riêng tư, cấp quyền giảng viên theo quy định lớp. Lấy URL repository nộp cùng Video 1.

## 12. Điểm cần tự giải thích khi được hỏi

1. Vì sao khóa ngoại ngăn xóa loại đang dùng? (RESTRICT và kiểm tra COUNT.)
2. Vì sao tên trùng cùng loại bị chặn nhưng khác loại được phép? (UNIQUE trên CategoryId + NameKey.)
3. Vì sao nhiều ảnh không thể cùng là đại diện? (bỏ cờ còn lại + partial unique index.)
4. Vì sao lưu được khi offline? (SQLite là nơi đọc/ghi chính.)
5. Vì sao retry không tạo thêm bản ghi? (UUID ổn định, GET ánh xạ, PUT thay cho POST.)
6. Vì sao sửa trong lúc đang sync không mất dữ liệu? (Revision mới không bị acknowledge của revision cũ xóa.)
7. Vì sao dữ liệu lọc không mất khi quay lại? (giữ biến trạng thái và SharedPreferences.)
8. Vì sao không cần cột Quick? (suy ra từ thời gian <= 15.)

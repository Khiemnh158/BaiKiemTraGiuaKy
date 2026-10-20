# Lời thuyết minh Video 2

Tên và MSSV hiển thị trong video; giọng nói là giọng tổng hợp.

## 1. Tổng quan bài làm
Video này trình bày ứng dụng quản lý món ăn bằng Android Native Java. Ứng dụng sử dụng SQLite để lưu dữ liệu trên thiết bị, hoạt động được khi không có Internet, và có giao diện tiếng Anh cùng tiếng Việt. Tên sinh viên, mã sinh viên và tên project được hiển thị trên màn hình. Phần trình bày lần lượt đi qua cấu trúc mã nguồn, mô hình dữ liệu, các chức năng, kiểm tra dữ liệu, và phương án đồng bộ mở rộng.

## 2. Cấu trúc project và công nghệ
Trong project, thư mục model chứa loại món, món ăn và ảnh của món. Thư mục data chứa các hằng số đặt tên bảng, lớp quản lý SQLite và lỗi kiểm tra dữ liệu. Main Activity xây dựng các màn hình Android. Lớp Texts cung cấp bản dịch. Image Loader tải ảnh và lưu bộ nhớ đệm. Sync Engine phụ trách phần đồng bộ tùy chọn. Ứng dụng yêu cầu Android tám trở lên, không cần cài thư viện giao diện bên ngoài và không cần Internet để sử dụng các chức năng cơ bản.

## 3. Model và quan hệ dữ liệu
Đối tượng loại món có mã định danh, tên và mô tả. Mỗi món ăn có tên, giá, thời gian chế biến, trạng thái đang bán, mô tả, khóa ngoại đến loại món và danh sách ảnh. Mỗi ảnh có đường dẫn và cờ ảnh đại diện. Một loại món có nhiều món ăn. Một món chỉ thuộc một loại, nhưng có thể có nhiều ảnh. Hàm is Quick trả về đúng nếu thời gian chế biến nhỏ hơn hoặc bằng mười lăm phút. Vì đây là dữ liệu có thể tính được, ứng dụng không lưu thêm một cột phân loại thời gian.

## 4. MSSV trong project, bảng và cột
Đây là đoạn mã quan trọng về quy tắc đặt tên. Hằng số P chứa tiền tố mã sinh viên có dấu gạch dưới ở cuối. Tên các bảng loại món, món ăn, ảnh và hàng đợi đều được ghép từ tiền tố này. Tương tự, tất cả tên cột như mã định danh, tên, giá, thời gian chế biến và khóa ngoại đều có tiền tố. Tên project được đặt trong file settings của Gradle và xuất hiện ở phần mở đầu. Như vậy, quy tắc mã sinh viên được áp dụng vào dữ liệu thực sự lưu trữ, không chỉ vào tên thuộc tính của model.

## 5. SQLite: khóa ngoại và ràng buộc
Khi cơ sở dữ liệu được tạo lần đầu, ứng dụng tạo bốn bảng. Khóa ngoại từ món ăn đến loại món dùng quy tắc hạn chế xóa. Vì vậy, loại món đang chứa món ăn không thể bị xóa. Khóa ngoại từ ảnh đến món dùng xóa dây chuyền để tránh ảnh mồ côi. Cặp loại món và tên đã chuẩn hóa có ràng buộc duy nhất. Một chỉ mục duy nhất có điều kiện bảo đảm mỗi món chỉ có tối đa một ảnh đại diện. Ngoài ra, giá và thời gian chế biến phải lớn hơn không. Cơ sở dữ liệu được khởi tạo với ba loại và sáu món mẫu.

## 6. Thêm, sửa, xóa và luồng lưu dữ liệu
Khi người dùng lưu form, giao diện thu thập dữ liệu và gọi hàm lưu món. Món mới nhận mã định danh dạng UUID. Khi sửa, ứng dụng kiểm tra mã định danh còn tồn tại. Dữ liệu được kiểm tra trước khi ghi. Thông tin món, danh sách ảnh và thay đổi trong hàng đợi được lưu trong cùng một giao dịch SQLite. Nếu có lỗi, giao dịch được hoàn tác. Khi lưu thành công, màn hình chi tiết đọc lại dữ liệu đã lưu. Thao tác xóa có hộp thoại xác nhận; sau khi đồng ý, món và ảnh liên quan được xóa, rồi danh sách được cập nhật.

## 7. Validation và thông báo lỗi
Hàm kiểm tra dữ liệu lần lượt kiểm tra tên, giá, thời gian chế biến và loại món. Tên được cắt khoảng trắng, chuẩn hóa Unicode và chuyển chữ thường để phát hiện trùng trong cùng loại. Cùng một tên ở hai loại khác nhau vẫn được phép. Ảnh phải có đường dẫn HTTPS hợp lệ, không được thêm trùng và không có quá một ảnh đại diện. Các lỗi được biểu diễn bằng khóa thông báo. Giao diện chuyển khóa này thành nội dung theo ngôn ngữ hiện tại. Mã không tồn tại, danh sách trống và ảnh lỗi cũng được xử lý bằng thông báo hoặc hình thay thế phù hợp.

## 8. Tìm kiếm, bộ lọc và sắp xếp
Danh sách áp dụng đồng thời tìm kiếm một phần tên, lọc loại món, trạng thái đang bán và khoảng giá. Phần truy vấn dùng tham số để truyền dữ liệu lọc. Tìm kiếm tên được thực hiện sau khi chuẩn hóa và xử lý ký tự phần trăm như ký tự thông thường. Có các lựa chọn sắp xếp theo tên, giá tăng, giá giảm và thời gian chế biến tăng. Bộ lọc được lưu trong tùy chọn của ứng dụng, nên vẫn còn sau khi mở chi tiết, sửa món hoặc mở lại ứng dụng. Khoảng giá âm hoặc giá thấp nhất lớn hơn giá cao nhất bị từ chối. Món ngừng bán vẫn xuất hiện trong danh sách quản lý chung.

## 9. Ảnh đại diện và mốc 15 phút
Form cho phép thêm nhiều ảnh bằng đường dẫn và gỡ từng ảnh. Trong màn hình chi tiết, người dùng có thể chọn một ảnh khác làm đại diện. Khi chọn, ứng dụng đặt cờ cho ảnh được chọn và bỏ cờ của các ảnh còn lại, sau đó lưu lại SQLite. Ảnh mẫu được đóng gói cùng ứng dụng; ảnh mới tải thành công được lưu bộ nhớ đệm. Nếu không có ảnh hoặc không tải được ảnh, giao diện có hình thay thế. Nhãn thời gian được tính trực tiếp từ model: mười lăm phút trở xuống là nhanh, lớn hơn mười lăm phút là thông thường.

## 10. Đa ngôn ngữ và duy trì lựa chọn
Ứng dụng mặc định tiếng Anh vì cờ ngôn ngữ ban đầu là sai. Nút chuyển tiếng Việt hoặc tiếng Anh xuất hiện ở phần đầu mỗi màn hình. Lựa chọn được lưu lại và dùng chung khi mở danh sách, form, chi tiết hay cài đặt. Lớp Texts chứa cặp bản dịch cho nhãn, nút, trạng thái, hộp thoại và lỗi kiểm tra dữ liệu. Khi đổi ngôn ngữ trong form, dữ liệu đang nhập được thu thập trước khi dựng lại giao diện. Tên và mô tả món do người dùng nhập không bị tự động dịch, đúng với yêu cầu đề bài.

## 11. Offline và tự động đồng bộ MockAPI thật
Các chức năng cơ bản luôn đọc và ghi SQLite, vì vậy vẫn sử dụng được khi tắt Internet. Hai endpoint Mock API thật đã được cấu hình sẵn cho loại món và món ăn. Mỗi thay đổi được đưa vào hàng đợi trong cùng giao dịch lưu dữ liệu. Lớp đồng bộ theo dõi kết nối mạng bằng Network Callback và tự chạy trên một luồng nền khi Internet trở lại. Trước khi tạo bản ghi, chương trình tìm mã định danh ổn định trên máy chủ để tránh tạo trùng khi thử lại. Sau khi gửi thành công, chỉ phiên bản hàng đợi tương ứng được xóa. Dữ liệu từ máy chủ được đọc về sau khi đẩy thay đổi, và bản ghi local còn chờ luôn được bảo vệ. Video demo minh chứng tạo và sửa khi mất mạng, sau đó tự đồng bộ lên dịch vụ thật mà không bấm nút đồng bộ.

## 12. Kiểm chứng và chuẩn bị nộp bài
Phần kiểm chứng gồm sáu mươi bảy kiểm tra dữ liệu và mười một kiểm tra giao diện đã chạy trên emulator, cùng kiểm tra đồng bộ với Mock API thật. Các kiểm tra bao gồm tiền tố bảng và cột, thêm sửa xóa, ràng buộc dữ liệu, bộ lọc, sắp xếp, ảnh và chuyển ngôn ngữ. Video demo được quay trực tiếp từ màn hình ứng dụng với giao diện màu hồng. Gói bàn giao có mã nguồn, file cài đặt Android, tài liệu hướng dẫn và hai video. Video thuyết minh dùng giọng đọc tổng hợp. Trước khi nộp, cần hiểu mã nguồn, chạy lại ứng dụng trên máy của mình và đưa project lên kho Git để lấy liên kết theo quy định của đề.

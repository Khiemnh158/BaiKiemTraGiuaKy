package vn.edu.bit240125.foodmanager;

import java.util.*;

public final class Texts {
  private static final Map<String, String[]> VALUES = new HashMap<>();

  static {
    VALUES.put("app", new String[] {"FoodManager", "Quản lý món ăn"});
    VALUES.put("kitchen", new String[] {"YOUR KITCHEN, ORGANIZED", "GỌN GÀNG GIAN BẾP CỦA BẠN"});
    VALUES.put("catalog", new String[] {"Dish collection", "Danh sách món ăn"});
    VALUES.put(
        "intro",
        new String[] {
          "Fresh ideas. Every dish in one place.", "Mọi món ăn, quản lý trong tầm tay."
        });
    VALUES.put("search", new String[] {"Search dish name…", "Tìm theo tên món…"});
    VALUES.put("filters", new String[] {"Filters", "Bộ lọc"});
    VALUES.put("sort", new String[] {"Sort", "Sắp xếp"});
    VALUES.put("add", new String[] {"Add dish", "Thêm món"});
    VALUES.put("categories", new String[] {"Categories", "Loại món"});
    VALUES.put("settings", new String[] {"Settings", "Cài đặt"});
    VALUES.put("all_categories", new String[] {"All categories", "Tất cả loại món"});
    VALUES.put("selling_only", new String[] {"Available dishes only", "Chỉ món đang bán"});
    VALUES.put("min_price", new String[] {"Minimum price (VND)", "Giá thấp nhất (VNĐ)"});
    VALUES.put("max_price", new String[] {"Maximum price (VND)", "Giá cao nhất (VNĐ)"});
    VALUES.put("apply", new String[] {"Apply", "Áp dụng"});
    VALUES.put("reset", new String[] {"Reset", "Đặt lại"});
    VALUES.put("clear_filters", new String[] {"Clear filters", "Xóa bộ lọc"});
    VALUES.put("cancel", new String[] {"Cancel", "Hủy"});
    VALUES.put("back", new String[] {"Back", "Quay lại"});
    VALUES.put("save", new String[] {"Save dish", "Lưu món"});
    VALUES.put("edit", new String[] {"Edit dish", "Sửa món"});
    VALUES.put("delete", new String[] {"Delete", "Xóa"});
    VALUES.put(
        "confirm_delete",
        new String[] {
          "Delete this dish? This action removes its images too.",
          "Xóa món này? Các ảnh của món cũng sẽ bị xóa."
        });
    VALUES.put("confirm_category", new String[] {"Delete this category?", "Xóa loại món này?"});
    VALUES.put("name", new String[] {"Dish name", "Tên món"});
    VALUES.put("price", new String[] {"Price (VND)", "Giá (VNĐ)"});
    VALUES.put("time", new String[] {"Preparation time (minutes)", "Thời gian chế biến (phút)"});
    VALUES.put("category", new String[] {"Category", "Loại món"});
    VALUES.put("available", new String[] {"Available", "Đang bán"});
    VALUES.put("unavailable", new String[] {"Unavailable", "Ngừng bán"});
    VALUES.put("description", new String[] {"Description", "Mô tả"});
    VALUES.put("no_description", new String[] {"No description provided.", "Chưa có mô tả."});
    VALUES.put("quick", new String[] {"Quick", "Nhanh"});
    VALUES.put("regular", new String[] {"Regular", "Thông thường"});
    VALUES.put("minutes", new String[] {"min", "phút"});
    VALUES.put("images", new String[] {"Photo gallery", "Bộ sưu tập ảnh"});
    VALUES.put("add_image", new String[] {"Add image URL", "Thêm URL ảnh"});
    VALUES.put("cover", new String[] {"Cover", "Ảnh đại diện"});
    VALUES.put("make_cover", new String[] {"Set cover", "Chọn đại diện"});
    VALUES.put("remove", new String[] {"Remove", "Gỡ ảnh"});
    VALUES.put(
        "no_images",
        new String[] {
          "No images. Add an HTTPS image URL to get started.",
          "Chưa có ảnh. Hãy thêm URL ảnh HTTPS."
        });
    VALUES.put("url", new String[] {"Image URL (HTTPS)", "URL ảnh (HTTPS)"});
    VALUES.put("add_url", new String[] {"Add image", "Thêm ảnh"});
    VALUES.put(
        "empty", new String[] {"No dishes match your filters.", "Không có món phù hợp bộ lọc."});
    VALUES.put(
        "empty_hint",
        new String[] {"Try another name or clear the filters.", "Thử tên khác hoặc xóa bộ lọc."});
    VALUES.put("sort_name", new String[] {"Name A–Z", "Tên A–Z"});
    VALUES.put("sort_price_asc", new String[] {"Price: low to high", "Giá tăng dần"});
    VALUES.put("sort_price_desc", new String[] {"Price: high to low", "Giá giảm dần"});
    VALUES.put("sort_time", new String[] {"Preparation: fastest first", "Chế biến nhanh nhất"});
    VALUES.put(
        "name_required", new String[] {"Name must not be empty.", "Tên không được để trống."});
    VALUES.put(
        "price_error",
        new String[] {
          "Price must be a positive whole number.", "Giá phải là số nguyên lớn hơn 0."
        });
    VALUES.put(
        "time_error",
        new String[] {
          "Preparation time must be a positive whole number.",
          "Thời gian chế biến phải là số nguyên lớn hơn 0."
        });
    VALUES.put(
        "category_error",
        new String[] {"Choose an existing category.", "Hãy chọn loại món hợp lệ."});
    VALUES.put(
        "duplicate",
        new String[] {
          "This dish name already exists in the selected category.",
          "Tên món đã tồn tại trong loại món đã chọn."
        });
    VALUES.put(
        "range_error",
        new String[] {
          "Enter non-negative prices with minimum ≤ maximum.",
          "Nhập khoảng giá không âm, giá thấp nhất ≤ giá cao nhất."
        });
    VALUES.put(
        "url_error", new String[] {"Enter a valid HTTPS image URL.", "Nhập URL ảnh HTTPS hợp lệ."});
    VALUES.put(
        "duplicate_image",
        new String[] {"This image URL has already been added.", "URL ảnh này đã được thêm."});
    VALUES.put(
        "thumbnail_error",
        new String[] {
          "Only one cover image is allowed per dish.", "Mỗi món chỉ có tối đa một ảnh đại diện."
        });
    VALUES.put(
        "not_found",
        new String[] {"This item no longer exists.", "Dữ liệu này không còn tồn tại."});
    VALUES.put(
        "category_used",
        new String[] {
          "Cannot delete a category that contains dishes.",
          "Không thể xóa loại món đang được sử dụng."
        });
    VALUES.put(
        "duplicate_category",
        new String[] {"This category name already exists.", "Tên loại món đã tồn tại."});
    VALUES.put(
        "storage_error",
        new String[] {
          "Unable to save data. Please try again.", "Không thể lưu dữ liệu. Vui lòng thử lại."
        });
    VALUES.put("error", new String[] {"Please check your data", "Vui lòng kiểm tra dữ liệu"});
    VALUES.put("ok", new String[] {"OK", "Đồng ý"});
    VALUES.put("saved", new String[] {"Saved locally", "Đã lưu trên thiết bị"});
    VALUES.put("deleted", new String[] {"Deleted", "Đã xóa"});
    VALUES.put("category_name", new String[] {"Category name", "Tên loại món"});
    VALUES.put("add_category", new String[] {"Add category", "Thêm loại món"});
    VALUES.put("edit_category", new String[] {"Edit category", "Sửa loại món"});
    VALUES.put("save_category", new String[] {"Save category", "Lưu loại món"});
    VALUES.put(
        "no_categories",
        new String[] {
          "No categories yet. Add a category before creating a dish.",
          "Chưa có loại món. Hãy thêm loại món trước."
        });
    VALUES.put("manage_categories", new String[] {"Manage categories", "Quản lý loại món"});
    VALUES.put("category_description", new String[] {"Category description", "Mô tả loại món"});
    VALUES.put("local", new String[] {"SQLite · offline ready", "SQLite · sẵn sàng ngoại tuyến"});
    VALUES.put(
        "offline",
        new String[] {"Offline · changes saved on device", "Ngoại tuyến · đã lưu trên thiết bị"});
    VALUES.put("syncing", new String[] {"Automatically syncing…", "Đang tự động đồng bộ…"});
    VALUES.put("synced", new String[] {"All changes synced", "Đã đồng bộ các thay đổi"});
    VALUES.put("pending", new String[] {"Changes waiting to sync", "Có thay đổi chờ đồng bộ"});
    VALUES.put(
        "sync_error",
        new String[] {"Sync failed · automatic retry enabled", "Đồng bộ lỗi · sẽ tự động thử lại"});
    VALUES.put("sync_settings", new String[] {"Automatic synchronization", "Tự động đồng bộ"});
    VALUES.put(
        "sync_help",
        new String[] {
          "Optional advanced feature. Enter two real MockAPI resource URLs. Leave blank to keep"
              + " using SQLite locally.",
          "Phần nâng cao tùy chọn. Nhập hai URL tài nguyên MockAPI thật. Để trống để dùng SQLite"
              + " trên thiết bị."
        });
    VALUES.put(
        "category_endpoint",
        new String[] {"DishCategory endpoint (HTTPS)", "Endpoint DishCategory (HTTPS)"});
    VALUES.put("dish_endpoint", new String[] {"Dish endpoint (HTTPS)", "Endpoint Dish (HTTPS)"});
    VALUES.put("save_settings", new String[] {"Save settings", "Lưu cài đặt"});
    VALUES.put(
        "endpoint_error",
        new String[] {
          "Enter two different HTTPS MockAPI endpoints, or leave both blank.",
          "Nhập hai endpoint HTTPS MockAPI khác nhau hoặc để trống cả hai."
        });
    VALUES.put(
        "sync_policy",
        new String[] {
          "Changes save locally first. Sync runs automatically when a validated Internet connection"
              + " returns. On this device, pending local edits take priority.",
          "Thay đổi được lưu local trước. Tự đồng bộ khi có Internet trở lại. Trên thiết bị này,"
              + " thay đổi local đang chờ được ưu tiên."
        });
    VALUES.put("pending_count", new String[] {"Pending records", "Bản ghi chờ"});
    VALUES.put(
        "not_configured",
        new String[] {
          "MockAPI has not been configured. Local features work fully.",
          "Chưa cấu hình MockAPI. Các chức năng local vẫn hoạt động đầy đủ."
        });
    VALUES.put(
        "configured",
        new String[] {
          "MockAPI configured. Sync starts automatically.",
          "Đã cấu hình MockAPI. Tự động bắt đầu đồng bộ."
        });
    VALUES.put(
        "photo_hint",
        new String[] {
          "Use direct HTTPS image links. Failed images show a placeholder.",
          "Dùng liên kết ảnh HTTPS trực tiếp. Ảnh lỗi sẽ có hình thay thế."
        });
    VALUES.put("results", new String[] {"dishes", "món ăn"});
    VALUES.put("active_filters", new String[] {"Active filters", "Điều kiện đang áp dụng"});
    VALUES.put(
        "identity", new String[] {"Nguyễn Đức Khiêm · BIT240125", "Nguyễn Đức Khiêm · BIT240125"});
    VALUES.put("project", new String[] {"BIT240125_FoodManager", "BIT240125_FoodManager"});
    VALUES.put("optional", new String[] {"Optional", "Không bắt buộc"});
    VALUES.put("detail", new String[] {"Dish details", "Chi tiết món"});
    VALUES.put("language", new String[] {"Language", "Ngôn ngữ"});
    VALUES.put("photos_count", new String[] {"photos", "ảnh"});
    VALUES.put(
        "local_note",
        new String[] {
          "Your dishes are stored on this device.", "Món ăn của bạn được lưu trên thiết bị này."
        });
  }

  public static String get(String key, boolean vi) {
    String[] s = VALUES.get(key);
    return s == null ? key : s[vi ? 1 : 0];
  }
}

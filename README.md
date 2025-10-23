# BTL-OOP

Hệ thống quản lí thư viện được xây dựng bằng JavaFX với cơ sở dữ liệu SQL nhẹ tự triển khai.

## Tính năng chính

- Đăng nhập với phân quyền quản trị viên và người dùng, hỗ trợ đăng ký tài khoản mới.
- Giao diện quản trị (JavaFX) quản lí sách, người dùng, phiếu mượn/trả, yêu cầu đặt trước và xuất báo cáo CSV.
- Giao diện người dùng cho phép mượn sách, đặt trước, theo dõi hạn trả và phí phạt theo ngày.
- Hệ thống tự động đánh dấu phiếu mượn quá hạn và tính phí phạt mỗi ngày dựa trên hạn trả.
- Bảng đặt trước (ReservationsPanel) hỗ trợ quản trị viên xử lý yêu cầu và người dùng tự hủy khi không còn nhu cầu.
- Cơ sở dữ liệu dạng SQL tối giản được lưu trữ vào tệp `data/library.db` cho phép mở rộng thêm bảng/chức năng.

## Yêu cầu môi trường

- Java 21 (hoặc tương thích) đã được cài đặt sẵn.

## Cách biên dịch và chạy

```bash
# Biên dịch
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.graphics -d out $(find src/main/java -name "*.java")

# Chạy chương trình
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.graphics -cp out library.Main
```

> Thay thế biến `$PATH_TO_FX` bằng thư mục chứa thư viện JavaFX (ví dụ: `/path/to/javafx/lib`).

Tài khoản quản trị mặc định:

- Tên đăng nhập: `admin`
- Mật khẩu: `admin123`

Cơ sở dữ liệu sẽ được khởi tạo cùng một số bản ghi mẫu trong lần chạy đầu tiên.

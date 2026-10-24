# Dữ liệu và quyền

- Tên chủ sở hữu, thứ tự app, hình nền và tùy chọn lưu cục bộ trên TV/box; không có backend riêng của bản tùy biến để đồng bộ các dữ liệu này.
- Chức năng thời tiết gọi HTTPS đến `ipwho.is` để ước lượng vị trí IP mạng; dịch vụ bên ngoài thấy IP công khai của kết nối. Truy vấn Open-Meteo gửi tọa độ gần đúng làm tròn 2 chữ số thập phân, không gửi tên chủ sở hữu hoặc PIN.
- Vị trí qua IP có thể sai do nhà mạng/VPN. Thông tin thời tiết là dữ liệu mô hình; dữ liệu lưu cũ được đánh dấu. Xem điều kiện/quyền riêng tư của các dịch vụ trước khi triển khai rộng.
- Quyền INTERNET phục vụ thời tiết; MODIFY_AUDIO_SETTINGS phục vụ yêu cầu mute khi khóa. Một số quyền mạng/vị trí/WRITE_SECURE_SETTINGS có từ launcher upstream và chỉ hoạt động tùy quyền/thiết bị. Không tự cấp quyền đặc biệt bằng script cài của gói này.
- Dịch vụ khóa xin lọc phím Trợ năng, không xin đọc nội dung cây màn hình. Khi không khóa, phím đi qua bình thường. Dịch vụ phải được người dùng bật thủ công.
- ADB shell có thể ghi theme và gọi cứu hộ. Dữ liệu phát triển/ADB authorization/keystore không đưa vào repo. Tắt gỡ lỗi và thu hồi ủy quyền khi không dùng.
- Nguồn dữ liệu thời tiết: [ipwho.is](https://ipwho.is/) và [Open-Meteo](https://open-meteo.com/). Luồng ứng dụng hiện có ghi nguồn Open-Meteo/CC BY 4.0 trong hộp chi tiết thời tiết; hãy giữ phần ghi nguồn đó.

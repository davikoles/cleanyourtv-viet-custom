# Nguồn gốc và ghi nhận

## CleanYourTV

Phần launcher nền tảng được lấy từ [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), copyright 2026 Gathin, giấy phép MIT. Giữ bản giấy phép tại `licenses/MIT-CleanYourTV.txt`; font Outfit và thông báo gốc vẫn nằm trong thư mục launcher.

Đây là một bản tùy biến được tách ra từ mã nguồn launcher người dùng cung cấp trong `launcher.zip`, không phải repo gốc được fork kèm toàn bộ Git history. Không tuyên bố toàn bộ mã được viết mới, không tạo quan hệ xác nhận/hỗ trợ chính thức từ upstream. Gói ZIP nguồn không cung cấp được commit gốc chính xác của upstream; không gán một commit đoán được cho snapshot.

## Phần tùy biến hiện có

- Khóa trẻ em bằng AccessibilityService, bộ đếm, bàn phím remote, PIN theo giờ/cố định và ADB cứu hộ có kiểm soát quyền.
- Lịch âm Việt Nam, định dạng ngày/header, thời tiết và tên chủ sở hữu theo thiết bị.
- Thanh nút kính bốn icon, dock/ô app gọn, tách hàm fit gốc và hàm kích thước gọn; nền không đổi màu theo app.
- Các file chính: `ChildLock*.java`, `VietnameseLunarDate.java`, `CompactHomeText.java`, `OwnerProfile.java`, `HomeWeather*.java`; sửa `MainActivity.java`, `AppAdapter.java`, `LauncherSettingsActivity.java`, manifest và resource liên quan.

## Thay đổi chỉ để đóng gói GitHub

- Bỏ file build/cache/IDE, đường dẫn SDK cục bộ và bản sao source tạm; không đóng gói APK hoặc khóa ký.
- Bỏ hai script `launcher/build-and-install.*` vì phụ thuộc `../tools` không có trong bản tách. Thêm script build/cài độc lập ở `scripts`, không tự disable launcher gốc.
- Thay tên ví dụ của người dùng bằng tên chung `An` trong lời nhắc nhập tên; không đổi cách lưu tên.
- Sửa chú thích nút cài đặt và đầu file config cho khớp hành vi hiện tại.
- Thêm README, hướng dẫn, workflow kiểm tra và thông báo giấy phép; không đổi package/version APK/logic hoạt động của bản nguồn đã cung cấp.

## Hồ Ngọc Đức — lịch âm

`VietnameseLunarDate.java` chuyển thể thuật toán `amlich-aa98` của Hồ Ngọc Đức, dựa trên thuật toán thiên văn trong sách Jean Meeus. Giữ thông báo copyright và điều kiện cá nhân/phi thương mại trong file nguồn và `licenses/AMLICH-NOTICE.txt`.

Nguồn được nêu trong file chuyển thể:

- https://www.informatik.uni-leipzig.de/~duc/amlich/
- https://github.com/vanng822/amlich/blob/master/lib/amlich-aa98.js

## Thành phần khác

- Outfit: copyright 2021 The Outfit Project Authors, SIL OFL 1.1, xem `launcher/FONT-LICENSE-OFL.txt`. Font không được sửa.
- AndroidX RecyclerView/DynamicAnimation và JUnit được Gradle tải như dependency, không chép mã hoặc JAR của chúng vào repo nguồn này.
- Gradle wrapper JAR/script được giữ để tái lập build, giấy phép Apache 2.0 của wrapper được thông báo trong các script và tại `licenses/Apache-2.0.txt`.
- Thời tiết: ghi nguồn và liên kết ipwho.is/Open-Meteo trong ứng dụng và tài liệu; dữ liệu lấy qua API, không kèm dữ liệu cá nhân từ box.
- Không kèm ứng dụng ngoài launcher, video hoặc ảnh chụp căn nhà/TV của người dùng.

## Bản có hình nền

Theo yêu cầu mới, gói kèm nguyên 4 JPEG từ TV-Wallpapers.zip: hai ảnh cực quang, CR7 và Avengers. Thêm `BundledWallpapers.java` để đưa ảnh vào trình chọn hiện có; chọn CR7 chỉ trên lần cài mới và không ghi đè ảnh/lựa chọn cũ. Thêm kiểm tra sao chép, khôi phục mặc định 6 cột/dock/cinematic/focus glow từ cấu hình người dùng cung cấp. Không thêm APK bên thứ ba hay sao chép tên/PIN/tài khoản.

Mã nguồn của phần bổ sung mới theo MIT như các thay đổi khác, ngoại trừ ảnh giữ quyền riêng của tác giả/nguồn ảnh. Xem docs/WALLPAPERS.md và WALLPAPER-SHA256.txt. Các thay đổi này được ghi thêm sau snapshot launcher ban đầu, vì vậy các câu mô tả "chỉ đổi chú thích/tên ví dụ" ở báo cáo đóng gói ban đầu không còn mô tả toàn bộ bản có ảnh.

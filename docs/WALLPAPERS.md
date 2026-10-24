# Bốn hình nền đi kèm

Ảnh được người dùng cung cấp trong TV-Wallpapers.zip để kèm launcher. Giữ nguyên toàn bộ byte JPEG; chỉ đổi tên file sang ASCII để tương thích provider và trình chọn.

| File gốc | File trong assets/wallpapers | Nội dung | Kích thước |
|---|---|---|---|
| hinh nen 1.jpg | bundled-01-aurora.jpg | Cực quang trên hồ | 1280×720 |
| hinh nen 2.jpg | bundled-02-mountain-lake.jpg | Cực quang, hồ và núi | 1280×720 |
| hinh nen 3.jpg | bundled-03-cr7.jpg | Cristiano Ronaldo CR7 | 1280×721 |
| hinh nen 4.jpg | bundled-04-avengers.jpg | Avengers | 1280×720 |

Xem `WALLPAPER-SHA256.txt` tại gốc repo để đối chiếu ảnh gốc. Không crop, đổi màu, nén lại hoặc xóa chữ ký/watermark trong quá trình đóng gói. Launcher vẫn scale/crop khi hiển thị theo màn hình như trước.

## Cách dùng

Build/cài launcher từ project này. Khi mở Home, ảnh được chép vào thư mục theme riêng của ứng dụng để bộ chọn hiện có đọc được. Cài mới chưa có theme/ảnh/tùy chọn sẽ chọn CR7. Cài đè không ép thay lựa chọn hình nền cũ; file có sẵn cùng tên không bị ghi đè. Nếu bộ ảnh đi kèm bị xóa trên thiết bị, app có thể bổ sung lại file thiếu khi tạo Home.

Giữ OK tại bánh răng → Cài đặt launcher → Hình nền → chọn một trong bốn ảnh → Back về Home. Không cần ADB để thêm bộ ảnh này. Nút Không có hình nền vẫn dùng được.

Khi chia sẻ lên GitHub, giữ bốn JPEG trong assets. `.gitignore` có ngoại lệ riêng để Git không bỏ qua các ảnh này; ảnh tự thêm ngoài bộ sưu tập vẫn được bỏ qua.

## Quyền đối với ảnh

Các JPEG là nội dung do người dùng cung cấp, không phải toàn bộ đều là tài sản tự do theo MIT của launcher. Quyền của ảnh, nhân vật và các yếu tố nhận diện vẫn thuộc tác giả/chủ thể quyền tương ứng. Gói không khẳng định ảnh sưu tầm được cấp giấy phép MIT/CC0 hay có quyền thương mại. Không xóa watermark/credit đang có. Khi tái phân phối công khai, người xuất bản cần giữ credit và bảo đảm có quyền chia sẻ từng ảnh; nếu không có quyền phù hợp thì thay hoặc bỏ ảnh tương ứng trước khi xuất bản.

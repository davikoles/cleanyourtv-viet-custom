# Đưa project lên GitHub

Gói này đã là cây source repo, không phải file cần chép vào project cũ. Tên đề xuất là `cleanyourtv-viet-custom`; có thể đổi tên repo, nhưng giữ credit và giấy phép trong source. Chưa có thao tác đăng/push nào được thực hiện thay người dùng.

## Cách dễ dùng trên Windows: Git và GitHub Desktop

1. Cài Git for Windows và GitHub Desktop nếu chưa có. Đăng nhập GitHub Desktop bằng tài khoản GitHub của bạn.
2. Giải nén gói nguồn. Mở thư mục `cleanyourtv-viet-custom`; bên trong phải thấy `README.md`, `launcher`, `docs`, `scripts`, `LICENSE` và `.github`.
3. Mở PowerShell ngay tại thư mục đó, chạy:

   ```powershell
   git init
   git add .
   git status --short
   ```

4. Kiểm tra danh sách đã thêm. Không được có local.properties, build, .gradle, keystore, adbkey hay ảnh/video riêng. `.github/workflows/verify.yml` và Gradle wrapper JAR là file nguồn cần giữ.
5. Tạo commit:

   ```powershell
   git commit -m "Initial custom Android TV launcher source"
   git branch -M main
   ```

   Nếu Git chưa biết danh tính, cấu hình tên hiển thị và địa chỉ noreply của chính tài khoản bạn, rồi chạy lại commit. Không cần gửi email/token/mật khẩu cho người khác.

6. Trong GitHub Desktop chọn File → Add local repository, chọn thư mục vừa giải nén.
7. Chọn Publish repository. Đặt tên và mô tả. Nếu muốn mọi người xem, bỏ chọn Keep this code private. Sau khi tự kiểm tra quyền riêng tư, bấm Publish repository để đăng.
8. Mở tab Actions trên GitHub để xem job kiểm tra. CI chưa được chạy trên repo của bạn trong lúc đóng gói; nếu lỗi, xem log và sửa trước khi phát hành APK.

Không chỉ upload nguyên ZIP vào repo: GitHub cần các thư mục/file bên trong để duyệt source và chạy workflow. Không đưa thư mục nguồn cá nhân cũ lên thay cho cây đã làm sạch.

## Nếu muốn dùng Git CLI hoàn toàn

Sau commit ở trên, tạo một repository trống trên GitHub, không tạo thêm README/license/gitignore vì gói này đã có. Tại thư mục source:

```powershell
$remote = Read-Host 'HTTPS clone URL cua repository GitHub moi'
git remote add origin $remote
git push -u origin main
```

Dùng cơ chế đăng nhập/Git Credential Manager của Git, không nhúng token vào URL hoặc commit. Nếu tạo repo đã có commit khác thì không dùng force-push để vượt lỗi; kiểm tra remote và lịch sử trước.

## Khi chia sẻ bản phát hành

- Mô tả đây là bản tùy biến từ Gathin23/cleanyourtv, chưa được xác nhận trên mọi TV.
- Chỉ đưa APK vào GitHub Releases khi đã có chiến lược ký ổn định; APK tự build bằng debug key khác nhau không thể cập nhật lẫn nhau.
- Không upload khóa ký, thông tin ADB, cấu hình cá nhân, ảnh/video của gia đình hoặc media bên thứ ba không có quyền chia sẻ.
- Giữ điều kiện của thành phần lịch âm; không chọn một license template trên GitHub để thay thế LICENSE/NOTICE hiện có của toàn bộ bản ghép.

Hướng dẫn chính thức: https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github

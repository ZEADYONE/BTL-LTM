# downloads/

Thư mục chứa bản tải phục vụ qua website (`/downloads/`). Nội dung được sinh bởi:

```powershell
.\gradlew.bat :client-fx:packageZip
```

- `BombermanOnline-<version>-Windows.zip` — toàn bộ app image JavaFX (exe + `app/` + `runtime/`).
- `BombermanOnline-<version>-Windows.zip.sha256` — checksum SHA-256.
- `release.json` — version, dung lượng, checksum; website đọc file này để hiển thị.

Các file trên không commit vào git: ZIP nặng khoảng 70 MB (GitHub cảnh báo file trên
50 MB, chặn file trên 100 MB) và đổi checksum sau mỗi lần build. Container `website` mount thư mục này ở chế độ chỉ đọc,
nên chỉ cần build lại ZIP, không cần build lại image.

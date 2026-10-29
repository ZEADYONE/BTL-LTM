"""Bảng phân công nhiệm vụ (ngay sau trang bìa).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
"""
from __future__ import annotations

from report_common import (
    add_body,
    add_heading,
    add_table,
)


def write(doc):
    add_heading(doc, "BẢNG PHÂN CÔNG NHIỆM VỤ", "CTDT-H0")
    add_body(
        doc,
        "Phần chung gồm Mở đầu, Chương 1, Chương 2 và Kết luận do cả nhóm thực hiện. Nội dung cá nhân của từng thành viên được trình bày trong chương tương ứng theo bảng dưới đây.",
    )
    add_table(
        doc,
        None,
        ["STT", "Họ và tên", "Mã sinh viên", "Module phụ trách", "Nội dung lập trình mạng", "Chương"],
        [
            ["1", "", "", "Kết nối và xác thực", "Server TCP, phiên kết nối, giao thức đóng khung và mã hóa thông điệp, tầng kết nối phía Client, đăng ký, đăng nhập, trạng thái trực tuyến, xử lý mất kết nối.", "Chương 3"],
            ["2", "", "", "Sảnh và phòng chờ", "Đồng bộ danh sách người trực tuyến và danh sách phòng; tạo, tham gia, rời phòng; sẵn sàng, bắt đầu, chơi lại; phát trạng thái phòng tới các Client liên quan.", "Chương 4"],
            ["3", "", "", "Game Engine và đồng bộ thời gian thực", "Nhận thao tác di chuyển, đặt bom; vòng lặp 20 tick/giây cho từng phòng; phát trạng thái trận và kết quả; thu phím và hiển thị trận đấu ở Client.", "Chương 5"],
            ["4", "", "", "Kiến trúc Desktop Client và dữ liệu trận đấu", "Tầng ứng dụng Client: gửi lệnh, phân phối thông điệp, cập nhật giao diện; lưu kết quả trận, tính điểm; truy vấn lịch sử và bảng xếp hạng.", "Chương 6"],
        ],
        [600, 1700, 1300, 1700, 3000, 1000],
    )

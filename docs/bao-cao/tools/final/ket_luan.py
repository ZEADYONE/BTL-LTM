"""Kết luận và tài liệu tham khảo.

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
"""
from __future__ import annotations

from report_common import (
    add_body,
    add_heading,
    add_reference,
)


def write(doc):
    add_heading(doc, "KẾT LUẬN", "CTDT-H0")
    add_body(
        doc,
        "Báo cáo đã trình bày hệ thống Bomberman Online Mini từ kiến trúc chung tới cài đặt của từng module. Hệ thống đáp ứng mô hình một Server - nhiều Client qua kết nối TCP duy trì liên tục, với giao thức đóng khung bằng tiền tố độ dài và nội dung JSON. Server quản lý phiên, tài khoản, sảnh và phòng trong bộ nhớ, xử lý trận đấu theo mô hình có thẩm quyền với một vòng lặp riêng cho mỗi phòng, lưu kết quả và số liệu xếp hạng vào cơ sở dữ liệu. Client JavaFX gửi thao tác, nhận trạng thái và hiển thị mà không tự quyết định diễn biến trận.",
    )
    add_body(
        doc,
        "Việc chia module theo tính năng giúp mỗi phần có trách nhiệm rõ ràng: kết nối và xác thực cung cấp phiên TCP đã định danh, sảnh và phòng chờ đồng bộ trạng thái chung trước trận, Game Engine đồng bộ trạng thái trận theo thời gian thực, còn Desktop Client và dữ liệu trận đấu kết nối các phần đó với người dùng và lưu lại kết quả. Các điểm giao giữa module như bắt đầu trận, kết thúc trận và mất kết nối đều đi qua những giao diện lập trình xác định.",
    )
    add_body(
        doc,
        "Hệ thống vẫn còn một số giới hạn đã nêu ở mục 2.11, như chưa mã hóa đường truyền, chưa khôi phục trận sau khi Server khởi động lại và chưa hỗ trợ tiếp tục phiên sau khi mất kết nối. Hướng phát triển tiếp theo gồm bổ sung TLS cho kết nối TCP, cơ chế kết nối lại vào trận đang diễn ra, giới hạn tần suất thao tác phía Server và quản lý phiên bản lược đồ cơ sở dữ liệu.",
    )
    add_heading(doc, "TÀI LIỆU THAM KHẢO", "CTDT-H0")
    add_reference(doc, "Nhóm phát triển, Mã nguồn dự án Bomberman Online Mini, phiên bản tại thời điểm lập báo cáo, 2026.")
    add_reference(doc, "Tệp README và cấu hình xây dựng của dự án Bomberman Online Mini, 2026.")
    add_reference(doc, "Tài liệu kỹ thuật đi kèm mã nguồn Java 21, Spring Boot, JavaFX và MySQL được sử dụng trong dự án.")

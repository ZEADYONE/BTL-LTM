"""Dựng báo cáo bản final từ các module nội dung trong tools/final/.

Các module được sinh từ bản Word đã chỉnh tay bằng docx_to_python.py và có thể sửa tiếp bằng tay.

    python docs/bao-cao/tools/build_final_report.py [--output <file.docx>]

Sau khi dựng, mở bằng Word và cập nhật trường (F9) hoặc chạy export_docx_pdf.ps1 -SaveUpdatedFields
để làm mới mục lục, danh mục hình và bảng.
"""
from __future__ import annotations

import argparse
import importlib
from pathlib import Path

from docx import Document

import build_group_report
import final
import functional_diagrams
from build_group_report import add_contents_lists, finalize_document
from report_common import ROOT, TEMPLATE, clear_after_cover, configure_styles, enable_update_fields, replace_cover_text

OUTPUT = ROOT / "docs" / "bao-cao" / "bao_cao_nhom_bomberman-final-build.docx"
# Chapter 2 keeps its original diagrams; Chapters 3–6 use the functional-level figures (update.md).
DIAGRAM_MODULES = (build_group_report, functional_diagrams)


def render_diagrams():
    """Redraw every generated diagram PNG so the content modules always find their images."""
    for module in DIAGRAM_MODULES:
        for name, render in vars(module).items():
            if name.startswith("diagram_") and callable(render):
                render()


def write_modules(doc, names: list[str]):
    for name in names:
        importlib.import_module(f"final.{name}").write(doc)


def build_report(output: Path):
    render_diagrams()

    doc = Document(str(TEMPLATE))
    replace_cover_text(doc)
    clear_after_cover(doc)
    configure_styles(doc)
    enable_update_fields(doc)

    write_modules(doc, final.FRONT)
    add_contents_lists(doc)
    write_modules(doc, final.BODY)

    finalize_document(doc, output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Dựng báo cáo bản final từ tools/final/.")
    parser.add_argument("--output", type=Path, default=OUTPUT, help="Đường dẫn file .docx kết quả")
    build_report(parser.parse_args().output.resolve())

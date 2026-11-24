"""把 docs/开发文档.md 转成排版好的 .docx（python-docx）。

支持：# ~ ###### 标题、| 表格 |、``` 代码块、**粗体**、--- 分隔线、普通段落。
"""
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Pt, RGBColor, Cm

SRC = Path(sys.argv[1])
DST = Path(sys.argv[2])

doc = Document()


def set_style_font(style, latin, eastasia):
    """显式写 w:rFonts，保证中文用黑体/宋体而不是回退到 Calibri。"""
    rpr = style.element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    for attr in ("w:ascii", "w:hAnsi", "w:cs"):
        rfonts.set(qn(attr), latin)
    rfonts.set(qn("w:eastAsia"), eastasia)

# 页面与默认样式
section = doc.sections[0]
section.top_margin = Cm(2.54)
section.bottom_margin = Cm(2.54)
section.left_margin = Cm(3.0)
section.right_margin = Cm(3.0)

normal = doc.styles["Normal"]
normal.font.size = Pt(12)
set_style_font(normal, "Times New Roman", "宋体")
normal.paragraph_format.line_spacing = 1.5
normal.paragraph_format.space_after = Pt(6)

for i, size in [(1, 18), (2, 16), (3, 14), (4, 12)]:
    st = doc.styles[f"Heading {i}"]
    st.font.size = Pt(size)
    st.font.bold = True
    st.font.color.rgb = RGBColor(0x1F, 0x3A, 0x2E)
    set_style_font(st, "Times New Roman", "黑体")
    st.paragraph_format.space_before = Pt(12)
    st.paragraph_format.space_after = Pt(6)


def add_runs(par, text):
    """把 **粗体** 与 `代码` 拆成不同 run。"""
    tokens = re.split(r"(\*\*.+?\*\*|`.+?`)", text)
    for tok in tokens:
        if not tok:
            continue
        if tok.startswith("**") and tok.endswith("**"):
            run = par.add_run(tok[2:-2])
            run.bold = True
        elif tok.startswith("`") and tok.endswith("`"):
            run = par.add_run(tok[1:-1])
            run.font.name = "Consolas"
            run.font.size = Pt(10.5)
        else:
            par.add_run(tok)


lines = SRC.read_text(encoding="utf-8").splitlines()
i = 0
while i < len(lines):
    line = lines[i].rstrip()

    # 代码块
    if line.startswith("```"):
        i += 1
        block = []
        while i < len(lines) and not lines[i].startswith("```"):
            block.append(lines[i])
            i += 1
        i += 1
        par = doc.add_paragraph()
        par.paragraph_format.line_spacing = 1.15
        par.paragraph_format.left_indent = Cm(0.6)
        run = par.add_run("\n".join(block))
        run.font.name = "Consolas"
        run.font.size = Pt(9.5)
        continue

    # 表格
    if line.startswith("|"):
        rows = []
        while i < len(lines) and lines[i].strip().startswith("|"):
            rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
            i += 1
        rows = [r for r in rows if not all(re.fullmatch(r":?-{2,}:?", c) for c in r)]
        if rows:
            table = doc.add_table(rows=len(rows), cols=len(rows[0]))
            table.style = "Table Grid"
            for r, cells in enumerate(rows):
                for c, text in enumerate(cells[: len(rows[0])]):
                    cell = table.cell(r, c)
                    cell.text = ""
                    p = cell.paragraphs[0]
                    p.paragraph_format.line_spacing = 1.0
                    add_runs(p, text.replace("`", ""))
                    for run in p.runs:
                        run.font.size = Pt(10.5)
                        if r == 0:
                            run.bold = True
            doc.add_paragraph()
        continue

    # 标题
    m = re.match(r"^(#{1,4})\s+(.*)$", line)
    if m:
        level = len(m.group(1))
        text = m.group(2).strip()
        doc.add_heading(text, level=level if level <= 4 else 4)
        i += 1
        continue

    # 分隔线
    if re.fullmatch(r"-{3,}", line.strip()):
        i += 1
        continue

    # 空行
    if not line.strip():
        i += 1
        continue

    # 列表项
    m = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", line)
    if m:
        indent = len(m.group(1)) // 2
        par = doc.add_paragraph(style="List Bullet" if m.group(2) in "-*" else "List Number")
        par.paragraph_format.left_indent = Cm(0.74 + 0.74 * indent)
        par.paragraph_format.line_spacing = 1.5
        add_runs(par, m.group(3))
        i += 1
        continue

    # 引用
    if line.lstrip().startswith(">"):
        par = doc.add_paragraph()
        par.paragraph_format.left_indent = Cm(0.6)
        add_runs(par, line.lstrip()[1:].strip())
        for run in par.runs:
            run.font.size = Pt(10.5)
        i += 1
        continue

    par = doc.add_paragraph()
    par.paragraph_format.first_line_indent = Cm(0.74)
    add_runs(par, line.strip())
    i += 1

doc.save(DST)
print(f"saved: {DST}")

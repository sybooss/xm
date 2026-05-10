from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from xml.sax.saxutils import escape
from zipfile import ZIP_DEFLATED, ZipFile


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "personal-defense-slides.pptx"
DOC_TIMESTAMP = "2026-05-05T00:00:00Z"

SLIDE_W = 12192000
SLIDE_H = 6858000

NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main"
NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main"
NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"


@dataclass(frozen=True)
class Bullet:
    title: str
    detail: str


@dataclass(frozen=True)
class Slide:
    title: str
    subtitle: str = ""
    bullets: tuple[Bullet | str, ...] = ()
    footer: str = ""
    kind: str = "standard"


def emu(inches: float) -> int:
    return int(inches * 914400)


def c(text: str) -> str:
    return escape(text, {"\n": " "})


def tx(text: str, size: int = 2400, color: str = "111827", bold: bool = False) -> str:
    b = ' b="1"' if bold else ""
    return (
        f'<a:r><a:rPr lang="zh-CN" sz="{size}"{b} dirty="0">'
        f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'
        f'<a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:rPr>'
        f"<a:t>{c(text)}</a:t></a:r>"
    )


def para(text: str, size: int = 2400, color: str = "111827", bold: bool = False, bullet: bool = False) -> str:
    bu = '<a:buChar char="•"/>' if bullet else "<a:buNone/>"
    mar = ' marL="342900" indent="-171450"' if bullet else ""
    return f"<a:p><a:pPr{mar}>{bu}</a:pPr>{tx(text, size=size, color=color, bold=bold)}</a:p>"


def text_box(
    sid: int,
    x: int,
    y: int,
    w: int,
    h: int,
    paragraphs: list[str],
    fill: str | None = None,
    line: str | None = None,
) -> str:
    fill_xml = (
        f'<a:solidFill><a:srgbClr val="{fill}"/></a:solidFill>' if fill else "<a:noFill/>"
    )
    line_xml = (
        f'<a:ln w="9525"><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln>'
        if line
        else "<a:ln><a:noFill/></a:ln>"
    )
    return f"""
      <p:sp>
        <p:nvSpPr><p:cNvPr id="{sid}" name="Text {sid}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr>
          <a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm>
          <a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom>
          {fill_xml}{line_xml}
        </p:spPr>
        <p:txBody>
          <a:bodyPr wrap="square" lIns="91440" tIns="68580" rIns="91440" bIns="68580"><a:spAutoFit/></a:bodyPr>
          <a:lstStyle/>
          {''.join(paragraphs)}
        </p:txBody>
      </p:sp>
    """


def rect(sid: int, x: int, y: int, w: int, h: int, fill: str, alpha: int = 100000, line: str | None = None) -> str:
    ln = (
        f'<a:ln w="9525"><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln>'
        if line
        else "<a:ln><a:noFill/></a:ln>"
    )
    return f"""
      <p:sp>
        <p:nvSpPr><p:cNvPr id="{sid}" name="Shape {sid}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr>
          <a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm>
          <a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom>
          <a:solidFill><a:srgbClr val="{fill}"><a:alpha val="{alpha}"/></a:srgbClr></a:solidFill>
          {ln}
        </p:spPr>
      </p:sp>
    """


def slide_xml(slide: Slide, idx: int) -> str:
    parts: list[str] = []
    sid = 2
    parts.append(rect(sid, 0, 0, SLIDE_W, SLIDE_H, "F6F8FB"))
    sid += 1
    parts.append(rect(sid, emu(0.35), emu(0.34), emu(12.65), emu(0.04), "2563EB", 18000))
    sid += 1

    if slide.kind == "cover":
        parts.append(text_box(sid, emu(0.78), emu(0.78), emu(1.9), emu(0.46), [para("个人答辩", 1450, "2563EB", True)], fill="EFF6FF", line="DBEAFE"))
        sid += 1
        parts.append(text_box(sid, emu(0.72), emu(1.55), emu(7.4), emu(1.5), [para("电商退换货 AI 客服系统", 4200, "0F172A", True)], fill=None))
        sid += 1
        parts.append(text_box(sid, emu(0.78), emu(3.08), emu(7.3), emu(0.95), [para("完整售后链路 + RAG 知识依据 + LangChain4j 增强 + 工单闭环 + 日志追踪", 2050, "4B5563")], fill=None))
        sid += 1
        parts.append(text_box(sid, emu(0.78), emu(5.85), emu(5.8), emu(0.45), [para("Spring Boot + Vue 3 + MySQL + LangChain4j", 1500, "64748B")], fill=None))
        sid += 1
        labels = [("AI 增强", "2563EB"), ("本地兜底", "059669"), ("可解释日志", "7C3AED"), ("权限隔离", "D97706")]
        for i, (label, color) in enumerate(labels):
            parts.append(text_box(sid, emu(8.45), emu(1.1 + i * 1.02), emu(3.2), emu(0.62), [para(label, 2100, color, True)], fill="FFFFFF", line="E5E7EB"))
            sid += 1
    else:
        parts.append(text_box(sid, emu(0.6), emu(0.56), emu(8.9), emu(0.72), [para(slide.title, 3000, "0F172A", True)], fill=None))
        sid += 1
        if slide.subtitle:
            parts.append(text_box(sid, emu(0.62), emu(1.24), emu(9.3), emu(0.54), [para(slide.subtitle, 1500, "64748B")], fill=None))
            sid += 1
        y = 2.03
        if slide.kind == "pipeline":
            for i, item in enumerate(slide.bullets):
                label = item if isinstance(item, str) else item.title
                detail = "" if isinstance(item, str) else item.detail
                parts.append(text_box(sid, emu(0.72 + i * 2.72), emu(y), emu(2.42), emu(2.75), [
                    para(str(i + 1).zfill(2), 1700, "2563EB", True),
                    para(label, 1680, "111827", True),
                    para(detail, 1180, "64748B")
                ], fill="FFFFFF", line="E5E7EB"))
                sid += 1
        else:
            for row, item in enumerate(slide.bullets):
                if isinstance(item, Bullet):
                    paragraphs = [para(item.title, 1850, "111827", True), para(item.detail, 1320, "64748B")]
                else:
                    paragraphs = [para(item, 1650, "374151", bullet=True)]
                col = row % 2
                r = row // 2
                parts.append(text_box(sid, emu(0.72 + col * 6.18), emu(y + r * 1.35), emu(5.65), emu(1.02), paragraphs, fill="FFFFFF", line="E5E7EB"))
                sid += 1

    footer = slide.footer or "D:\\复制软件系统 | 《复杂软件系统实践》个人答辩"
    parts.append(text_box(sid, emu(0.72), emu(6.86), emu(8.8), emu(0.28), [para(footer, 1050, "94A3B8")], fill=None))
    sid += 1
    parts.append(text_box(sid, emu(11.72), emu(6.78), emu(0.8), emu(0.34), [para(str(idx), 1200, "94A3B8", True)], fill=None))

    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="{NS_A}" xmlns:r="{NS_R}" xmlns:p="{NS_P}">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      {''.join(parts)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""


def content_types(slide_count: int) -> str:
    overrides = "\n".join(
        f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        for i in range(1, slide_count + 1)
    )
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  {overrides}
</Types>"""


def root_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""


def presentation_xml(slide_count: int) -> str:
    sld_ids = "\n".join(f'<p:sldId id="{255 + i}" r:id="rId{i}"/>' for i in range(1, slide_count + 1))
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="{NS_A}" xmlns:r="{NS_R}" xmlns:p="{NS_P}" saveSubsetFonts="1">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId{slide_count + 1}"/></p:sldMasterIdLst>
  <p:sldIdLst>{sld_ids}</p:sldIdLst>
  <p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
  <p:defaultTextStyle/>
</p:presentation>"""


def presentation_rels(slide_count: int) -> str:
    rels = [f'<Relationship Id="rId{i}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>' for i in range(1, slide_count + 1)]
    rels.append(f'<Relationship Id="rId{slide_count + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>')
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  {''.join(rels)}
</Relationships>"""


def master_xml() -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="{NS_A}" xmlns:r="{NS_R}" xmlns:p="{NS_P}">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>"""


def master_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""


def layout_xml() -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="{NS_A}" xmlns:r="{NS_R}" xmlns:p="{NS_P}" type="blank" preserve="1">
  <p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>"""


def layout_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>"""


def slide_rels() -> str:
    return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>"""


def theme_xml() -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="{NS_A}" name="Course Defense">
  <a:themeElements>
    <a:clrScheme name="Course">
      <a:dk1><a:srgbClr val="111827"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="334155"/></a:dk2><a:lt2><a:srgbClr val="F6F8FB"/></a:lt2>
      <a:accent1><a:srgbClr val="2563EB"/></a:accent1><a:accent2><a:srgbClr val="059669"/></a:accent2>
      <a:accent3><a:srgbClr val="D97706"/></a:accent3><a:accent4><a:srgbClr val="7C3AED"/></a:accent4>
      <a:accent5><a:srgbClr val="0284C7"/></a:accent5><a:accent6><a:srgbClr val="DC2626"/></a:accent6>
      <a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="7C3AED"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Course"><a:majorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:majorFont><a:minorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:minorFont></a:fontScheme>
    <a:fmtScheme name="Course"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme>
  </a:themeElements>
  <a:objectDefaults/><a:extraClrSchemeLst/>
</a:theme>"""


def app_xml(slide_count: int) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application><PresentationFormat>Widescreen</PresentationFormat><Slides>{slide_count}</Slides><Company>人工智能学院</Company>
</Properties>"""


def core_xml() -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>电商退换货智能客服系统个人答辩</dc:title><dc:creator>Codex</dc:creator><cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{DOC_TIMESTAMP}</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">{DOC_TIMESTAMP}</dcterms:modified>
</cp:coreProperties>"""


def slides() -> list[Slide]:
    return [
        Slide("电商退换货 AI 客服系统", kind="cover"),
        Slide("我负责讲清楚的一条主线", "完整售后系统，不是单纯套壳聊天框", (
            Bullet("业务闭环", "登录注册、订单售后、知识库、聊天、工单、日志全部打通"),
            Bullet("AI 增强", "LangChain4j 负责增强表达和工具编排，业务规则仍由后端控制"),
            Bullet("稳定演示", "模型失败时使用本地规则兜底，核心售后流程不中断"),
            Bullet("证据可查", "处理轨迹、检索日志、AI 日志和浏览器烟测共同支撑"),
        )),
        Slide("核心链路：用户问题如何变成可信回复", "每一步都有代码、数据或日志对应", (
            Bullet("消息进入", "POST /chat-sessions/{id}/message-stream 保存用户消息并推送进度"),
            Bullet("业务理解", "解析上下文、识别退货/退款/物流/投诉等售后意图"),
            Bullet("依据生成", "读取订单状态，检索 knowledge_doc，形成本地业务判断"),
            Bullet("回复与沉淀", "AI 增强或本地兜底，必要时创建工单并记录日志"),
        )),
        Slide("处理流程可以现场展开", "右侧处理洞察和日志中心让 AI 链路可解释", (
            Bullet("上下文承接", "判断是否为追问并继承上轮意图"),
            Bullet("意图识别", "识别退货、换货、退款进度、物流异常、投诉转接"),
            Bullet("知识检索", "展示命中文档和检索原因"),
            Bullet("生成与兜底", "记录模型状态、耗时、失败或兜底结果"),
        ), kind="pipeline"),
        Slide("RAG 知识依据：回答不是凭空生成", "老师质疑时可以打开知识库和日志中心验证", (
            Bullet("知识来源", "退货规则、退款时效、物流异常、投诉转人工等文档保存在 knowledge_doc"),
            Bullet("命中展示", "咨询工作台右侧展示命中文档标题和规则摘要"),
            Bullet("日志留痕", "retrieval_log 保存查询词、命中文档、排序、分数和原因"),
            Bullet("答辩话术", "AI 只是组织语言，业务依据来自可维护知识库"),
        )),
        Slide("LangChain4j 与本地兜底的取舍", "AI 提升体验，但不能替代业务规则", (
            Bullet("增强层", "AiServiceImpl 组织 prompt 并调用 OpenAI-compatible 模型"),
            Bullet("工具结果", "AiBusinessToolService 封装订单查询、知识检索和工单创建结果"),
            Bullet("业务优先", "订单是否可退、是否已有售后、是否转人工由 Spring Boot 判断"),
            Bullet("兜底策略", "无 key、模型失败或关闭 AI 时返回本地规则模板并记录状态"),
        )),
        Slide("智能工单：从问答走向真实客服协同", "投诉、人工客服、物流异常不是只回复一句话", (
            Bullet("触发条件", "投诉、平台介入、商家不处理、人工客服等诉求触发工单"),
            Bullet("工单内容", "保存会话、订单、用户问题、AI 摘要、处理建议和优先级"),
            Bullet("页面演示", "/service-tickets 可筛选、查看详情和流转状态"),
            Bullet("高分价值", "体现复杂软件系统的业务闭环，而不只是 API 调用"),
        )),
        Slide("前端展示：把亮点变成老师能看到的证据", "新增答辩展示中心，先建立整体印象再进入工作台", (
            Bullet("展示中心", "/showcase 汇总演示顺序、8 个亮点、系统状态和关键入口"),
            Bullet("咨询工作台", "消息区、订单上下文、知识命中、处理轨迹同屏展示"),
            Bullet("角色隔离", "客户看不到也访问不了后台和展示中心"),
            Bullet("日志诊断", "/logs 聚合 AI 成功率、平均耗时、知识命中和会话步骤"),
        )),
        Slide("日志诊断中心：把可解释性变成一屏证据", "答辩收尾时用数据证明系统不是黑盒", (
            Bullet("AI 稳定性", "最近调用按 SUCCESS、SKIPPED、FAILED 统计，并展示成功率"),
            Bullet("响应效率", "平均耗时只统计有真实耗时记录的模型调用"),
            Bullet("知识命中", "展示检索日志数量、去重命中文档、平均检索分数和高频文档"),
            Bullet("流程轨迹", "输入会话 ID 后展开上下文解析、意图识别、检索、AI 生成和最终回复"),
        )),
        Slide("验证证据：本项目不靠口头承诺", "最近一轮验证已经覆盖代码、接口和浏览器", (
            Bullet("后端", "mvn.cmd -q -DskipTests package 通过"),
            Bullet("前端", "npm.cmd run build 通过，仅有 Vite chunk size 提示"),
            Bullet("全链路", "tools/full-smoke-test.ps1：FAILED_COUNT=0"),
            Bullet("浏览器", "test:browser 与 test:browser:roles 均 FAILED_COUNT=0"),
        )),
        Slide("如果老师继续追问", "把问题落回业务可靠性、可解释性和可扩展性", (
            Bullet("为什么不是普通聊天机器人？", "因为它先做业务编排，再做 AI 增强，并能生成工单和日志"),
            Bullet("AI 不可用怎么办？", "本地规则兜底，聊天主链路不中断"),
            Bullet("后续如何扩展？", "向量检索、细粒度 RBAC、统计看板、售后状态工作流"),
            Bullet("个人贡献怎么概括？", "核心链路、AI/RAG/工单/日志、展示入口和验证材料"),
        )),
    ]


def write_pptx() -> None:
    deck = slides()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(OUT, "w", ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types(len(deck)))
        z.writestr("_rels/.rels", root_rels())
        z.writestr("docProps/app.xml", app_xml(len(deck)))
        z.writestr("docProps/core.xml", core_xml())
        z.writestr("ppt/presentation.xml", presentation_xml(len(deck)))
        z.writestr("ppt/_rels/presentation.xml.rels", presentation_rels(len(deck)))
        z.writestr("ppt/slideMasters/slideMaster1.xml", master_xml())
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", master_rels())
        z.writestr("ppt/slideLayouts/slideLayout1.xml", layout_xml())
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", layout_rels())
        z.writestr("ppt/theme/theme1.xml", theme_xml())
        for i, slide in enumerate(deck, 1):
            z.writestr(f"ppt/slides/slide{i}.xml", slide_xml(slide, i))
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", slide_rels())
    print(OUT)


if __name__ == "__main__":
    write_pptx()

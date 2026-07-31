#!/usr/bin/env python3
"""
为 3 道示例论述题填充 angle + notes + related_point_ids JSON 字段，
并 bump seed 版本号 2.13.1 → 2.14.0 触发重新导入。

对应 docs/design/essay-module-design.md 3.5/3.6 节。
仅修改 3 道题：eq_0038（五位女作家）/ eq_0182（农民题材）/ eq_0254（知识分子形象）。
"""
import json
import copy
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0038: 2008 604卷 现当代·五位女作家异同（比较型）─────────────
EQ_0038_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["冰心", "丁玲", "萧红", "张爱玲", "王安忆", "女作家", "异同"],
    "limitKeywords": ["创作", "不同时期"],
    "task": "比较异同 + 梳理演变",
    "breakthroughAngles": [
        "①时代背景（五四→左翼→抗战→沦陷→新时期）",
        "②女性意识（启蒙→革命→悲剧→世俗→都市）",
        "③代表作品与艺术风格",
        "④文学史地位",
    ],
    "angleRationale": "从'时代—意识—作品—地位'四维度纵向梳理演变，横向比较异同，符合比较型题'同中求异、异中求同'原则",
    "argumentPath": {
        "thesis": "五位女作家共同关注女性命运，但随时代变迁，女性意识从启蒙走向革命、从悲剧走向世俗、从乡村走向都市，呈现中国现当代女性写作的演变轨迹",
        "points": [
            {"label": "总述（同）", "content": "共同点：都是女性视角，关注女性命运与生存困境，语言各有诗意"},
            {"label": "分1·冰心", "content": "五四时期——'爱的哲学'，母爱/童心/自然，《繁星·春水》哲理小诗，女性意识处于启蒙阶段"},
            {"label": "分2·丁玲", "content": "左翼时期——从莎菲女士的个人苦闷到《太阳照在桑干河上》的革命女性，女性意识转向革命"},
            {"label": "分3·萧红", "content": "抗战时期——底层女性悲剧，《生死场》《呼兰河传》散文化叙事，女性意识与民族苦难交织"},
            {"label": "分4·张爱玲", "content": "沦陷时期——苍凉美学，世俗女性婚恋困境，《倾城之恋》《金锁记》，女性意识走向世俗审视"},
            {"label": "分5·王安忆", "content": "新时期——都市女性命运，《长恨歌》写上海弄堂女性，女性意识回归日常与都市"},
            {"label": "总结（异+演变）", "content": "差异背后是时代变迁；演变轨迹：启蒙→革命→悲剧→世俗→都市，构成现当代女性写作完整谱系"},
        ],
        "conclusion": "五位女作家的创作异同，折射出中国现当代女性文学从五四启蒙到当代都市书写的完整历程",
    },
}

EQ_0038_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "冰心《繁星》：「繁星闪烁着——深蓝的太空，何曾听得见他们对语」",
            "source": "冰心《繁星·春水》人民文学出版社",
            "linkedKnowledgePointId": "kp_00595",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "丁玲《莎菲女士的日记》：「我了解我自己，我是一个女人，我要被人爱，我也要爱人」",
            "source": "丁玲《莎菲女士的日记》1928年《小说月报》",
            "linkedKnowledgePointId": "kp_00634",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "张爱玲《倾城之恋》：「香港的陷落成全了她」",
            "source": "张爱玲《倾城之恋》1943年《杂志》月刊",
            "linkedKnowledgePointId": "kp_00625",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王安忆《长恨歌》：「上海弄堂里的感动，是流言的感动」",
            "source": "王安忆《长恨歌》作家出版社1995年",
            "linkedKnowledgePointId": "kp_00686",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群等指出：丁玲从《莎菲女士的日记》到《太阳照在桑干河上》，体现了左翼文学中女性作家从个人主义到集体主义的转型",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": "kp_00634",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "夏志清认为张爱玲的苍凉美学是对五四浪漫主义的反拨，'在传奇里寻找普通人，在普通人里寻找传奇'",
            "source": "夏志清《中国现代小说史》香港友联出版社1979年版",
            "linkedKnowledgePointId": "kp_00625",
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将王安忆《长恨歌》定位为90年代都市文学的代表作，'弄堂里的女性史诗'",
            "source": "丁帆《中国新文学史》下册 高等教育出版社2013年版",
            "linkedKnowledgePointId": "kp_00686",
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重五位作家在现代文学史（1917-1949）中的定位；丁帆《新文学史》打通现当代，将王安忆纳入谱系。两书对冰心/丁玲/萧红/张爱玲的定位基本一致，差异在王安忆——钱理群未覆盖（成书早），丁帆重点论述。",
        "scholarComparison": "孟悦/戴锦华《浮出历史地表》从女性主义视角系统梳理五位作家，强调'女性写作的自觉'；钱理群从文学史主流视角定位。两种视角互补：前者重性别意识，后者重文学史贡献。",
    },
    "referenceLinks": [
        {"label": "中国作家网·茹志鹃：历史褶皱里的文学烛照（女作家群研究）", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·女性写作的百年流变", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "萧红", "note": "项目暂无萧红独立知识点，建议补充'萧红《生死场》《呼兰河传》与散文化叙事'知识点以完善女作家谱系"},
    ],
}

EQ_0038_RELATED = ["kp_00595", "kp_00634", "kp_00625", "kp_00626", "kp_00686"]

# ── eq_0182: 2014 615卷 农民题材+国民性（综合型）─────────────
EQ_0182_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["农民题材", "国民性", "批判", "创作特征"],
    "limitKeywords": ["现当代", "小说"],
    "task": "梳理演变 + 归纳特征 + 评价意义",
    "breakthroughAngles": [
        "①时代脉络（五四→解放区→十七年→新时期→新世纪）",
        "②国民性批判维度（奴性/麻木/狡黠/觉醒/反思）",
        "③代表性作家作品（鲁迅/赵树理/柳青/高晓声/陈忠实）",
        "④创作特征（典型化/细节真实/方言运用/历史纵深）",
    ],
    "angleRationale": "本题为综合型（流变+评价），需纵向梳理百年农民题材小说演变，横向归纳国民性批判维度，最后评价创作特征。符合'流变型拉时间轴+评价型先表态'双重策略。",
    "argumentPath": {
        "thesis": "现当代农民题材小说以国民性批判为核心主题，从鲁迅的'哀其不幸、怒其不争'到陈忠实的文化反思，形成了贯穿百年的批判传统，在典型化、细节真实、历史纵深等方面呈现出鲜明的创作特征",
        "points": [
            {"label": "总述", "content": "国民性批判是现当代农民题材小说的核心主题，鲁迅奠基，后人沿袭与深化"},
            {"label": "分1·五四启蒙（鲁迅）", "content": "鲁迅《阿Q正传》塑造阿Q形象，批判精神胜利法、奴性、麻木，'哀其不幸、怒其不争'，奠定国民性批判范式"},
            {"label": "分2·解放区（赵树理）", "content": "赵树理《小二黑结婚》《锻炼锻炼》写解放区农民新变化，但保留对落后面的善意讽刺，批判与歌颂并存"},
            {"label": "分3·十七年（柳青）", "content": "柳青《创业史》写合作化中的农民，梁三老汉代表保守的小生产者心理，梁生宝代表新农民的觉醒，批判让位于歌颂"},
            {"label": "分4·新时期（高晓声）", "content": "高晓声《陈奂生上城》写改革开放后的农民，陈奂生的'漏斗户主'身份与进城后的阿Q式精神胜利，回归国民性批判传统"},
            {"label": "分5·创作特征归纳", "content": "特征：①典型化塑造（阿Q/陈奂生成为国民性符号）②细节真实（方言/习俗/心理）③历史纵深（百年演变反映中国社会转型）④批判与同情交织"},
            {"label": "总结", "content": "百年农民题材小说的国民性批判，从启蒙到反思，构成中国现当代文学最重要的精神传统之一"},
        ],
        "conclusion": "农民题材小说的国民性批判传统，既是中国现代性焦虑的文学投射，也是中国作家对民族精神结构的持续叩问",
    },
}

EQ_0182_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《阿Q正传》：「然而阿Q虽然常优胜，却直待蒙赵太爷打他嘴巴之后，这才出了名」",
            "source": "鲁迅《阿Q正传》1921年《晨报副刊》",
            "linkedKnowledgePointId": "kp_00615",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "赵树理《锻炼锻炼》：'小腿疼'和'吃不饱'两个落后妇女形象的讽刺描写",
            "source": "赵树理《锻炼锻炼》1958年《火花》月刊",
            "linkedKnowledgePointId": "kp_00641",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "柳青《创业史》：「梁三老汉恨不得用尽气力，把每一颗粮食都收进自己的仓里」",
            "source": "柳青《创业史》中国青年出版社1960年版",
            "linkedKnowledgePointId": "kp_00654",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "高晓声《陈奂生上城》：「陈奂生想到花了五块钱住一夜，就决心去糟蹋它一下」",
            "source": "高晓声《陈奂生上城》1980年《人民文学》",
            "linkedKnowledgePointId": "kp_00681",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：鲁迅的国民性批判继承了晚清启蒙思潮，'哀其不幸、怒其不争'奠定了现代文学农民书写的基调",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和认为：高晓声的陈奂生系列是'阿Q精神在新时代的延续'，国民性批判在新时期文学中复活",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": "kp_00681",
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将农民题材小说列为现当代文学三大题材之一，国民性批判是其核心精神谱系",
            "source": "丁帆《中国新文学史》高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》重点论述鲁迅—赵树理脉络；丁帆《新文学史》打通现当代，将高晓声/陈忠实纳入国民性批判谱系；洪子诚《当代文学史》侧重十七年农民题材的政治化书写。三书共识：国民性批判是贯穿现当代农民题材小说的核心主题。",
        "scholarComparison": "陈思和《当代文学史教程》强调'民间立场'与'启蒙立场'的张力；丁帆强调'文化反思'维度。差异：陈思和重民间审美，丁帆重批判精神。",
    },
    "referenceLinks": [
        {"label": "中国作家网·农民形象与国民性批判百年流变", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·乡村振兴与新时代农民书写", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陈忠实", "note": "项目暂无陈忠实《白鹿原》独立知识点，建议补充以完善农民题材小说谱系（白嘉轩代表传统农民的宗法文化）"},
        {"author": "莫言", "note": "项目暂无莫言独立知识点，建议补充'莫言《红高粱》与乡土叙事'以覆盖新时期农民题材的先锋写作"},
    ],
}

EQ_0182_RELATED = ["kp_00613", "kp_00615", "kp_00641", "kp_00654", "kp_00681"]

# ── eq_0254: 2015 615卷 知识分子形象演变（流变型）─────────────
EQ_0254_ANGLE = {
    "questionType": "流变型",
    "coreKeywords": ["知识分子形象", "类型", "演变"],
    "limitKeywords": ["现当代", "结合作品"],
    "task": "梳理演变 + 归纳类型",
    "breakthroughAngles": [
        "①时代脉络（五四→左翼→抗战→十七年→新时期→90年代）",
        "②形象类型（启蒙者/革命者/改造者/受难者/反思者）",
        "③代表性作家作品（鲁迅/郁达夫/杨沫/王蒙/张贤亮/杨绛）",
        "④演变逻辑（启蒙→革命→改造→受难→反思）",
    ],
    "angleRationale": "流变型题以时间纵轴为主干，每个时期=一个分论点，突出'承前启后'的因果链。同时归纳类型，符合综合型题双重策略。",
    "argumentPath": {
        "thesis": "现当代文学中知识分子形象经历了从启蒙者到革命者、从改造者到受难者、再到反思者的演变，折射出中国知识分子在20世纪历史中的命运轨迹与精神变迁",
        "points": [
            {"label": "总述", "content": "知识分子形象是现当代文学的镜像，其演变反映中国知识分子的精神史"},
            {"label": "分1·五四启蒙者（鲁迅/郁达夫）", "content": "鲁迅笔下的魏连殳、N先生是孤独的启蒙者；郁达夫《沉沦》写'零余者'的苦闷，知识分子形象带有启蒙主义的悲剧色彩"},
            {"label": "分2·左翼革命者（丁玲/巴金）", "content": "丁玲《水》转向革命写作；巴金《家》中觉慧是叛逆的革命青年，知识分子从启蒙走向革命"},
            {"label": "分3·十七年改造者（杨沫）", "content": "杨沫《青春之歌》林道静从个人主义小资产阶级到革命知识分子的成长，反映知识分子'改造'主题"},
            {"label": "分4·新时期受难者（王蒙/张贤亮）", "content": "王蒙《组织部新来的青年人》写理想主义遭遇官僚体制；张贤亮《绿化树》写反右中知识分子的饥饿与苦难，知识分子形象回归批判"},
            {"label": "分5·90年代反思者（巴金/杨绛）", "content": "巴金《随想录》知识分子的自我反省；杨绛《干校六记》《洗澡》以反讽笔法写知识分子苦难，从受难走向反思"},
            {"label": "总结", "content": "知识分子形象的演变轨迹：启蒙→革命→改造→受难→反思，构成20世纪中国知识分子精神史"},
        ],
        "conclusion": "知识分子形象的百年演变，是中国现代性追求与挫折的文学见证，也是知识分子自我认知不断深化的过程",
    },
}

EQ_0254_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《在酒楼上》：「我在少年时，看见蜂子或蝇子停在一个地方，给什么来一吓，即刻飞去了，但是飞了一个小圈子，便又回来停在原地点」",
            "source": "鲁迅《在酒楼上》1924年《小说月报》",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "郁达夫《沉沦》：「祖国呀祖国！我的死是你害我的！你快富起来，强起来罢！」",
            "source": "郁达夫《沉沦》1921年泰东书局",
            "linkedKnowledgePointId": "kp_00602",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王蒙《组织部新来的青年人》：「林震渴望斗争，渴望生活里充满阳光」",
            "source": "王蒙《组织部新来的青年人》1956年《人民文学》",
            "linkedKnowledgePointId": "kp_00674",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "杨绛《干校六记》：「我们常把'送行'说成'送终'，本是开玩笑，谁知竟成了真」",
            "source": "杨绛《干校六记》1981年《收获》",
            "linkedKnowledgePointId": "kp_00695",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：20世纪中国文学中知识分子形象经历了'启蒙—革命—改造—受难—反思'五阶段，每一阶段都对应着知识分子的精神危机与重构",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "许子东认为：杨绛《干校六记》的'哀而不伤'笔法，是知识分子苦难叙事的独特审美选择，区别于张贤亮的'苦难崇高化'",
            "source": "许子东《当代小说阅读笔记》华东师范大学出版社2007年版",
            "linkedKnowledgePointId": "kp_00695",
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将知识分子形象列为现当代文学三大人物长廊之一（农民/知识分子/女性），其演变反映中国社会现代化进程",
            "source": "丁帆《中国新文学史》高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重现代阶段（五四—1949）知识分子启蒙者形象；洪子诚《当代文学史》侧重十七年与新时期改造者/受难者形象；丁帆《新文学史》打通现当代，构建完整演变脉络。三书共识：知识分子形象演变与时代政治密切相关。",
        "scholarComparison": "陈思和以'民间立场'解读知识分子受难叙事；许子东以'叙事学'分析笔法差异；王德威以'史诗叙事'观照90年代反思写作。三种视角互补：陈思和重精神史，许子东重叙事学，王德威重文学史定位。",
    },
    "referenceLinks": [
        {"label": "中国作家网·知识分子形象百年流变", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·90年代反思文学与知识分子写作", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "钱钟书《围城》", "note": "项目暂无钱钟书《围城》独立知识点，建议补充'钱钟书《围城》与方鸿渐形象'以覆盖40年代知识分子形象（方鸿渐代表'围城'中挣扎的近代知识分子）"},
        {"author": "路遥《平凡的世界》", "note": "项目暂无路遥独立知识点，建议补充以覆盖80年代农村知识青年的精神成长（孙少平代表城乡之间的知识青年）"},
    ],
}

EQ_0254_RELATED = ["kp_00613", "kp_00602", "kp_00674", "kp_00694", "kp_00695"]


def main():
    with SEED_PATH.open("r", encoding="utf-8") as f:
        data = json.load(f)

    # bump version
    old_version = data["metadata"]["version"]
    if old_version != "2.13.1":
        print(f"WARNING: expected version 2.13.1, got {old_version}")
    data["metadata"]["version"] = "2.14.0"
    # append change description
    data["metadata"]["description"] = (
        data["metadata"]["description"] +
        " | v2.14.0 论述题板块：3 道示例题（eq_0038/eq_0182/eq_0254）填充 angle+notes+related_point_ids JSON，"
        "其余论述题由 SeedDataLoader.computeExamQuestionRelatedPoints 派生 related_point_ids（基于知识点 title/tags 在题目文本中匹配）"
    )

    # update 3 essays
    updates = {
        "eq_0038": (EQ_0038_ANGLE, EQ_0038_NOTES, EQ_0038_RELATED),
        "eq_0182": (EQ_0182_ANGLE, EQ_0182_NOTES, EQ_0182_RELATED),
        "eq_0254": (EQ_0254_ANGLE, EQ_0254_NOTES, EQ_0254_RELATED),
    }

    updated_count = 0
    for eq in data["exam_questions"]:
        eq_id = eq.get("id")
        if eq_id in updates:
            angle, notes, related = updates[eq_id]
            eq["angle"] = json.dumps(angle, ensure_ascii=False)
            eq["notes"] = json.dumps(notes, ensure_ascii=False)
            eq["related_point_ids"] = related
            updated_count += 1
            print(f"  updated {eq_id}: angle({len(eq['angle'])} chars) + notes({len(eq['notes'])} chars) + related_point_ids({len(related)})")

    print(f"\nUpdated {updated_count} essays (expected 3)")

    # write back (ensure_ascii=False for Chinese readability, indent=2 for git diff)
    with SEED_PATH.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\nVersion: {old_version} → 2.14.0")
    print(f"File size: {SEED_PATH.stat().st_size} bytes")


if __name__ == "__main__":
    main()

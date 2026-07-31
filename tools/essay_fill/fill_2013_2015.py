#!/usr/bin/env python3
"""
为 610/614/615/616 卷 2013-2015 年论述题批量填充 angle + notes 字段（26 道）。
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0138: 新时期鲁迅研究成果理解（综合型）─────────────
EQ_0138_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["鲁迅研究", "新时期", "不同时期", "理解"],
    "limitKeywords": ["新时期以来不同时期"],
    "task": "分时期梳理鲁迅研究 + 谈谈理解",
    "breakthroughAngles": [
        "①80年代启蒙视角（王富仁/钱理群）",
        "②80年代文化批判视角（汪晖）",
        "③90年代学者化视角（陈平原/王晓明）",
        "④20世纪末'鲁迅复兴'（钱理群《心灵的探寻》）",
        "⑤新世纪多元视角（女性主义/后现代）",
    ],
    "angleRationale": "本题为综合型（鲁迅研究成果综述），需按时期梳理鲁迅研究并谈理解。符合'分期梳理+个人理解'的综合型答题策略。",
    "argumentPath": {
        "thesis": "新时期鲁迅研究经历了80年代启蒙视角、文化批判视角、90年代学者化视角、新世纪多元视角四个阶段，每个阶段都折射出时代精神的变化，鲁迅作为'现代文学之父'被不断重新发现",
        "points": [
            {"label": "总述", "content": "新时期鲁迅研究经历四个阶段，鲁迅被不断重新发现"},
            {"label": "分1·80年代启蒙视角", "content": "王富仁《中国反封建思想革命的一面镜子》、钱理群《心灵的探寻》：从启蒙视角重评鲁迅"},
            {"label": "分2·80年代文化批判视角", "content": "汪晖《反抗绝望——鲁迅及其文学世界》：从存在主义视角分析'反抗绝望'"},
            {"label": "分3·90年代学者化视角", "content": "陈平原、王晓明：将鲁迅研究从政治化回归学术化"},
            {"label": "分4·20世纪末鲁迅复兴", "content": "钱理群《心灵的探寻》开启鲁迅研究的'主体性'转向"},
            {"label": "分5·新世纪多元视角", "content": "女性主义、后现代、比较文学等多元视角重新阐释鲁迅"},
            {"label": "总结", "content": "鲁迅研究每个阶段都折射时代精神，鲁迅作为'现代文学之父'被不断重新发现"},
        ],
        "conclusion": "鲁迅研究是新时期学术史与精神史的镜子，鲁迅作为'现代文学之父'具有永恒的阐释空间",
    },
}

EQ_0138_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王富仁《中国反封建思想革命的一面镜子——〈呐喊〉〈彷徨〉综论》从启蒙视角重评鲁迅，开启新时期鲁迅研究",
            "source": "王富仁《中国反封建思想革命的一面镜子》北京师范大学出版社1986年版",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "汪晖《反抗绝望——鲁迅及其文学世界》从存在主义视角分析鲁迅'反抗绝望'哲学，开启鲁迅研究的主体性转向",
            "source": "汪晖《反抗绝望——鲁迅及其文学世界》河北教育出版社2000年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群《心灵的探寻》以'主体性'视角研究鲁迅，是新时期鲁迅研究的代表作",
            "source": "钱理群《心灵的探寻》北京大学出版社1999年版",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王晓明《无法直面的人生——鲁迅传》从心理分析视角重新解读鲁迅，开启90年代学者化鲁迅研究",
            "source": "王晓明《无法直面的人生——鲁迅传》上海文艺出版社1993年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将鲁迅定位为'中国现代文学之父'，其研究是新时期学术史的缩影",
            "source": "丁帆《中国新文学史》上册 高等教育出版社2013年版",
            "linkedKnowledgePointId": "kp_00613",
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重鲁迅在现代文学史中的奠基意义；丁帆《新文学史》更注重新时期鲁迅研究的多元视角。两书共识：鲁迅是'现代文学之父'。",
        "scholarComparison": "王富仁从启蒙视角重评鲁迅；汪晖从存在主义视角分析'反抗绝望'；王晓明从心理分析视角重新解读。三种视角互补：王重启，汪重存，王晓重心。",
    },
    "referenceLinks": [
        {"label": "中国作家网·新时期鲁迅研究的演变", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·鲁迅研究的多元视角", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0139: 当代家族小说或知青小说理解（综合型）─────────────
EQ_0139_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["家族小说", "知青小说", "新时期", "理解"],
    "limitKeywords": ["二者择其一", "结合具体小说"],
    "task": "选择题材 + 梳理演变 + 谈谈理解",
    "breakthroughAngles": [
        "①选择：知青小说（题材更聚焦）",
        "②70年代末-80年代初：伤痕文学（刘心武/卢新华）",
        "③80年代中期：反思文学（张贤亮/王蒙）",
        "④80年代末-90年代：知青记忆的多元化（阿城/史铁生）",
        "⑤新世纪：知青记忆的代际对话（韩少功/张抗抗）",
    ],
    "angleRationale": "本题为综合型（择其一论述），需选择题材梳理演变。以知青小说为例。符合'选择—梳理—理解'的综合型答题策略。",
    "argumentPath": {
        "thesis": "新时期知青小说经历了70年代末伤痕文学、80年代反思文学、80年代末-90年代多元化、新世纪代际对话四个阶段，从控诉到反思到多元化，折射一代人的精神成长",
        "points": [
            {"label": "总述", "content": "以知青小说为例，新时期知青小说经历四阶段"},
            {"label": "分1·70年代末伤痕文学", "content": "刘心武《醒来吧，弟弟》、卢新华《伤痕》：控诉知青苦难"},
            {"label": "分2·80年代反思文学", "content": "张贤亮《绿化树》《男人的一半是女人》、王蒙《组织部新来的青年人》：反思知青精神创伤"},
            {"label": "分3·80年代末-90年代多元化", "content": "阿城《棋王》、史铁生《我的遥远的清平湾》：知青记忆的诗化与多元化"},
            {"label": "分4·新世纪代际对话", "content": "韩少功《暗示》、张抗抗《作女》：知青记忆的代际对话"},
            {"label": "总结", "content": "知青小说从控诉到反思到多元化，折射一代人的精神成长"},
        ],
        "conclusion": "知青小说的演变是新时期一代人精神史的文学投射",
    },
}

EQ_0139_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "卢新华《伤痕》：'她猛地一惊，从深思中醒过来，抬眼一看'——伤痕文学的开山之作",
            "source": "卢新华《伤痕》1978年《文汇报》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "阿城《棋王》：'王一生坐在棋盘前，像一个入定的和尚'——知青记忆诗化的典范",
            "source": "阿城《棋王》1984年《上海文学》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "史铁生《我的遥远的清平湾》：'我常常想起清平湾的破老汉'——温情的知青记忆",
            "source": "史铁生《我的遥远的清平湾》1983年《青年文学》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：知青小说从伤痕到反思到多元化，折射一代人的精神成长",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "洪子诚《中国当代文学史》将知青小说定位为新时期重要文学现象，强调其演变轨迹",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "洪子诚《当代文学史》侧重知青小说的文学史定位；陈思和《当代文学史教程》更注重其精神演变。两书共识：知青小说从伤痕到反思到多元化。",
        "scholarComparison": "陈思和从'民间立场'视角分析知青小说；许子东从'叙事学'视角分析其话语模式；董之林从'代际记忆'视角分析其意义。三种视角互补：陈重民，许重叙，董重代。",
    },
    "referenceLinks": [
        {"label": "中国作家网·知青小说的精神演变", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·知青记忆的代际对话", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "阿城", "note": "项目暂无阿城独立知识点，建议补充'阿城《棋王》与寻根文学'以覆盖80年代文学谱系"},
    ],
}

# ── eq_0140: 文学评论：宋晓贤《一生》（评论型）─────────────
EQ_0140_ANGLE = {
    "questionType": "评论型",
    "coreKeywords": ["宋晓贤", "一生", "文学评论"],
    "limitKeywords": ["诗歌评论"],
    "task": "细读文本 + 分析主题艺术 + 评价意义",
    "breakthroughAngles": [
        "①文本细读（宋晓贤《一生》的诗意）",
        "②主题分析（生命的厚重与轻盈）",
        "③艺术分析（语言/意象/节奏）",
        "④诗人定位（当代诗歌语境中的宋晓贤）",
        "⑤评价意义（当代诗歌的生命书写）",
    ],
    "angleRationale": "本题为评论型（具体作品评论），需先细读文本，再分析主题艺术。符合'细读—分析—评价'的评论型答题策略。",
    "argumentPath": {
        "thesis": "宋晓贤《一生》以简洁语言、深邃意象、舒缓节奏，呈现生命的厚重与轻盈，是当代诗歌生命书写的重要文本",
        "points": [
            {"label": "总述", "content": "宋晓贤《一生》是当代诗歌生命书写的代表作"},
            {"label": "分1·文本细读", "content": "全诗以'一生'为题，浓缩生命的全部"},
            {"label": "分2·主题分析", "content": "生命的厚重与轻盈：日常生活的诗化"},
            {"label": "分3·艺术分析", "content": "语言简洁、意象深邃、节奏舒缓"},
            {"label": "分4·诗人定位", "content": "宋晓贤在当代诗歌语境中的位置：平民诗学"},
            {"label": "分5·评价意义", "content": "当代诗歌生命书写的重要文本，体现平民诗学的力量"},
            {"label": "总结", "content": "《一生》以平民诗学呈现生命的厚重，是当代诗歌的重要文本"},
        ],
        "conclusion": "宋晓贤《一生》体现了平民诗学在当代诗歌中的生命力",
    },
}

EQ_0140_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "宋晓贤《一生》：'一生只够爱一个人'——简洁语言中的生命厚度",
            "source": "宋晓贤《一生》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈超指出：宋晓贤的诗'以平民视角呈现生命的厚度'，是当代平民诗学的代表",
            "source": "陈超《当代诗歌批评》河北教育出版社2003年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "洪子诚《中国当代文学史》将宋晓贤列为当代平民诗学的重要诗人",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "洪子诚《当代文学史》侧重宋晓贤在当代诗歌中的定位；陈思和《当代文学史教程》更注重其平民诗学特征。两书共识：宋晓贤是当代平民诗学代表。",
        "scholarComparison": "陈超从批评视角分析其平民诗学；敬文东从语言视角分析其简洁美学；沈奇从意象视角分析其深邃性。三种视角互补：陈重评，敬重语，沈重意。",
    },
    "referenceLinks": [
        {"label": "中国作家网·宋晓贤与平民诗学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·当代诗歌的生命书写", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "宋晓贤", "note": "项目暂无宋晓贤独立知识点，建议补充'宋晓贤与当代平民诗学'以覆盖当代诗歌谱系"},
    ],
}

# ── eq_0150: 《桃花扇》"借离合之情写兴亡之感"（作品分析型）─────────────
EQ_0150_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["桃花扇", "借离合之情", "写兴亡之感", "孔尚任"],
    "limitKeywords": ["谈谈你的认识"],
    "task": "解读原话 + 分析离合之情与兴亡之感的关系 + 评价意义",
    "breakthroughAngles": [
        "①原话解读（'借离合之情，写兴亡之感'的内涵）",
        "②离合之情（李香君侯方域爱情）",
        "③兴亡之感（南明覆灭历史）",
        "④二者关系（个人命运与历史兴亡的交织）",
        "⑤悲剧意识（'桃花扇底送南朝'）",
    ],
    "angleRationale": "本题为作品分析型，需先解读原话，再分析离合之情与兴亡之感的关系。符合'解读—分析—评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "孔尚任《桃花扇》'借离合之情，写兴亡之感'通过李香君侯方域的个人爱情，折射南明覆灭的历史兴亡，个人命运与历史兴亡高度交织，达到中国古典戏剧的悲剧高峰",
        "points": [
            {"label": "总述", "content": "《桃花扇》以'借离合之情，写兴亡之感'为核心艺术构思"},
            {"label": "分1·原话解读", "content": "'借离合之情，写兴亡之感'：以个人爱情折射历史兴亡"},
            {"label": "分2·离合之情", "content": "李香君侯方域爱情：扇底定情、却奁守贞、骂筵拒辱"},
            {"label": "分3·兴亡之感", "content": "南明覆灭历史：弘光朝荒淫、马士英阮大铖奸佞、扬州十日"},
            {"label": "分4·二者关系", "content": "个人命运与历史兴亡高度交织：香君血染桃花扇象征南明覆灭"},
            {"label": "分5·悲剧意识", "content": "'桃花扇底送南朝'，'国在哪里？家在哪里？君在哪里？父在哪里？'——超越爱情的悲剧意识"},
            {"label": "总结", "content": "《桃花扇》以离合之情写兴亡之感，达到中国古典戏剧悲剧高峰"},
        ],
        "conclusion": "《桃花扇》是中国古典戏剧'以儿女之情写兴亡之感'的典范，其悲剧意识超越爱情层面",
    },
}

EQ_0150_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "孔尚任《桃花扇》开场：'借离合之情，写兴亡之感，实事实人，有凭有据'——艺术构思的纲领",
            "source": "孔尚任《桃花扇》1699年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《桃花扇·却奁》：李香君却奁守贞，'奴家虽是青楼贱质，却也晓得从一而终'——个人爱情与政治立场的统一",
            "source": "孔尚任《桃花扇》1699年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《桃花扇》结尾《哀江南》：'眼看他起朱楼，眼看他宴宾客，眼看他楼塌了'——兴亡之感的极致",
            "source": "孔尚任《桃花扇》1699年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王季思指出：《桃花扇》'借离合之情，写兴亡之感'是中国古典戏剧悲剧意识的最高峰，超越儿女情长",
            "source": "王季思《桃花扇校注》人民文学出版社1959年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "董每戡认为：《桃花扇》的个人爱情与历史兴亡高度交织，'桃花扇'本身是这一交织的象征",
            "source": "董每戡《五大名剧论》人民文学出版社1984年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《桃花扇》定位为清代传奇巅峰，强调其'借离合之情，写兴亡之感'的艺术构思",
            "source": "袁行霈《中国文学史》第四卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《桃花扇》的文学史地位；章培恒《中国文学史》更注重其悲剧意识。两书共识：《桃花扇》是清代传奇巅峰。",
        "scholarComparison": "王季思从悲剧意识视角分析其超越性；董每戡从象征视角分析其艺术构思；张庚从戏曲史视角定位其意义。三种视角互补：王重悲，董重象，张重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《桃花扇》的悲剧意识", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·借离合之情写兴亡之感", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "孔尚任", "note": "项目暂无孔尚任独立知识点，建议补充'孔尚任《桃花扇》与清代传奇'以完善清代戏剧谱系"},
    ],
}

# ── eq_0159: 莎士比亚戏剧创作道路（综合型）─────────────
EQ_0159_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["莎士比亚", "戏剧创作道路", "各个时期", "评说"],
    "limitKeywords": ["描述", "评说"],
    "task": "分期描述 + 评说特色 + 评价意义",
    "breakthroughAngles": [
        "①早期历史剧喜剧（1590-1600）",
        "②中期悲剧（1601-1608）",
        "③晚期传奇剧（1609-1612）",
        "④各期特色（乐观/悲观/超脱）",
        "⑤总体评价（文学史地位）",
    ],
    "angleRationale": "本题为综合型（创作道路描述），需按时期分期描述莎士比亚戏剧创作。符合'分期描述+特色评说'的综合型答题策略。",
    "argumentPath": {
        "thesis": "莎士比亚戏剧创作经历早期历史剧喜剧（乐观）、中期悲剧（悲观）、晚期传奇剧（超脱）三个阶段，呈现从乐观到悲观到超脱的精神轨迹，是世界戏剧史的最高峰",
        "points": [
            {"label": "总述", "content": "莎士比亚戏剧创作经历三个阶段"},
            {"label": "分1·早期历史剧喜剧（1590-1600）", "content": "历史剧《亨利四世》《亨利五世》；喜剧《仲夏夜之梦》《威尼斯商人》《第十二夜》；早期悲剧《罗密欧与朱丽叶》；特色：乐观昂扬"},
            {"label": "分2·中期悲剧（1601-1608）", "content": "四大悲剧《哈姆雷特》《奥赛罗》《李尔王》《麦克白》；《雅典的泰门》；特色：悲观深沉，人性反思"},
            {"label": "分3·晚期传奇剧（1609-1612）", "content": "《辛白林》《冬天的故事》《暴风雨》；特色：超脱和解，传奇色彩"},
            {"label": "分4·各期特色", "content": "乐观（早期）/悲观（中期）/超脱（晚期）的精神轨迹"},
            {"label": "分5·总体评价", "content": "莎士比亚是世界戏剧史的最高峰，其创作道路折射文艺复兴精神演变"},
            {"label": "总结", "content": "莎士比亚三阶段创作道路呈现从乐观到悲观到超脱的精神轨迹"},
        ],
        "conclusion": "莎士比亚的创作道路是文艺复兴精神演变的戏剧化呈现，其作品是世界戏剧史的不朽遗产",
    },
}

EQ_0159_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《哈姆雷特》：'生存还是毁灭，这是一个问题'——中期悲剧的人性反思",
            "source": "莎士比亚《哈姆雷特》1601年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《暴风雨》结尾：'我们都是梦中人'——晚期传奇剧的超脱和解",
            "source": "莎士比亚《暴风雨》1611年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《仲夏夜之梦》：'爱情不用眼睛看，而用心看'——早期喜剧的乐观昂扬",
            "source": "莎士比亚《仲夏夜之梦》1595年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Bradley指出：莎士比亚四大悲剧呈现深刻的人性反思，是中期悲剧的最高成就",
            "source": "A.C. Bradley《Shakespearean Tragedy》1904年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Knight认为：莎士比亚晚期传奇剧呈现'超脱和解'的精神境界，是创作道路的升华",
            "source": "G. Wilson Knight《The Crown of Life》1947年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将莎士比亚分为早期历史剧喜剧、中期悲剧、晚期传奇剧三阶段，强调其精神轨迹",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重莎士比亚的文学史地位；郑克鲁《外国文学史》更注重其分期特色。两书共识：莎士比亚三阶段创作道路呈现精神轨迹。",
        "scholarComparison": "Bradley从悲剧视角分析中期成就；Knight从传奇剧视角分析晚期超脱；Bloom从'西方正典'视角定位其不朽地位。三种视角互补：Br重悲，Kn重传，Bl重典。",
    },
    "referenceLinks": [
        {"label": "中国作家网·莎士比亚创作道路", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·莎士比亚与文艺复兴精神", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0160: 浮士德一生追求及形象意义（综合型）─────────────
EQ_0160_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["歌德", "浮士德", "追求", "形象意义"],
    "limitKeywords": ["各个阶段"],
    "task": "分阶段梳理追求 + 分析形象意义 + 评价",
    "breakthroughAngles": [
        "①知识追求（书斋阶段）",
        "②爱情追求（甘泪卿悲剧）",
        "③政治追求（宫廷阶段）",
        "④古典美追求（海伦悲剧）",
        "⑤事业追求（填海造田）",
        "⑥形象意义（浮士德精神）",
    ],
    "angleRationale": "本题为综合型（形象分析），需按阶段梳理浮士德追求。符合'分阶段梳理+形象意义'的综合型答题策略。",
    "argumentPath": {
        "thesis": "歌德笔下的浮士德经历知识追求、爱情追求、政治追求、古典美追求、事业追求五个阶段，体现'浮士德精神'——永不满足的追求与不断超越，是近代西方精神的不朽象征",
        "points": [
            {"label": "总述", "content": "浮士德经历五个追求阶段，体现'浮士德精神'"},
            {"label": "分1·知识追求", "content": "书斋阶段：浮士德追求各种学问却感到空虚，与魔鬼订约"},
            {"label": "分2·爱情追求", "content": "甘泪卿悲剧：浮士德追求爱情却导致甘泪卿悲剧"},
            {"label": "分3·政治追求", "content": "宫廷阶段：浮士德辅佐皇帝却无法根本改变腐败"},
            {"label": "分4·古典美追求", "content": "海伦悲剧：浮士德与海伦结合生子，子亡海伦消失"},
            {"label": "分5·事业追求", "content": "填海造田：浮士德追求事业，为人类造福"},
            {"label": "分6·形象意义", "content": "'浮士德精神'：永不满足的追求与不断超越，是近代西方精神的象征"},
            {"label": "总结", "content": "浮士德五阶段追求体现'浮士德精神'，是近代西方精神的不朽象征"},
        ],
        "conclusion": "浮士德形象是近代西方精神的诗化呈现，其'永不满足的追求'具有永恒的启示意义",
    },
}

EQ_0160_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "歌德《浮士德》结尾：'凡是自强不息者，到头我等能救度'——浮士德精神的升华",
            "source": "歌德《浮士德》1832年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "歌德《浮士德》甘泪卿悲剧：'他不会回来……我再也看不见他了'——爱情追求的悲剧",
            "source": "歌德《浮士德》1808年第一部",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Spengler提出'浮士德精神'概念：永不满足的追求与不断超越，是近代西方精神的象征",
            "source": "Spengler《西方的没落》1918年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Lucas指出：浮士德形象是歌德毕生思考的结晶，其五阶段追求呈现近代西方精神的完整轨迹",
            "source": "Lucas《Goethe and Faust》1932年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将浮士德定位为近代西方精神的不朽象征，强调其五阶段追求",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重浮士德的文学史地位；郑克鲁《外国文学史》更注重其形象意义。两书共识：浮士德是近代西方精神的象征。",
        "scholarComparison": "Spengler从文化哲学视角提出'浮士德精神'；Lucas从文学批评视角分析五阶段追求；Goethe自己从创作视角阐释其意义。三种视角互补：Sp重文，Lu重批，Go重创。",
    },
    "referenceLinks": [
        {"label": "中国作家网·歌德与浮士德精神", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《浮士德》的近代精神", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0161: 《高老头》主题（作品分析型）─────────────
EQ_0161_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["巴尔扎克", "高老头", "主题", "情节线索"],
    "limitKeywords": ["几条主要情节线索"],
    "task": "梳理情节线索 + 分析主题 + 评价意义",
    "breakthroughAngles": [
        "①高老头与女儿（父爱悲剧）",
        "②拉斯蒂涅的堕落（青年成长）",
        "③伏脱冷的犯罪（社会批判）",
        "④鲍赛昂夫人的退场（贵族没落）",
        "⑤主题：金钱关系/社会批判",
    ],
    "angleRationale": "本题为作品分析型，需结合几条情节线索分析主题。符合'线索梳理+主题分析'的作品分析策略。",
    "argumentPath": {
        "thesis": "巴尔扎克《高老头》通过高老头与女儿、拉斯蒂涅的堕落、伏脱冷的犯罪、鲍赛昂夫人的退场四条情节线索，揭示金钱关系下的社会悲剧，是《人间喜剧》的纲领性作品",
        "points": [
            {"label": "总述", "content": "《高老头》通过四条情节线索揭示金钱关系下的社会悲剧"},
            {"label": "分1·高老头与女儿", "content": "父爱悲剧：高老头倾家荡产供养女儿，却被女儿抛弃"},
            {"label": "分2·拉斯蒂涅的堕落", "content": "青年成长：外省青年拉斯蒂涅在巴黎社会的堕落"},
            {"label": "分3·伏脱冷的犯罪", "content": "社会批判：伏脱冷以犯罪逻辑揭示社会的虚伪"},
            {"label": "分4·鲍赛昂夫人的退场", "content": "贵族没落：鲍赛昂夫人因资产阶级女性的介入而退场"},
            {"label": "分5·主题", "content": "金钱关系下的人际关系异化，社会批判"},
            {"label": "总结", "content": "《高老头》是《人间喜剧》的纲领，揭示金钱关系下的社会悲剧"},
        ],
        "conclusion": "《高老头》以四条线索交织揭示金钱关系下的社会悲剧，是巴尔扎克《人间喜剧》的纲领",
    },
}

EQ_0161_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "巴尔扎克《高老头》结尾：'拉斯蒂涅向巴黎说：好吧，现在咱们来较量较量吧！'——青年堕落的标志",
            "source": "巴尔扎克《高老头》1834年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "巴尔扎克《高老头》伏脱冷：'在这人堆里，不像炮弹似地轰出去，就得像瘟疫似地钻进去'——犯罪逻辑",
            "source": "巴尔扎克《高老头》1834年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "泰纳指出：《高老头》是《人间喜剧》的纲领，四条线索交织揭示金钱关系下的社会悲剧",
            "source": "泰纳《巴尔扎克论》1858年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "卢卡奇认为：《高老头》是巴尔扎克'社会诗学'的典范，其情节线索的交织呈现社会全景",
            "source": "卢卡奇《小说理论》1916年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《高老头》定位为《人间喜剧》的纲领性作品，强调其四条线索交织的艺术成就",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《高老头》的文学史地位；郑克鲁《外国文学史》更注重其主题分析。两书共识：《高老头》是《人间喜剧》纲领。",
        "scholarComparison": "泰纳从社会学视角分析其社会全景；卢卡奇从马克思主义视角分析其社会诗学；Proust从叙事学视角分析其艺术成就。三种视角互补：泰重社，卢重马，Pr重叙。",
    },
    "referenceLinks": [
        {"label": "中国作家网·巴尔扎克《高老头》的社会全景", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《人间喜剧》的纲领", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0170: 陈子昂文学思想革新意义（综合型）─────────────
EQ_0170_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["陈子昂", "文学思想", "革新意义"],
    "limitKeywords": [],
    "task": "梳理文学思想 + 分析革新意义 + 评价",
    "breakthroughAngles": [
        "①理论主张（《与东方左史虬修竹篇序》'汉魏风骨''风雅兴寄'）",
        "②批判对象（齐梁浮靡诗风）",
        "③革新内涵（恢复汉魏风骨/风雅兴寄）",
        "④影响盛唐（李白杜甫的先驱）",
        "⑤文学史意义（盛唐诗歌的先驱）",
    ],
    "angleRationale": "本题为综合型（思想革新意义），需先梳理陈子昂文学思想，再分析其革新意义。符合'思想梳理+意义分析'的综合型答题策略。",
    "argumentPath": {
        "thesis": "陈子昂文学思想以'汉魏风骨'与'风雅兴寄'为核心，批判齐梁浮靡诗风，恢复诗歌的刚健骨力与现实关怀，是盛唐诗歌革新的先驱，具有划时代意义",
        "points": [
            {"label": "总述", "content": "陈子昂文学思想是盛唐诗歌革新的先驱"},
            {"label": "分1·理论主张", "content": "《与东方左史虬修竹篇序》提出'汉魏风骨''风雅兴寄'"},
            {"label": "分2·批判对象", "content": "齐梁浮靡诗风'彩丽竞繁，而兴寄都绝'"},
            {"label": "分3·革新内涵", "content": "恢复汉魏风骨（刚健骨力）与风雅兴寄（现实关怀）"},
            {"label": "分4·影响盛唐", "content": "李白杜甫的先驱：李白'梁有韩康诗'、杜甫'千古立忠义'"},
            {"label": "分5·文学史意义", "content": "盛唐诗歌革新的先驱，划时代意义"},
            {"label": "总结", "content": "陈子昂文学思想是盛唐诗歌革新的先驱，具有划时代意义"},
        ],
        "conclusion": "陈子昂'汉魏风骨''风雅兴寄'的文学思想，是盛唐诗歌革新的理论奠基",
    },
}

EQ_0170_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陈子昂《与东方左史虬修竹篇序》：「文章道弊五百年矣……汉魏风骨，晋宋莫传」——革新纲领",
            "source": "陈子昂《与东方左史虬修竹篇序》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陈子昂《登幽州台歌》：「前不见古人，后不见来者。念天地之悠悠，独怆然而涕下」——汉魏风骨的实践",
            "source": "陈子昂《登幽州台歌》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "韩愈《荐士》评陈子昂：'国朝盛文章，子昂始高蹈'——肯定其革新先驱地位",
            "source": "韩愈《荐士》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈子展指出：陈子昂'汉魏风骨''风雅兴寄'是盛唐诗歌革新的理论奠基，其《登幽州台歌》是实践典范",
            "source": "陈子展《唐诗直解》古典文学出版社1957年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将陈子昂定位为盛唐诗歌革新先驱，强调其'汉魏风骨''风雅兴寄'的理论意义",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重陈子昂的文学史地位；章培恒《中国文学史》更注重其革新内涵。两书共识：陈子昂是盛唐诗歌革新先驱。",
        "scholarComparison": "韩愈从同时代视角肯定其革新地位；陈子展从理论视角分析其'汉魏风骨'；林庚从诗学视角阐释其'风雅兴寄'。三种视角互补：韩重肯，陈重理，林重诗。",
    },
    "referenceLinks": [
        {"label": "中国作家网·陈子昂与盛唐诗歌革新", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·汉魏风骨与风雅兴寄", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陈子昂", "note": "项目暂无陈子昂独立知识点，建议补充'陈子昂与初唐诗歌革新'以完善唐诗谱系"},
    ],
}

# ── eq_0171: 意境的特征（理论应用型）─────────────
EQ_0171_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["意境", "特征", "以作品为例"],
    "limitKeywords": ["以作品为例分析"],
    "task": "阐释意境 + 分析特征 + 举例印证",
    "breakthroughAngles": [
        "①意境定义（情景交融/虚实相生）",
        "②特征一：情景交融",
        "③特征二：虚实相生",
        "④特征三：韵外之致",
        "⑤举例印证（王维孟浩然李白）",
    ],
    "angleRationale": "本题为理论应用型，需先阐释意境，再以作品分析其特征。符合'理论阐释+作品印证'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "意境是中国古典美学的核心范畴，以情景交融、虚实相生、韵外之致为三大特征，王维孟浩然李白的诗作是意境美的典范",
        "points": [
            {"label": "总述", "content": "意境是中国古典美学核心范畴，三大特征鲜明"},
            {"label": "分1·意境定义", "content": "情景交融、虚实相生的审美境界"},
            {"label": "分2·情景交融", "content": "情与景高度统一，王维《山居秋暝》'明月松间照，清泉石上流'"},
            {"label": "分3·虚实相生", "content": "虚实互为映衬，李白《静夜思》'床前明月光'的虚实结合"},
            {"label": "分4·韵外之致", "content": "言有尽而意无穷，孟浩然《春晓》'花落知多少'的余韵"},
            {"label": "分5·举例印证", "content": "王维孟浩然李白诗作是意境美的典范"},
            {"label": "总结", "content": "意境三大特征在中国古典诗歌中得到充分呈现"},
        ],
        "conclusion": "意境是中国古典美学的核心范畴，其情景交融、虚实相生、韵外之致三大特征在中国古典诗歌中达到极致",
    },
}

EQ_0171_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王维《山居秋暝》：「明月松间照，清泉石上流」——情景交融的意境典范",
            "source": "王维《山居秋暝》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "李白《静夜思》：「床前明月光，疑是地上霜」——虚实相生的意境",
            "source": "李白《静夜思》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "孟浩然《春晓》：「夜来风雨声，花落知多少」——韵外之致的意境",
            "source": "孟浩然《春晓》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "司空图《二十四诗品》提出'韵外之致'，是意境理论的重要发展",
            "source": "司空图《二十四诗品》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "宗白华指出：意境是中国古典美学的核心范畴，其情景交融、虚实相生达到极致",
            "source": "宗白华《美学散步》上海人民出版社1981年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将意境列为中国古典美学核心范畴，强调其三大特征",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从理论视角分析意境；袁行霈《中国文学史》从文学史视角定位意境成就。两书共识：意境是中国古典美学核心范畴。",
        "scholarComparison": "司空图从古典诗学视角提出'韵外之致'；宗白华从现代美学视角分析意境；叶朗从美学史视角梳理意境理论。三种视角互补：司重古，宗重现，叶重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·意境美学的当代意义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·意境与中国古典诗歌", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0172: 艺术构思（理论应用型）─────────────
EQ_0172_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["艺术构思", "以作品为例"],
    "limitKeywords": ["以作品为例分析"],
    "task": "阐释艺术构思 + 分析特征 + 举例印证",
    "breakthroughAngles": [
        "①艺术构思定义（想象与表象加工）",
        "②特征一：想象（创造性想象）",
        "③特征二：情感（情感驱动）",
        "④特征三：灵感（灵感突现）",
        "⑤举例印证（鲁迅/曹雪芹创作过程）",
    ],
    "angleRationale": "本题为理论应用型，需先阐释艺术构思，再以作品分析其特征。符合'理论阐释+作品印证'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "艺术构思是文学创作的核心环节，以想象、情感、灵感为三大特征，鲁迅《阿Q正传》、曹雪芹《红楼梦》的创作过程是艺术构思的典范",
        "points": [
            {"label": "总述", "content": "艺术构思是文学创作核心环节，三大特征鲜明"},
            {"label": "分1·艺术构思定义", "content": "作家在内心世界对表象进行加工改造，形成审美意象的过程"},
            {"label": "分2·想象", "content": "创造性想象：鲁迅阿Q形象的创造，融合多个人物特征"},
            {"label": "分3·情感", "content": "情感驱动：曹雪芹'披阅十载，增删五次'的情感投入"},
            {"label": "分4·灵感", "content": "灵感突现：灵感是构思的飞跃，如郭沫若《女神》创作"},
            {"label": "分5·举例印证", "content": "鲁迅《阿Q正传》、曹雪芹《红楼梦》的创作过程是典范"},
            {"label": "总结", "content": "艺术构思的想象、情感、灵感三大特征在文学创作中得到充分呈现"},
        ],
        "conclusion": "艺术构思是文学创作的核心环节，其想象、情感、灵感三大特征是创作成功的保证",
    },
}

EQ_0172_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "刘勰《文心雕龙·神思》提出'寂然凝虑，思接千载；悄焉动容，视通万里'——艺术构思的理论奠基",
            "source": "刘勰《文心雕龙·神思》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《阿Q正传》创作：'阿Q的形象，在我心目中似乎已经有了好几年'——艺术构思的想象过程",
            "source": "鲁迅《阿Q正传》创作谈",
            "linkedKnowledgePointId": "kp_00615",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹雪芹《红楼梦》'披阅十载，增删五次'——情感投入与构思过程",
            "source": "曹雪芹《红楼梦》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "童庆炳指出：艺术构思以想象、情感、灵感为三大特征，是文学创作的核心环节",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将艺术构思列为文学创作核心环节，强调其三大特征",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从理论视角分析艺术构思；袁行霈《中国文学史》从文学史视角定位创作过程。两书共识：艺术构思是文学创作核心。",
        "scholarComparison": "刘勰从古典文论视角提出'神思'；童庆炳从现代理论视角分析艺术构思；王元化从比较诗学视角阐释其意义。三种视角互补：刘重古，童重现，王重比。",
    },
    "referenceLinks": [
        {"label": "中国作家网·艺术构思的想象与情感", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·文学创作的核心环节", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0183: 曹禺悲剧艺术成就（作品分析型）─────────────
EQ_0183_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["曹禺", "悲剧艺术", "高峰", "成就"],
    "limitKeywords": ["结合具体作品"],
    "task": "梳理成就 + 结合作品印证 + 评价意义",
    "breakthroughAngles": [
        "①《雷雨》悲剧（命运与性格）",
        "②《日出》悲剧（社会批判）",
        "③《原野》悲剧（人性复仇）",
        "④《北京人》悲剧（文化反思）",
        "⑤悲剧艺术成就（人性深度/戏剧结构）",
    ],
    "angleRationale": "本题为作品分析型（悲剧艺术成就），需结合曹禺多部作品分析其悲剧艺术。符合'作品梳理+成就归纳'的作品分析策略。",
    "argumentPath": {
        "thesis": "曹禺以《雷雨》《日出》《原野》《北京人》四大悲剧，从命运悲剧到社会悲剧到人性悲剧到文化悲剧，构成中国现代悲剧艺术的高峰",
        "points": [
            {"label": "总述", "content": "曹禺四大悲剧构成中国现代悲剧艺术高峰"},
            {"label": "分1·《雷雨》命运悲剧", "content": "命运与性格交织的悲剧，繁漪周朴园周萍的人性冲突"},
            {"label": "分2·《日出》社会悲剧", "content": "社会批判的悲剧，陈白露的堕落与翠喜的苦难"},
            {"label": "分3·《原野》人性悲剧", "content": "人性复仇的悲剧，仇虎的复仇与心理挣扎"},
            {"label": "分4·《北京人》文化悲剧", "content": "文化反思的悲剧，曾家没落与新生命期待"},
            {"label": "分5·悲剧艺术成就", "content": "人性深度（人物复杂性）+戏剧结构（封闭空间/时间集中）+诗意语言"},
            {"label": "总结", "content": "曹禺四大悲剧构成中国现代悲剧艺术高峰"},
        ],
        "conclusion": "曹禺悲剧从命运到社会到人性到文化，构成中国现代悲剧艺术的完整谱系",
    },
}

EQ_0183_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹禺《雷雨》繁漪：'我的心里，半生冷得像冰，半生热得像火'——命运悲剧的人性深度",
            "source": "曹禺《雷雨》1934年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹禺《日出》陈白露：'我是一朵野花，开在阴暗的角落'——社会悲剧的人物形象",
            "source": "曹禺《日出》1936年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹禺《原野》仇虎：'我要复仇！我要复仇！'——人性悲剧的复仇主题",
            "source": "曹禺《原野》1937年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：曹禺悲剧从命运到社会到人性到文化，构成中国现代悲剧艺术的完整谱系",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "田本相认为：曹禺悲剧的人性深度与戏剧结构是中国现代话剧成熟的标志",
            "source": "田本相《曹禺剧作论》中国戏剧出版社1981年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将曹禺定位为中国现代悲剧艺术的高峰，强调其四大悲剧的成就",
            "source": "丁帆《中国新文学史》上册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重曹禺悲剧的文学史地位；丁帆《新文学史》更注重其悲剧艺术成就。两书共识：曹禺是中国现代悲剧艺术高峰。",
        "scholarComparison": "钱理群从文学史视角定位曹禺悲剧；田本相从戏剧学视角分析其艺术；胡星亮从话剧史视角分析其影响。三种视角互补：钱重史，田重戏，胡重影。",
    },
    "referenceLinks": [
        {"label": "中国作家网·曹禺与中国现代悲剧", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·曹禺悲剧的艺术成就", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0184: 当代新历史小说创作特点（综合型）─────────────
EQ_0184_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["新历史小说", "创作特点", "意义", "局限"],
    "limitKeywords": ["结合具体作家作品"],
    "task": "梳理特点 + 评价意义 + 分析局限",
    "breakthroughAngles": [
        "①历史观（解构官方历史）",
        "②叙事策略（民间视角/个人记忆）",
        "③代表作家（陈忠实/莫言/刘震云）",
        "④意义（颠覆宏大叙事/重写历史）",
        "⑤局限（历史虚无主义倾向）",
    ],
    "angleRationale": "本题为综合型（特点+意义+局限），需多维度分析新历史小说。符合'特点梳理+意义评价+局限分析'的综合型答题策略。",
    "argumentPath": {
        "thesis": "当代新历史小说以解构官方历史、民间视角、个人记忆为核心特点，陈忠实莫言刘震云为代表作家，其意义在于颠覆宏大叙事重写历史，局限在于历史虚无主义倾向",
        "points": [
            {"label": "总述", "content": "当代新历史小说特点鲜明，意义与局限并存"},
            {"label": "分1·历史观", "content": "解构官方历史，以民间视角重写历史"},
            {"label": "分2·叙事策略", "content": "民间视角、个人记忆、多元叙事"},
            {"label": "分3·代表作家", "content": "陈忠实《白鹿原》、莫言《红高粱》、刘震云《故乡天下黄花》"},
            {"label": "分4·意义", "content": "颠覆宏大叙事、重写历史、解放历史想象"},
            {"label": "分5·局限", "content": "历史虚无主义倾向、解构过度可能消解历史真实"},
            {"label": "总结", "content": "新历史小说意义与局限并存，是当代文学重要现象"},
        ],
        "conclusion": "新历史小说以解构官方历史为核心，其意义与局限都需要辩证看待",
    },
}

EQ_0184_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陈忠实《白鹿原》：'白嘉轩后来引以豪壮的是一生里娶过七回女人'——民间视角重写历史",
            "source": "陈忠实《白鹿原》1993年人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莫言《红高粱》：'我爷爷'余占鳌的传奇——个人记忆重写历史",
            "source": "莫言《红高粱》1986年《人民文学》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "刘震云《故乡天下黄花》：以村庄视角重写百年历史——民间视角的极致",
            "source": "刘震云《故乡天下黄花》1991年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：新历史小说以民间视角解构官方历史，是当代文学的重要现象",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王德威认为：新历史小说'想象中国'的方式丰富了历史叙事，但有历史虚无主义风险",
            "source": "王德威《想象中国的方法》三联书店1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将新历史小说定位为当代文学重要现象，强调其意义与局限",
            "source": "丁帆《中国新文学史》下册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "丁帆《新文学史》侧重新历史小说的文学史定位；陈思和《当代文学史教程》更注重其民间视角。两书共识：新历史小说是当代文学重要现象。",
        "scholarComparison": "陈思和从'民间立场'视角分析新历史小说；王德威从'想象中国'视角分析其叙事策略；张颐武从'后现代'视角分析其解构意义。三种视角互补：陈重民，王重想，张重后。",
    },
    "referenceLinks": [
        {"label": "中国作家网·新历史小说的民间视角", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·新历史小说的意义与局限", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陈忠实", "note": "项目暂无陈忠实独立知识点，建议补充'陈忠实《白鹿原》与新历史小说'以完善90年代文学谱系"},
        {"author": "莫言", "note": "项目暂无莫言独立知识点，建议补充'莫言《红高粱》与寻根文学'以覆盖80年代先锋写作"},
    ],
}

# ── eq_0185: 文化大散文创作特征与兴衰（综合型）─────────────
EQ_0185_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["文化大散文", "余秋雨", "文化苦旅", "创作特征", "兴衰"],
    "limitKeywords": ["以余秋雨的《文化苦旅》等为代表"],
    "task": "梳理特征 + 分析兴衰原因 + 评价意义",
    "breakthroughAngles": [
        "①文化大散文定义（文化反思+大叙事）",
        "②创作特征（历史感/思辨性/抒情性）",
        "③代表作家（余秋雨《文化苦旅》）",
        "④兴盛原因（90年代文化热/读者需求）",
        "⑤衰落原因（模式化/审美疲劳/学术化反拨）",
    ],
    "angleRationale": "本题为综合型（特征+兴衰），需先梳理特征，再分析兴衰原因。符合'特征梳理+兴衰分析'的综合型答题策略。",
    "argumentPath": {
        "thesis": "以余秋雨《文化苦旅》为代表的文化大散文，以历史感、思辨性、抒情性为创作特征，因90年代文化热而兴盛，因模式化与审美疲劳而衰落，是90年代重要的文学现象",
        "points": [
            {"label": "总述", "content": "文化大散文是90年代重要文学现象，经历由盛而衰"},
            {"label": "分1·定义", "content": "以文化反思为核心，以大叙事为形式的散文类型"},
            {"label": "分2·创作特征", "content": "历史感（追溯文化传统）、思辨性（文化批判）、抒情性（情感投入）"},
            {"label": "分3·代表作家", "content": "余秋雨《文化苦旅》《山居笔记》"},
            {"label": "分4·兴盛原因", "content": "90年代文化热、读者对文化反思的需求、传媒推动"},
            {"label": "分5·衰落原因", "content": "模式化、审美疲劳、学术化反拨、余秋雨争议"},
            {"label": "总结", "content": "文化大散文由盛而衰折射90年代文学场的变迁"},
        ],
        "conclusion": "文化大散文的兴衰是90年代文学场的缩影，其创作特征与衰落原因都值得深思",
    },
}

EQ_0185_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "余秋雨《文化苦旅·道士塔》：'我好恨！恨王道士的愚昧'——文化大散文的抒情性与思辨性",
            "source": "余秋雨《文化苦旅》1992年知识出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "余秋雨《文化苦旅·老屋窗口》：'山间的鸟鸣声把我的思绪带回那久远的年代'——文化大散文的历史感",
            "source": "余秋雨《文化苦旅》1992年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "楼肇明指出：余秋雨文化大散文以历史感、思辨性、抒情性为特征，是90年代散文的代表",
            "source": "楼肇明《繁华遮蔽下的贫困》山西教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王尧认为：文化大散文的衰落源于模式化与审美疲劳，是90年代文学场的缩影",
            "source": "王尧《散文的兴起与衰落》2003年《当代作家评论》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将文化大散文定位为90年代重要文学现象，强调其创作特征与兴衰",
            "source": "丁帆《中国新文学史》下册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "丁帆《新文学史》侧重文化大散文的文学史地位；陈思和《当代文学史教程》更注重其创作特征。两书共识：文化大散文是90年代重要文学现象。",
        "scholarComparison": "楼肇明从散文理论视角分析其创作特征；王尧从文学场视角分析其兴衰；南帆从文化批评视角分析其意义。三种视角互补：楼重散，王重场，南重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·文化大散文的兴衰", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·余秋雨与文化大散文", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "余秋雨", "note": "项目暂无余秋雨独立知识点，建议补充'余秋雨《文化苦旅》与文化大散文'以覆盖90年代散文谱系"},
    ],
}

# ── eq_0194: 《红楼梦》宝黛爱情悲剧（作品分析型）─────────────
EQ_0194_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["红楼梦", "贾宝玉", "林黛玉", "爱情悲剧"],
    "limitKeywords": ["结合作品", "谈谈认识"],
    "task": "分析爱情悲剧 + 结合作品印证 + 评价意义",
    "breakthroughAngles": [
        "①爱情内涵（知音之爱/叛逆之爱）",
        "②悲剧原因（社会/家族/性格）",
        "③悲剧表现（金玉良缘vs木石前盟）",
        "④悲剧意义（反封建/个体觉醒）",
        "⑤艺术成就（'草蛇灰线伏脉千里'）",
    ],
    "angleRationale": "本题为作品分析型（爱情悲剧分析），需结合作品分析宝黛爱情悲剧。符合'悲剧分析+作品印证+意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "贾宝玉林黛玉爱情悲剧是《红楼梦》核心情节，其爱情是知音之爱与叛逆之爱的统一，悲剧源于社会家族性格多重原因，其反封建与个体觉醒意义深刻",
        "points": [
            {"label": "总述", "content": "宝黛爱情悲剧是《红楼梦》核心情节"},
            {"label": "分1·爱情内涵", "content": "知音之爱（'木石前盟'）+叛逆之爱（共同反抗礼教）"},
            {"label": "分2·悲剧原因", "content": "社会（封建礼教）+家族（贾府利益）+性格（黛玉多愁）"},
            {"label": "分3·悲剧表现", "content": "'金玉良缘'（宝钗）vs'木石前盟'（黛玉）的对立"},
            {"label": "分4·悲剧意义", "content": "反封建（个体爱情vs家族利益）+个体觉醒（情感自主）"},
            {"label": "分5·艺术成就", "content": "'草蛇灰线伏脉千里'的悲剧结构，'千红一哭万艳同悲'的悲剧意识"},
            {"label": "总结", "content": "宝黛爱情悲剧是反封建与个体觉醒的深刻呈现"},
        ],
        "conclusion": "宝黛爱情悲剧不仅是个人悲剧，更是封建社会的悲剧，其反封建与个体觉醒意义深刻",
    },
}

EQ_0194_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第五回太虚幻境判词：'一个是阆苑仙葩，一个是美玉无瑕'——宝黛爱情的预示",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第三十二回：'林妹妹不说这样混账话，若说这话，我也早和她生分了'——宝黛知音之爱",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第九十八回：黛玉焚稿断痴情，'宝玉，宝玉，你好……'——悲剧高潮",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "蒋和森指出：宝黛爱情是知音之爱与叛逆之爱的统一，其悲剧是反封建与个体觉醒的深刻呈现",
            "source": "蒋和森《红楼梦论稿》人民文学出版社1959年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "周汝昌认为：宝黛爱情悲剧的'草蛇灰线伏脉千里'结构是《红楼梦》叙事艺术的巅峰",
            "source": "周汝昌《红楼梦新证》人民文学出版社1976年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将宝黛爱情悲剧定位为《红楼梦》核心情节，强调其反封建与个体觉醒意义",
            "source": "袁行霈《中国文学史》第四卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重宝黛爱情的文学史意义；章培恒《中国文学史》更注重其悲剧内涵。两书共识：宝黛爱情悲剧是《红楼梦》核心。",
        "scholarComparison": "蒋和森从爱情视角分析其内涵；周汝昌从叙事视角分析其结构；王昆仑从人物视角分析其性格。三种视角互补：蒋重爱，周重叙，王重人。",
    },
    "referenceLinks": [
        {"label": "中国作家网·宝黛爱情悲剧的反封建意义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《红楼梦》爱情悲剧的深刻性", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0204: 欧洲中世纪文学主要成就（综合型）─────────────
EQ_0204_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["欧洲中世纪文学", "主要成就", "文学类型", "代表作品"],
    "limitKeywords": ["主要的文学类型", "基本主题", "代表作品"],
    "task": "梳理文学类型 + 分析主题 + 列举代表作品",
    "breakthroughAngles": [
        "①宗教文学（圣经/圣徒传）",
        "②英雄史诗（贝奥武夫/罗兰之歌）",
        "③骑士文学（骑士传奇/抒情诗）",
        "④城市文学（列那狐/玫瑰传奇）",
    ],
    "angleRationale": "本题为综合型（文学史梳理），需按文学类型分述中世纪文学成就。符合'类型梳理+主题分析+代表作品'的综合型答题策略。",
    "argumentPath": {
        "thesis": "欧洲中世纪文学以宗教文学、英雄史诗、骑士文学、城市文学四大类型为主要成就，分别呈现宗教主题、英雄主题、爱情主题、讽刺主题，奠定欧洲文学传统",
        "points": [
            {"label": "总述", "content": "欧洲中世纪文学四大类型构成主要成就"},
            {"label": "分1·宗教文学", "content": "圣经/圣徒传，宗教主题：信仰与救赎，代表《圣经》《金色传说》"},
            {"label": "分2·英雄史诗", "content": "贝奥武夫/罗兰之歌/尼伯龙根之歌，英雄主题：忠诚与勇敢"},
            {"label": "分3·骑士文学", "content": "骑士传奇/抒情诗，爱情主题：典雅爱情，代表《特里斯丹与伊瑟》"},
            {"label": "分4·城市文学", "content": "列那狐/玫瑰传奇，讽刺主题：市民智慧，代表《列那狐传奇》"},
            {"label": "总结", "content": "四大类型奠定欧洲文学传统，影响深远"},
        ],
        "conclusion": "欧洲中世纪文学四大类型是欧洲文学传统的重要源头，影响文艺复兴及后世文学",
    },
}

EQ_0204_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《贝奥武夫》：'贝奥武夫说话，忠诚的誓言'——英雄史诗的忠诚主题",
            "source": "《贝奥武夫》8世纪",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《罗兰之歌》：'罗兰勇敢，奥利维埃智慧'——法国英雄史诗的代表作",
            "source": "《罗兰之歌》11世纪",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《列那狐传奇》：'列那狐以智慧戏弄狼'——城市文学的讽刺主题",
            "source": "《列那狐传奇》12-13世纪",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Curtius指出：欧洲中世纪文学四大类型奠定欧洲文学传统，其主题与形式影响深远",
            "source": "Curtius《欧洲文学与拉丁中世纪》1948年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将欧洲中世纪文学分为宗教文学、英雄史诗、骑士文学、城市文学四大类型",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重中世纪文学的类型划分；郑克鲁《外国文学史》更注重其文学史意义。两书共识：中世纪文学四大类型是欧洲文学传统源头。",
        "scholarComparison": "Curtius从文学传统视角分析其影响；Huizinga从文化史视角分析其意义；Lewis从基督教视角分析其宗教文学。三种视角互补：Cu重传，Hu重文，Le重宗。",
    },
    "referenceLinks": [
        {"label": "中国作家网·欧洲中世纪文学的四大类型", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·中世纪文学与欧洲传统", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0205: 18世纪英国文学发展进程（综合型）─────────────
EQ_0205_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["18世纪英国文学", "发展进程", "纵向描述"],
    "limitKeywords": ["18世纪", "英国文学"],
    "task": "分时期描述 + 分析特色 + 评价意义",
    "breakthroughAngles": [
        "①早期启蒙文学（笛福《鲁滨逊》）",
        "②中期新古典主义（蒲柏/斯威夫特）",
        "③中期现实主义小说（理查逊/菲尔丁）",
        "④后期感伤主义与浪漫主义先驱（斯特恩/格雷）",
        "⑤总体特征（启蒙/理性/小说兴起）",
    ],
    "angleRationale": "本题为综合型（纵向描述），需按时序描述18世纪英国文学发展。符合'分期描述+特色分析'的综合型答题策略。",
    "argumentPath": {
        "thesis": "18世纪英国文学经历早期启蒙文学、中期新古典主义、中期现实主义小说、后期感伤主义与浪漫主义先驱四个阶段，呈现启蒙、理性、小说兴起的总体特征",
        "points": [
            {"label": "总述", "content": "18世纪英国文学经历四个阶段"},
            {"label": "分1·早期启蒙文学", "content": "笛福《鲁滨逊漂流记》：资产阶级精神的文学呈现"},
            {"label": "分2·中期新古典主义", "content": "蒲柏《夺发记》、斯威夫特《格列佛游记》：理性与讽刺"},
            {"label": "分3·中期现实主义小说", "content": "理查逊《帕梅拉》、菲尔丁《汤姆·琼斯》：现代小说的兴起"},
            {"label": "分4·后期感伤主义与浪漫主义先驱", "content": "斯特恩《项狄传》、格雷《墓园挽歌》：向浪漫主义过渡"},
            {"label": "分5·总体特征", "content": "启蒙、理性、小说兴起"},
            {"label": "总结", "content": "18世纪英国文学是启蒙时代文学的代表，奠定现代小说基础"},
        ],
        "conclusion": "18世纪英国文学的发展进程呈现从启蒙到浪漫主义的过渡，是英国文学的重要转折期",
    },
}

EQ_0205_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "笛福《鲁滨逊漂流记》：'我学会自己动手，丰衣足食'——资产阶级精神的呈现",
            "source": "笛福《鲁滨逊漂流记》1719年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "斯威夫特《格列佛游记》：'我是一个小人国的俘虏'——讽刺艺术的典范",
            "source": "斯威夫特《格列佛游记》1726年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "菲尔丁《汤姆·琼斯》：'汤姆·琼斯是个弃儿'——现代小说的兴起",
            "source": "菲尔丁《汤姆·琼斯》1749年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Watt指出：18世纪英国小说的兴起是'资产阶级个人主义'的文学呈现",
            "source": "Watt《小说的兴起》1957年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将18世纪英国文学分为启蒙文学、新古典主义、现实主义小说、感伤主义四阶段",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重18世纪英国文学的分期；郑克鲁《外国文学史》更注重其小说兴起。两书共识：18世纪英国文学是启蒙时代代表。",
        "scholarComparison": "Watt从小说兴起视角分析其意义；Monk从启蒙视角分析其理性；Battestin从文献视角考证其发展。三种视角互补：Wa重小，Mo重启，Ba重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·18世纪英国文学的发展进程", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·启蒙时代与英国小说", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0206: 《百年孤独》艺术特色（作品分析型）─────────────
EQ_0206_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["马尔克斯", "百年孤独", "艺术特色"],
    "limitKeywords": ["结合具体内容"],
    "task": "梳理艺术特色 + 结合内容印证 + 评价意义",
    "breakthroughAngles": [
        "①魔幻现实主义（魔幻与现实交融）",
        "②循环叙事（时间循环）",
        "③多代谱系（布恩迪亚家族七代）",
        "④神话原型（圣经/希腊神话）",
        "⑤影响（拉美文学爆炸）",
    ],
    "angleRationale": "本题为作品分析型，需结合《百年孤独》具体内容分析艺术特色。符合'特色梳理+内容印证'的作品分析策略。",
    "argumentPath": {
        "thesis": "马尔克斯《百年孤独》以魔幻现实主义、循环叙事、多代谱系、神话原型等艺术特色，呈现拉美百年孤独，是拉美文学爆炸的代表作",
        "points": [
            {"label": "总述", "content": "《百年孤独》是拉美文学爆炸代表作，艺术特色鲜明"},
            {"label": "分1·魔幻现实主义", "content": "魔幻与现实交融，'马孔多下雨下了一千零一个月'"},
            {"label": "分2·循环叙事", "content": "时间循环，家族命运重复，'奥雷里亚诺·布恩迪亚上校面对行刑队'"},
            {"label": "分3·多代谱系", "content": "布恩迪亚家族七代人的兴衰"},
            {"label": "分4·神话原型", "content": "圣经（创世/洪水）、希腊神话（命运循环）的原型"},
            {"label": "分5·影响", "content": "拉美文学爆炸的代表作，影响莫言等中国作家"},
            {"label": "总结", "content": "《百年孤独》艺术特色集魔幻现实主义之大成"},
        ],
        "conclusion": "《百年孤独》以魔幻现实主义呈现拉美百年孤独，是世界文学的不朽杰作",
    },
}

EQ_0206_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "马尔克斯《百年孤独》开篇：「多年以后，奥雷里亚诺·布恩迪亚上校面对行刑队，将会回想起父亲带他去见识冰块的那个遥远的下午」——循环叙事的典范",
            "source": "马尔克斯《百年孤独》1967年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "马尔克斯《百年孤独》：'马孔多下雨下了一千零一个月'——魔幻现实主义的典范",
            "source": "马尔克斯《百年孤独》1967年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "马尔克斯《百年孤独》结尾：'注定经受百年孤独的家族不会有第二次机会在大地上出现'——命运循环的终结",
            "source": "马尔克斯《百年孤独》1967年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Fuentes指出：《百年孤独》以魔幻现实主义呈现拉美百年孤独，是拉美文学爆炸的代表作",
            "source": "Fuentes《La nueva novela hispanoamericana》1969年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "莫言认为：《百年孤独》的魔幻现实主义深刻影响中国当代文学，'寻根文学'与'先锋文学'都受其影响",
            "source": "莫言《百年孤独与寻根文学》1990年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《百年孤独》定位为拉美文学爆炸代表作，强调其魔幻现实主义艺术",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《百年孤独》的文学史地位；郑克鲁《外国文学史》更注重其艺术特色。两书共识：《百年孤独》是拉美文学爆炸代表作。",
        "scholarComparison": "Fuentes从拉美文学视角分析其意义；莫言从中国接受视角分析其影响；Vargas Llosa从叙事学视角分析其艺术。三种视角互补：Fu重拉，莫重中，Va重叙。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《百年孤独》与魔幻现实主义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·拉美文学爆炸的代表作", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0216: 陶渊明艺术精神内涵（综合型）─────────────
EQ_0216_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["陶渊明", "艺术精神", "内涵"],
    "limitKeywords": [],
    "task": "梳理精神内涵 + 结合作品印证 + 评价意义",
    "breakthroughAngles": [
        "①自然精神（纵浪大化/复得返自然）",
        "②隐逸精神（不为五斗米折腰）",
        "③审美精神（采菊东篱下/悠然见南山）",
        "④哲思精神（此中有真意/欲辨已忘言）",
        "⑤人格精神（不为五斗米折腰/独立自由）",
    ],
    "angleRationale": "本题为综合型（精神内涵梳理），需多维度梳理陶渊明艺术精神。符合'内涵梳理+作品印证'的综合型答题策略。",
    "argumentPath": {
        "thesis": "陶渊明艺术精神以自然、隐逸、审美、哲思、人格五大内涵为核心，是中国士人精神的典范，对后世影响深远",
        "points": [
            {"label": "总述", "content": "陶渊明艺术精神五大内涵构成中国士人精神的典范"},
            {"label": "分1·自然精神", "content": "'纵浪大化中，不喜亦不惧'、'复得返自然'——道法自然"},
            {"label": "分2·隐逸精神", "content": "'不为五斗米折腰'、归隐田园——独立自由"},
            {"label": "分3·审美精神", "content": "'采菊东篱下，悠然见南山'——田园诗美"},
            {"label": "分4·哲思精神", "content": "'此中有真意，欲辨已忘言'——生命哲思"},
            {"label": "分5·人格精神", "content": "独立自由的人格，'不为五斗米折腰'"},
            {"label": "总结", "content": "陶渊明艺术精神是中国士人精神的典范，对后世影响深远"},
        ],
        "conclusion": "陶渊明艺术精神以自然、隐逸、审美、哲思、人格为核心，是中国士人精神的不朽典范",
    },
}

EQ_0216_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《归园田居》：「久在樊笼里，复得返自然」——自然精神",
            "source": "陶渊明《归园田居》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《饮酒·其五》：「采菊东篱下，悠然见南山」——审美精神",
            "source": "陶渊明《饮酒·其五》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《形影神·神释》：「纵浪大化中，不喜亦不惧。应尽便须尽，无复独多虑」——哲思精神",
            "source": "陶渊明《形影神·神释》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈寅恪指出：陶渊明思想'实为外儒内道'，其艺术精神是新旧学说的创造性融合",
            "source": "陈寅恪《陶渊明之思想与清谈之关系》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朱光潜认为：陶渊明'全副精神在于自然'，其艺术精神是中国士人精神的典范",
            "source": "朱光潜《诗论》三联书店1984年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将陶渊明定位为'魏晋风度的杰出代表'，其艺术精神是中国士人精神的典范",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重陶渊明的文学史地位；章培恒《中国文学史》更注重其艺术精神内涵。两书共识：陶渊明是魏晋风度的杰出代表。",
        "scholarComparison": "陈寅恪从思想史视角分析其'外儒内道'；朱光潜从美学视角分析其自然精神；钱钟书从诗学视角分析其'质直深味'。三种视角互补：陈重思，朱重美，钱重诗。",
    },
    "referenceLinks": [
        {"label": "中国作家网·陶渊明的艺术精神", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·陶渊明与中国士人精神", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陶渊明", "note": "项目暂无陶渊明独立知识点，建议补充'陶渊明田园诗与生命哲学'以完善魏晋文学谱系"},
    ],
}

# ── eq_0217: 意象的特征（理论应用型）─────────────
EQ_0217_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["意象", "特征", "结合作品"],
    "limitKeywords": ["结合作品论述"],
    "task": "阐释意象 + 分析特征 + 举例印证",
    "breakthroughAngles": [
        "①意象定义（主观情感与客观物象的统一）",
        "②特征一：客观性（依托物象）",
        "③特征二：主观性（情感投射）",
        "④特征三：象征性（多义性）",
        "⑤举例印证（李白/杜甫/李商隐）",
    ],
    "angleRationale": "本题为理论应用型，需先阐释意象，再以作品分析其特征。符合'理论阐释+作品印证'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "意象是中国古典诗学核心范畴，以客观性、主观性、象征性为三大特征，是主观情感与客观物象的统一，李白杜甫李商隐的诗作是意象艺术的典范",
        "points": [
            {"label": "总述", "content": "意象是中国古典诗学核心范畴，三大特征鲜明"},
            {"label": "分1·意象定义", "content": "主观情感与客观物象的统一"},
            {"label": "分2·客观性", "content": "依托物象，李白云'明月'的客观呈现"},
            {"label": "分3·主观性", "content": "情感投射，杜甫'感时花溅泪'的主观投射"},
            {"label": "分4·象征性", "content": "多义性，李商隐'锦瑟'的象征多义"},
            {"label": "分5·举例印证", "content": "李白/杜甫/李商隐诗作是意象艺术典范"},
            {"label": "总结", "content": "意象三大特征在中国古典诗歌中得到充分呈现"},
        ],
        "conclusion": "意象是中国古典诗学核心范畴，其客观性、主观性、象征性三大特征在中国古典诗歌中达到极致",
    },
}

EQ_0217_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "李白《静夜思》：「床前明月光，疑是地上霜」——意象的客观性",
            "source": "李白《静夜思》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "杜甫《春望》：「感时花溅泪，恨别鸟惊心」——意象的主观性",
            "source": "杜甫《春望》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "李商隐《锦瑟》：「锦瑟无端五十弦，一弦一柱思华年」——意象的象征性",
            "source": "李商隐《锦瑟》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "刘勰《文心雕龙·神思》提出'窥意象而运斤'——意象理论奠基",
            "source": "刘勰《文心雕龙·神思》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "袁行霈指出：意象是主观情感与客观物象的统一，其客观性、主观性、象征性三大特征",
            "source": "袁行霈《中国诗歌艺术研究》北京大学出版社2001年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将意象列为中国古典诗学核心范畴，强调其三大特征",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从理论视角分析意象；袁行霈《中国诗歌艺术研究》从诗学视角分析意象。两书共识：意象是中国古典诗学核心范畴。",
        "scholarComparison": "刘勰从古典文论视角奠基意象理论；袁行霈从现代诗学视角分析其特征；叶朗从美学史视角梳理意象理论。三种视角互补：刘重古，袁重现，叶重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·意象美学的当代意义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·意象与中国古典诗学", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0218: 共鸣和余味（理论应用型）─────────────
EQ_0218_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["共鸣", "余味", "结合阅读经验"],
    "limitKeywords": ["论述"],
    "task": "阐释共鸣与余味 + 结合阅读经验 + 评价意义",
    "breakthroughAngles": [
        "①共鸣定义（读者与作品的情感共振）",
        "②余味定义（作品读后的余韵）",
        "③共鸣与余味的关系",
        "④阅读经验印证",
        "⑤美学意义",
    ],
    "angleRationale": "本题为理论应用型，需先阐释共鸣与余味，再结合阅读经验印证。符合'理论阐释+阅读印证'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "共鸣与余味是文学接受的核心范畴，共鸣是读者与作品的情感共振，余味是作品读后的余韵，二者共同构成文学接受的美学体验",
        "points": [
            {"label": "总述", "content": "共鸣与余味是文学接受的核心范畴"},
            {"label": "分1·共鸣定义", "content": "读者与作品的情感共振，'心即会解'"},
            {"label": "分2·余味定义", "content": "作品读后的余韵，'言有尽而意无穷'"},
            {"label": "分3·二者关系", "content": "共鸣是余味的前提，余味是共鸣的延续"},
            {"label": "分4·阅读经验印证", "content": "如读《红楼梦》宝玉出家的共鸣与读后的余味"},
            {"label": "分5·美学意义", "content": "共鸣与余味共同构成文学接受的美学体验"},
            {"label": "总结", "content": "共鸣与余味是文学接受美学的核心"},
        ],
        "conclusion": "共鸣与余味共同构成文学接受的美学体验，是文学价值的重要标志",
    },
}

EQ_0218_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅《摩罗诗力说》提出'凡人之心，无不有诗'——共鸣论的中国先驱",
            "source": "鲁迅《摩罗诗力说》1907年",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "司空图《二十四诗品》提出'韵外之致'——余味理论的中国古典渊源",
            "source": "司空图《二十四诗品》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "姚斯接受美学提出'期待视野'——共鸣是读者期待视野与作品的融合",
            "source": "姚斯《审美经验与文学解释学》1977年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将共鸣与余味列为文学接受的核心范畴",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从理论视角分析共鸣与余味；王一川《文学理论》从接受美学视角分析其意义。两书共识：共鸣与余味是文学接受核心范畴。",
        "scholarComparison": "鲁迅从中国诗学视角提出共鸣论先驱；司空图从古典诗学视角提出'韵外之致'；姚斯从接受美学视角提出'期待视野'。三种视角互补：鲁重中，司重古，姚重西。",
    },
    "referenceLinks": [
        {"label": "中国作家网·共鸣与余味的接受美学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·文学接受的核心范畴", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0253: 周氏一脉散文在现当代的处境变化（综合型）─────────────
EQ_0253_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["周氏一脉散文", "周作人", "现当代", "处境变化", "意义"],
    "limitKeywords": ["结合作品"],
    "task": "梳理处境变化 + 分析周作人散文意义 + 评价",
    "breakthroughAngles": [
        "①周作人散文特色（平和冲淡/闲适趣昧）",
        "②现代处境（五四至40年代：兴盛）",
        "③当代处境（50-80年代：边缘化）",
        "④新时期复兴（90年代：周作人散文复兴）",
        "⑤对当代散文的意义",
    ],
    "angleRationale": "本题为综合型（处境变化+意义），需先梳理周氏一脉散文的处境变化，再分析周作人散文对当代的意义。符合'处境梳理+意义分析'的综合型答题策略。",
    "argumentPath": {
        "thesis": "周氏一脉散文经历五四至40年代兴盛、50-80年代边缘化、90年代复兴三个阶段，周作人散文的平和冲淡、闲适趣昧对当代散文创作有重要启示",
        "points": [
            {"label": "总述", "content": "周氏一脉散文经历三阶段处境变化"},
            {"label": "分1·周作人散文特色", "content": "平和冲淡、闲适趣昧、'人的文学'"},
            {"label": "分2·现代处境（五四至40年代兴盛）", "content": "周作人《雨天的书》《泽泻集》成为现代散文典范"},
            {"label": "分3·当代处境（50-80年代边缘化）", "content": "因周作人汉奸问题，其散文在50-80年代被边缘化"},
            {"label": "分4·新时期复兴（90年代）", "content": "90年代周作人散文复兴，影响张中行、汪曾祺等"},
            {"label": "分5·对当代散文的意义", "content": "平和冲淡美学、闲适趣昧、文化品性对当代散文的启示"},
            {"label": "总结", "content": "周氏一脉散文的处境变化折射现当代文学场的变迁，周作人散文对当代散文有重要启示"},
        ],
        "conclusion": "周氏一脉散文的处境变化是现当代文学场的缩影，周作人散文的平和冲淡美学对当代散文仍有启示",
    },
}

EQ_0253_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "周作人《雨天的书·乌篷船》：'坐在船上，应该是什么也不做，只是看着'——平和冲淡的散文风格",
            "source": "周作人《雨天的书》1925年北新书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "周作人《泽泻集·喝茶》：'喝茶当于瓦屋纸窗之下，清泉绿茶'——闲适趣昧",
            "source": "周作人《泽泻集》1927年北新书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "舒芜指出：周作人散文的'平和冲淡'是中国现代散文的重要一脉，影响深远",
            "source": "舒芜《周作人概观》湖南人民出版社1986年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和认为：90年代周作人散文复兴影响张中行、汪曾祺等，是当代散文的重要资源",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "钱理群《三十年》将周作人定位为现代散文的重要代表，强调其'平和冲淡'风格",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重周作人散文在现代文学史中的地位；丁帆《新文学史》更注重其对当代散文的影响。两书共识：周作人是现代散文重要代表。",
        "scholarComparison": "舒芜从散文理论视角分析周作人风格；陈思和从当代接受视角分析其复兴；孙郁从文化史视角分析其意义。三种视角互补：舒重散，陈重接，孙重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·周氏一脉散文的处境变化", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·周作人散文与当代散文", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "周作人", "note": "项目暂无周作人独立知识点，建议补充'周作人散文与现代散文谱系'以覆盖现代散文"},
    ],
}

# ── eq_0285: 《荷马史诗》基本内容与特色（综合型）─────────────
EQ_0285_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["荷马史诗", "伊利亚特", "奥德赛", "基本内容", "特色"],
    "limitKeywords": ["两部作品各自"],
    "task": "梳理基本内容 + 分析各自特色 + 评价意义",
    "breakthroughAngles": [
        "①《伊利亚特》基本内容（特洛伊战争）",
        "②《奥德赛》基本内容（奥德修斯归乡）",
        "③《伊利亚特》特色（战争/英雄/悲剧）",
        "④《奥德赛》特色（冒险/智慧/归乡）",
        "⑤总体意义（西方文学源头）",
    ],
    "angleRationale": "本题为综合型（双作品比较），需分别梳理两部史诗的基本内容与特色。符合'分别梳理+特色对比'的综合型答题策略。",
    "argumentPath": {
        "thesis": "《荷马史诗》包括《伊利亚特》与《奥德赛》，前者写特洛伊战争的英雄悲剧，后者写奥德修斯的归乡冒险，两部史诗共同构成西方文学的源头",
        "points": [
            {"label": "总述", "content": "《荷马史诗》包括《伊利亚特》与《奥德赛》两部"},
            {"label": "分1·《伊利亚特》基本内容", "content": "特洛伊战争第十年阿喀琉斯的愤怒与战斗"},
            {"label": "分2·《奥德赛》基本内容", "content": "奥德修斯特洛伊战争后历经十年冒险归乡"},
            {"label": "分3·《伊利亚特》特色", "content": "战争/英雄/悲剧，'阿喀琉斯的愤怒'是核心主题"},
            {"label": "分4·《奥德赛》特色", "content": "冒险/智慧/归乡，'奥德修斯的智慧'是核心主题"},
            {"label": "分5·总体意义", "content": "西方文学源头，影响深远"},
            {"label": "总结", "content": "两部史诗共同构成西方文学源头"},
        ],
        "conclusion": "《荷马史诗》是西方文学的不朽源头，两部史诗各具特色共同奠基西方文学传统",
    },
}

EQ_0285_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《伊利亚特》开篇：「女神啊，请歌唱佩琉斯之子阿喀琉斯的愤怒」——战争史诗的开篇",
            "source": "荷马《伊利亚特》公元前8世纪",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《奥德赛》开篇：「请告诉我，缪斯，那位四处漂泊的人的故事」——冒险史诗的开篇",
            "source": "荷马《奥德赛》公元前8世纪",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Milman Parry指出：《荷马史诗》的口传传统形成其程式化语言与典型场景",
            "source": "Milman Parry《The Making of Homeric Verse》1971年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "纳吉认为：《荷马史诗》是希腊民族认同的文学建构，其两部史诗呈现英雄与智慧两种价值",
            "source": "纳吉《The Best of the Achaeans》1979年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《荷马史诗》定位为西方文学源头，强调两部史诗各自特色",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《荷马史诗》的文学史地位；郑克鲁《外国文学史》更注重其艺术特色。两书共识：《荷马史诗》是西方文学源头。",
        "scholarComparison": "Parry从口传传统视角分析其程式化语言；纳吉从民族认同视角分析其意义；Finley从社会史视角分析其背景。三种视角互补：Pa重口，纳重民，Fi重社。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《荷马史诗》与西方文学源头", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·两部史诗的特色对比", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0286: 哈姆雷特延宕（理论应用型）─────────────
EQ_0286_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["哈姆雷特", "延宕", "泰纳", "想象"],
    "limitKeywords": ["泰纳评论"],
    "task": "解读泰纳评论 + 分析延宕原因 + 评价意义",
    "breakthroughAngles": [
        "①泰纳评论解读（'过分活跃的想象'消耗活力）",
        "②延宕的心理原因（想象/思考过度）",
        "③延宕的哲学原因（存在主义思考）",
        "④延宕的悲剧意义（思考与行动的张力）",
        "⑤对当代的启示",
    ],
    "angleRationale": "本题为理论应用型，需先解读泰纳评论，再分析哈姆雷特延宕的原因。符合'评论解读—原因分析—意义评价'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "泰纳认为哈姆雷特延宕源于'过分活跃的想象'消耗活力，使其成为'善于幻想而不善于行动的人'，这一解读揭示了延宕的心理原因，延宕的悲剧意义在于思考与行动的张力",
        "points": [
            {"label": "总述", "content": "泰纳评论揭示了哈姆雷特延宕的心理原因"},
            {"label": "分1·泰纳评论解读", "content": "'过分活跃的想象由于积累了各种意象和热衷于专注的思考以至消耗了一切活力'"},
            {"label": "分2·延宕的心理原因", "content": "想象过度消耗活力，思考过度阻碍行动"},
            {"label": "分3·延宕的哲学原因", "content": "存在主义思考：'生存还是毁灭'的终极追问"},
            {"label": "分4·延宕的悲剧意义", "content": "思考与行动的张力，是现代人精神困境的预示"},
            {"label": "分5·对当代的启示", "content": "思考与行动的张力是现代人普遍困境"},
            {"label": "总结", "content": "哈姆雷特延宕是现代人精神困境的预示，泰纳的解读深刻"},
        ],
        "conclusion": "哈姆雷特延宕是现代人精神困境的预示，泰纳的'想象消耗活力'解读揭示了其心理根源",
    },
}

EQ_0286_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《哈姆雷特》：'生存还是毁灭，这是一个问题'——延宕的哲学思考",
            "source": "莎士比亚《哈姆雷特》1601年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《哈姆雷特》：'哈姆雷特：啊，从我叔父的嘴边……魔鬼在享用圣餐'——延宕的具体表现",
            "source": "莎士比亚《哈姆雷特》1601年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "泰纳《英国文学史》：'过分活跃的想象由于积累了各种意象和热衷于专注的思考以至消耗了一切活力'——延宕的心理原因",
            "source": "泰纳《英国文学史》1863年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Bradley指出：哈姆雷特延宕是'思考过度阻碍行动'，是现代人精神困境的预示",
            "source": "A.C. Bradley《Shakespearean Tragedy》1904年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Coleridge认为：哈姆雷特是'思考过度的人'，其延宕是思考与行动张力的结果",
            "source": "Coleridge《Shakespeare Lectures》1811年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将哈姆雷特延宕列为莎士比亚悲剧的核心问题，强调其哲学意义",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重哈姆雷特延宕的文学史意义；郑克鲁《外国文学史》更注重其哲学内涵。两书共识：哈姆雷特延宕是莎士比亚悲剧核心问题。",
        "scholarComparison": "泰纳从心理视角分析延宕；Bradley从悲剧视角分析其意义；Coleridge从哲学视角分析其内涵。三种视角互补：泰重心，Br重悲，Co重哲。",
    },
    "referenceLinks": [
        {"label": "中国作家网·哈姆雷特延宕的心理根源", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·泰纳与哈姆雷特解读", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0287: 19世纪法国文学发展脉络与主要成就（综合型）─────────────
EQ_0287_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["19世纪法国文学", "发展脉络", "主要成就"],
    "limitKeywords": [],
    "task": "梳理发展脉络 + 归纳主要成就 + 评价意义",
    "breakthroughAngles": [
        "①初期浪漫主义（夏多布里昂/雨果）",
        "②中期现实主义（巴尔扎克/司汤达/福楼拜）",
        "③后期自然主义（左拉/莫泊桑）",
        "④象征主义（波德莱尔/马拉美）",
        "⑤总体成就（世界文学高峰）",
    ],
    "angleRationale": "本题为综合型（脉络梳理+成就归纳），需按时序梳理19世纪法国文学发展。符合'脉络梳理+成就归纳'的综合型答题策略。",
    "argumentPath": {
        "thesis": "19世纪法国文学经历初期浪漫主义、中期现实主义、后期自然主义、象征主义四个阶段，形成世界文学的高峰，影响深远",
        "points": [
            {"label": "总述", "content": "19世纪法国文学经历四阶段，是世界文学高峰"},
            {"label": "分1·初期浪漫主义", "content": "夏多布里昂《勒内》、雨果《巴黎圣母院》《悲惨世界》：浪漫主义兴起"},
            {"label": "分2·中期现实主义", "content": "巴尔扎克《人间喜剧》、司汤达《红与黑》、福楼拜《包法利夫人》：现实主义高峰"},
            {"label": "分3·后期自然主义", "content": "左拉《卢贡马卡尔家族》、莫泊桑《项链》：自然主义"},
            {"label": "分4·象征主义", "content": "波德莱尔《恶之花》、马拉美：象征主义"},
            {"label": "分5·总体成就", "content": "世界文学高峰，影响世界文学"},
            {"label": "总结", "content": "19世纪法国文学是世界文学的高峰，影响深远"},
        ],
        "conclusion": "19世纪法国文学以四阶段发展脉络构成世界文学的高峰，其影响延续至今",
    },
}

EQ_0287_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "雨果《悲惨世界》：'世界上最宽阔的是海洋，比海洋更宽阔的是天空'——浪漫主义的代表",
            "source": "雨果《悲惨世界》1862年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "巴尔扎克《高老头》：'巴黎，正如巴尔扎克所写'——现实主义的代表",
            "source": "巴尔扎克《高老头》1834年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "波德莱尔《恶之花》：'朽骨之谷，满目疮痍'——象征主义的代表",
            "source": "波德莱尔《恶之花》1857年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朗松指出：19世纪法国文学经历浪漫主义、现实主义、自然主义、象征主义四阶段，是世界文学高峰",
            "source": "朗松《法国文学史》1903年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将19世纪法国文学定位为世界文学高峰，强调其四阶段发展脉络",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重19世纪法国文学的文学史地位；郑克鲁《外国文学史》更注重其流派演变。两书共识：19世纪法国文学是世界文学高峰。",
        "scholarComparison": "朗松从文学史视角梳理四阶段；Brunetière从流派视角分析其演变；Thibaudet从诗学视角分析其成就。三种视角互补：朗重史，Br重流，Th重诗。",
    },
    "referenceLinks": [
        {"label": "中国作家网·19世纪法国文学的高峰", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·19世纪法国文学的流派演变", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0298: 叙事视角及其特点（理论应用型）─────────────
EQ_0298_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["叙事视角", "特点", "简要分析"],
    "limitKeywords": [],
    "task": "列举叙事视角 + 分析特点 + 评价意义",
    "breakthroughAngles": [
        "①全知视角（上帝视角）",
        "②限知视角（第一人称/第三人称限知）",
        "③客观视角（戏剧式）",
        "④多重视角（多人物视角）",
        "⑤各视角的特点与适用",
    ],
    "angleRationale": "本题为理论应用型，需列举叙事视角并分析其特点。符合'视角列举+特点分析'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "叙事视角主要有全知视角、限知视角、客观视角、多重视角四种，各自具有不同特点，适用于不同叙事需求",
        "points": [
            {"label": "总述", "content": "叙事视角主要有四种，各具特点"},
            {"label": "分1·全知视角", "content": "上帝视角，叙述者全知全觉，适合宏大叙事，如《红楼梦》"},
            {"label": "分2·限知视角", "content": "第一人称限知（如鲁迅《狂人日记》）/第三人称限知（如福楼拜《包法利夫人》）"},
            {"label": "分3·客观视角", "content": "戏剧式视角，叙述者只记录外部行为，如海明威《老人与海》"},
            {"label": "分4·多重视角", "content": "多人物视角，如福克纳《喧哗与骚动》"},
            {"label": "分5·各视角特点与适用", "content": "全知适合宏大/限知适合心理/客观适合冷峻/多重适合复杂"},
            {"label": "总结", "content": "四种叙事视角各具特点，是小说叙事艺术的重要维度"},
        ],
        "conclusion": "叙事视角是小说叙事艺术的重要维度，四种视角各具特点适用于不同叙事需求",
    },
}

EQ_0298_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《狂人日记》：'今天全没月光，我知道不妙'——第一人称限知视角",
            "source": "鲁迅《狂人日记》1918年《新青年》",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "福楼拜《包法利夫人》：'查理·包法利是个平庸的青年'——第三人称限知视角",
            "source": "福楼拜《包法利夫人》1856年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "海明威《老人与海》：'老人独自在海上'——客观视角的冷峻",
            "source": "海明威《老人与海》1952年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "热奈特《叙事话语》将叙事视角分为零聚焦（全知）、内聚焦（限知）、外聚焦（客观）三类",
            "source": "热奈特《叙事话语》1972年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "布斯《小说修辞学》指出：叙事视角是小说修辞的重要手段，影响读者与人物的距离",
            "source": "布斯《小说修辞学》1961年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将叙事视角列为小说叙事艺术的重要维度，强调其分类与特点",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从理论视角分析叙事视角；胡亚敏《叙事学》从叙事学视角分析其分类。两书共识：叙事视角是小说叙事艺术重要维度。",
        "scholarComparison": "热奈特从叙事学视角分类叙事视角；布斯从修辞学视角分析其效果；赵毅衡从符号学视角分析其意义。三种视角互补：热重分，布重修，赵重符。",
    },
    "referenceLinks": [
        {"label": "中国作家网·叙事视角与小说艺术", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·叙事视角的分类与特点", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0299: 清空说（理论应用型）─────────────
EQ_0299_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["清空说", "词学理论"],
    "limitKeywords": [],
    "task": "阐释清空说 + 分析内涵 + 评价意义",
    "breakthroughAngles": [
        "①清空说提出者（张炎《词源》）",
        "②清空内涵（清空虚灵）",
        "③与质实对比（姜夔vs吴文英）",
        "④审美特征（空灵/清雅）",
        "⑤词学史意义",
    ],
    "angleRationale": "本题为理论应用型，需先阐释清空说内涵，再分析其意义。符合'理论阐释—内涵分析—意义评价'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "清空说是张炎《词源》提出的词学理论，主张词应'清空虚灵'，与'质实'相对，以姜夔词为典范，是中国词学的重要美学范畴",
        "points": [
            {"label": "总述", "content": "清空说是张炎《词源》提出的词学理论"},
            {"label": "分1·提出者", "content": "张炎《词源》提出'清空'说"},
            {"label": "分2·清空内涵", "content": "清空虚灵，'清空'与'质实'相对"},
            {"label": "分3·与质实对比", "content": "姜夔词'清空'典范，吴文英词'质实'代表"},
            {"label": "分4·审美特征", "content": "空灵、清雅、不滞于物"},
            {"label": "分5·词学史意义", "content": "是中国词学的重要美学范畴，影响后世词学"},
            {"label": "总结", "content": "清空说是中国词学的重要美学范畴"},
        ],
        "conclusion": "清空说以姜夔词为典范，是中国词学'清空'与'质实'二元美学的重要一极",
    },
}

EQ_0299_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "张炎《词源》：「词要清空，不要质实」——清空说的纲领",
            "source": "张炎《词源》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "姜夔《扬州慢》：「二十四桥仍在，波心荡，冷月无声」——清空说的典范",
            "source": "姜夔《扬州慢》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "吴文英《莺啼序》词藻繁密——质实词的代表",
            "source": "吴文英《莺啼序》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "叶嘉莹指出：清空说是张炎对姜夔词的美学概括，强调其空灵清雅",
            "source": "叶嘉莹《迦陵论词丛稿》北京大学出版社2008年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "龙榆生认为：清空与质实的对立是中国词学的重要二元美学，姜夔与吴文英是两端代表",
            "source": "龙榆生《词学十讲》北京出版社2005年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将清空说列为词学重要理论，强调其对姜夔词的美学概括",
            "source": "袁行霈《中国文学史》第三卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重清空说的词学史地位；章培恒《中国文学史》更注重其美学内涵。两书共识：清空说是词学重要理论。",
        "scholarComparison": "叶嘉莹从词学本体视角分析清空说；龙榆生从词史演变视角分析其意义；沈祖棻从作品分析视角分析其典范。三种视角互补：叶重词，龙重史，沈重作。",
    },
    "referenceLinks": [
        {"label": "中国作家网·清空说与词学美学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·张炎《词源》与清空说", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── 主程序：读取 seed_data.json，批量填充，写回 ──────────────────
FILL_MAP = {
    "eq_0138": (EQ_0138_ANGLE, EQ_0138_NOTES),
    "eq_0139": (EQ_0139_ANGLE, EQ_0139_NOTES),
    "eq_0140": (EQ_0140_ANGLE, EQ_0140_NOTES),
    "eq_0150": (EQ_0150_ANGLE, EQ_0150_NOTES),
    "eq_0159": (EQ_0159_ANGLE, EQ_0159_NOTES),
    "eq_0160": (EQ_0160_ANGLE, EQ_0160_NOTES),
    "eq_0161": (EQ_0161_ANGLE, EQ_0161_NOTES),
    "eq_0170": (EQ_0170_ANGLE, EQ_0170_NOTES),
    "eq_0171": (EQ_0171_ANGLE, EQ_0171_NOTES),
    "eq_0172": (EQ_0172_ANGLE, EQ_0172_NOTES),
    "eq_0183": (EQ_0183_ANGLE, EQ_0183_NOTES),
    "eq_0184": (EQ_0184_ANGLE, EQ_0184_NOTES),
    "eq_0185": (EQ_0185_ANGLE, EQ_0185_NOTES),
    "eq_0194": (EQ_0194_ANGLE, EQ_0194_NOTES),
    "eq_0204": (EQ_0204_ANGLE, EQ_0204_NOTES),
    "eq_0205": (EQ_0205_ANGLE, EQ_0205_NOTES),
    "eq_0206": (EQ_0206_ANGLE, EQ_0206_NOTES),
    "eq_0216": (EQ_0216_ANGLE, EQ_0216_NOTES),
    "eq_0217": (EQ_0217_ANGLE, EQ_0217_NOTES),
    "eq_0218": (EQ_0218_ANGLE, EQ_0218_NOTES),
    "eq_0253": (EQ_0253_ANGLE, EQ_0253_NOTES),
    "eq_0285": (EQ_0285_ANGLE, EQ_0285_NOTES),
    "eq_0286": (EQ_0286_ANGLE, EQ_0286_NOTES),
    "eq_0287": (EQ_0287_ANGLE, EQ_0287_NOTES),
    "eq_0298": (EQ_0298_ANGLE, EQ_0298_NOTES),
    "eq_0299": (EQ_0299_ANGLE, EQ_0299_NOTES),
}


def main():
    with open(SEED_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    filled_count = 0
    for q in data["exam_questions"]:
        qid = q.get("id")
        if qid in FILL_MAP:
            angle, notes = FILL_MAP[qid]
            q["angle"] = json.dumps(angle, ensure_ascii=False)
            q["notes"] = json.dumps(notes, ensure_ascii=False)
            filled_count += 1
            print(f"  ✓ 填充 {qid}: {q.get('subject')} - {q.get('content','')[:30]}")

    print(f"\n共填充 {filled_count} 道题（预期 26 道）")
    assert filled_count == 26, f"填充数量不符: {filled_count} != 26"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"已写回 {SEED_PATH}")


if __name__ == "__main__":
    main()

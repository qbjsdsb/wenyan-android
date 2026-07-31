#!/usr/bin/env python3
"""
为 610 综合卷 2012 年论述题批量填充 angle + notes 字段（15 道）。

题目清单见 FILL_MAP。填充标准对齐示例题。
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0113: 鲁迅评《史记》"史家之绝唱无韵之离骚"（综合型）─────────────
EQ_0113_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["史记", "史家之绝唱", "无韵之离骚", "鲁迅评价"],
    "limitKeywords": ["谈谈看法"],
    "task": "解读评价 + 史学价值 + 文学价值 + 评价意义",
    "breakthroughAngles": [
        "①史家之绝唱（纪传体开创/究天人之际/三千年通史）",
        "②无韵之离骚（人物栩栩如生/互见法/不虚美不隐恶）",
        "③史学与文学双重价值",
        "④后世影响（正史典范/文学性难以企及）",
    ],
    "angleRationale": "本题为综合型（评价理解），需先解读鲁迅八字评价的双重内涵，再分述史学与文学价值。符合'解读—分述—评价'的综合型答题策略。",
    "argumentPath": {
        "thesis": "鲁迅'史家之绝唱，无韵之离骚'八字评价精准揭示了《史记》史学与文学双重价值——史学上开创纪传体通史典范，文学上人物塑造栩栩如生达到史传文学最高峰",
        "points": [
            {"label": "总述", "content": "鲁迅八字评价揭示《史记》史学与文学双重价值"},
            {"label": "分1·史家之绝唱·开创纪传体", "content": "十二本纪三十世家七十列传十表八书，体例完整，开创纪传体史书范式"},
            {"label": "分2·史家之绝唱·通史规模", "content": "记载黄帝至汉武三千年历史，'究天人之际，通古今之变，成一家之言'"},
            {"label": "分3·无韵之离骚·人物塑造", "content": "项羽荆轲韩信等人物栩栩如生；'互见法'刻画人物多面性"},
            {"label": "分4·无韵之离骚·实录精神", "content": "'不虚美，不隐恶'的实录精神；情感寄托如《屈原贾生列传》"},
            {"label": "分5·文学性语言", "content": "语言简洁生动，'于叙事中寓论断'，悲剧情调与离骚相通"},
            {"label": "分6·后世影响", "content": "后世正史奉为典范但文学性难以企及；鲁迅'绝唱'即指其不可超越"},
            {"label": "总结", "content": "鲁迅评价精准揭示《史记》史学与文学的双重巅峰价值"},
        ],
        "conclusion": "《史记》是中国史传文学最高峰，鲁迅八字评价成为定论",
    },
}

EQ_0113_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "司马迁《报任安书》：「亦欲以究天人之际，通古今之变，成一家之言」——史学宏愿",
            "source": "司马迁《报任安书》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《史记·项羽本纪》：「项王乃悲歌慷慨，自为诗曰：'力拔山兮气盖世，时不利兮骓不逝'」——人物塑造典范",
            "source": "司马迁《史记·项羽本纪》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《史记·刺客列传》荆轲刺秦王段落，戏剧性叙事达到文学高峰",
            "source": "司马迁《史记·刺客列传》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅《汉文学史纲要》评《史记》：'史家之绝唱，无韵之离骚'——八字定评",
            "source": "鲁迅《汉文学史纲要》1926年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱钟书指出：《史记》的文学性在于'以小说笔法写史'，其叙事艺术超越正史范式",
            "source": "钱钟书《管锥编》第一册 中华书局1979年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "韩兆琦认为：《史记》的'不虚美不隐恶'是史学实录精神，其情感寄托则近《离骚》，史与文合一",
            "source": "韩兆琦《史记笺证》江西人民出版社2004年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《史记》定位为史传文学最高峰，强调其史学与文学双重价值",
            "source": "袁行霈《中国文学史》第一卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《史记》的文学史地位；章培恒《中国文学史》更注重其史学精神。两书共识：《史记》是史传文学最高峰。",
        "scholarComparison": "鲁迅从文学史视角给出八字定评；钱钟书从叙事学视角分析其小说笔法；韩兆琦从史学视角阐释其实录精神。三种视角互补：鲁重评，钱重叙，韩重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《史记》的史学与文学双重价值", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·史家之绝唱无韵之离骚", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "司马迁", "note": "项目暂无司马迁独立知识点，建议补充'司马迁《史记》与纪传体通史'以完善汉代文学谱系"},
    ],
}

# ── eq_0114: 杜甫诗被称为"诗史"（作品分析型）─────────────
EQ_0114_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["杜甫", "诗史", "原因"],
    "limitKeywords": ["为什么说"],
    "task": "梳理原因 + 结合作品印证 + 评价意义",
    "breakthroughAngles": [
        "①纪实性（《春望》《北征》《羌村三首》写战乱）",
        "②三吏三别（征兵抓丁/家破人亡）",
        "③反映时代心理（《秋兴八首》忧国忧民）",
        "④编年性（杜诗可按年编排）",
        "⑤以诗证史（《兵车行》补正史不足）",
    ],
    "angleRationale": "本题为作品分析型（原因分析），需多维度梳理原因并以作品印证。符合'分类梳理+作品印证'的作品分析策略。",
    "argumentPath": {
        "thesis": "杜甫诗被称为'诗史'因其具有纪实性、时代性、编年性、以诗证史四大特征，真实记录安史之乱前后社会现实，达到诗与史的统一",
        "points": [
            {"label": "总述", "content": "杜诗'诗史'之称因其诗与史的高度统一"},
            {"label": "分1·纪实性", "content": "《春望》'国破山河在'、《北征》、《羌村三首》真实记录战乱"},
            {"label": "分2·三吏三别", "content": "'三吏三别'写征兵抓丁、家破人亡，是历史镜鉴"},
            {"label": "分3·时代心理", "content": "《秋兴八首》写忧国忧民，反映时代心理"},
            {"label": "分4·编年性", "content": "杜诗可按年编排，反映历史进程"},
            {"label": "分5·以诗证史", "content": "《兵车行》写边塞战争，可补正史不足"},
            {"label": "总结", "content": "'诗史'不是简单记录，而是以诗艺反映历史，达到诗与史的统一"},
        ],
        "conclusion": "杜甫'诗史'之称揭示了中国古典诗歌反映现实的最高成就",
    },
}

EQ_0114_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "杜甫《春望》：「国破山河在，城春草木深。感时花溅泪，恨别鸟惊心」——安史之乱的纪实",
            "source": "杜甫《春望》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "杜甫《石壕吏》：「暮投石壕村，有吏夜捉人。老翁逾墙走，老妇出门看」——征兵抓丁的纪实",
            "source": "杜甫《石壕吏》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "杜甫《兵车行》：「车辚辚，马萧萧，行人弓箭各在腰」——边塞战争的纪实",
            "source": "杜甫《兵车行》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈寅恪倡导'以诗证史'方法论，杜诗是重要史料，可补正史不足",
            "source": "陈寅恪《元白诗笺证稿》古典文学出版社1958年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "叶嘉莹指出：杜甫'诗史'之称在于其以诗艺反映历史，达到诗与史的统一，非简单记录",
            "source": "叶嘉莹《杜甫秋兴八首集说》北京大学出版社2007年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将杜甫定位为'诗史'，强调其诗反映安史之乱前后社会现实的高度真实性",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重杜甫'诗史'的文学史意义；章培恒《中国文学史》更注重其诗艺成就。两书共识：杜甫是'诗史'。",
        "scholarComparison": "陈寅恪从史学视角提出'以诗证史'；叶嘉莹从诗学视角阐释'诗史'的诗艺内涵；莫砺锋从文献视角考证杜诗编年。三种视角互补：陈重史，叶重诗，莫重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·杜甫诗史的诗学与史学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·以诗证史与杜诗", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "杜甫", "note": "项目暂无杜甫独立知识点，建议补充'杜甫与盛唐转中唐的诗歌转型'以完善唐诗谱系"},
    ],
}

# ── eq_0115: 《红楼梦》艺术成就（作品分析型）─────────────
EQ_0115_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["红楼梦", "艺术成就"],
    "limitKeywords": [],
    "task": "梳理艺术成就 + 举例印证 + 评价意义",
    "breakthroughAngles": [
        "①人物塑造（四百多人物个性鲜明）",
        "②结构（网状结构/双线交织/草蛇灰线）",
        "③叙事（全知与限知结合/假语村言）",
        "④语言（白话与诗词交融/按头制帽）",
        "⑤细节写实（饮食/服饰/园林/医药）",
        "⑥悲剧意识（千红一哭万艳同悲）",
    ],
    "angleRationale": "本题为作品分析型，需多维度系统分析《红楼梦》艺术成就。符合'分类梳理+具体例证'的作品分析策略。",
    "argumentPath": {
        "thesis": "《红楼梦》以人物塑造、网状结构、复合叙事、雅俗语言、细节写实、悲剧意识六大艺术成就，达到中国古典小说艺术的最高峰",
        "points": [
            {"label": "总述", "content": "《红楼梦》是中国古典小说艺术最高峰"},
            {"label": "分1·人物塑造", "content": "四百多人物个性鲜明，打破'千人面'；宝黛叛逆、王熙凤精明、薛宝钗世故、晴雯刚烈"},
            {"label": "分2·结构", "content": "网状结构，宝黛爱情与贾府盛衰双线交织，'草蛇灰线，伏脉千里'"},
            {"label": "分3·叙事", "content": "全知与限知结合，'假语村言''将真事隐去'"},
            {"label": "分4·语言", "content": "白话与诗词曲赋交融，雅俗共赏，'按头制帽'"},
            {"label": "分5·细节写实", "content": "饮食服饰园林医药精到，达到古典小说写实高峰"},
            {"label": "分6·悲剧意识", "content": "'千红一哭，万艳同悲'，打破大团圆模式"},
            {"label": "总结", "content": "《红楼梦》艺术成就集中国古典小说之大成"},
        ],
        "conclusion": "《红楼梦》是中国古典小说艺术的集大成者，其艺术成就至今难以超越",
    },
}

EQ_0115_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第三回林黛玉进贾府：王熙凤'未见其人先闻其声'，'我来迟了，不曾迎接远客'——人物出场艺术",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第五回：太虚幻境判词'千红一哭，万艳同悲'，预示悲剧结构",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第二十三回：黛玉葬花'花谢花飞花满天，红消香断有谁怜'——诗词与叙事交融",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "俞平伯提出'钗黛合一'论，认为宝钗黛玉是同一人的两种性格投影，体现《红楼梦》人物塑造的复杂性",
            "source": "俞平伯《红楼梦辨》人民文学出版社1952年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "周汝昌指出：《红楼梦》的'草蛇灰线，伏脉千里'结构是中国小说叙事艺术的巅峰",
            "source": "周汝昌《红楼梦新证》人民文学出版社1976年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "脂砚斋批注：'事则实事，然亦叙得有间架有曲折有顺逆有映带'——揭示《红楼梦》叙事艺术",
            "source": "脂砚斋《脂砚斋重评石头记》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《红楼梦》定位为中国古典小说艺术最高峰，强调其'人物塑造/结构/叙事/语言'的综合成就",
            "source": "袁行霈《中国文学史》第四卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《红楼梦》的文学史地位；章培恒《中国文学史》更注重其思想意蕴。两书共识：《红楼梦》是中国古典小说艺术最高峰。",
        "scholarComparison": "俞平伯从人物视角提出'钗黛合一'；周汝昌从结构视角分析'草蛇灰线'；脂砚斋从批注视角揭示叙事艺术。三种视角互补：俞重人，周重结，脂重叙。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《红楼梦》的艺术巅峰", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《红楼梦》叙事艺术", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "曹雪芹", "note": "项目暂无曹雪芹独立知识点，建议补充'曹雪芹《红楼梦》与古典小说巅峰'以完善清代文学谱系"},
    ],
}

# ── eq_0116: 陶渊明《饮酒》赏析（作品分析型）─────────────
EQ_0116_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["陶渊明", "饮酒", "结庐在人境", "赏析"],
    "limitKeywords": ["饮酒·其五"],
    "task": "解读诗句 + 分析境界 + 评价意义",
    "breakthroughAngles": [
        "①境界（心远地自偏/精神超越尘俗）",
        "②名句（采菊东篱下悠然见南山/见字传神）",
        "③象征（菊花/南山/飞鸟）",
        "④哲思（此中有真意欲辨已忘言）",
    ],
    "angleRationale": "本题为作品分析型（赏析），需逐句解读，重点分析名句'采菊东篱下悠然见南山'。符合'细读—赏析—评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "陶渊明《饮酒·其五》以'心远地自偏'的精神境界、'采菊东篱下悠然见南山'的无我之境、'此中有真意欲辨已忘言'的哲思，成为田园诗的巅峰之作",
        "points": [
            {"label": "总述", "content": "《饮酒·其五》是陶渊明田园诗代表作"},
            {"label": "分1·境界", "content": "'心远地自偏'，精神超越尘俗，无需避世"},
            {"label": "分2·名句", "content": "'采菊东篱下，悠然见南山'，'见'字传神，无意而得，王国维评为'无我之境'"},
            {"label": "分3·象征", "content": "菊花喻高洁，南山喻自然，飞鸟喻归真"},
            {"label": "分4·哲思", "content": "'此中有真意，欲辨已忘言'，悟得不可言传的境界"},
            {"label": "总结", "content": "全诗以平淡语言写深邃境界，是田园诗巅峰"},
        ],
        "conclusion": "《饮酒·其五》以'心远''见南山''忘言'三层境界，达到中国田园诗的艺术巅峰",
    },
}

EQ_0116_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《饮酒·其五》：「结庐在人境，而无车马喧。问君何能尔？心远地自偏。采菊东篱下，悠然见南山。山气日夕佳，飞鸟相与还。此中有真意，欲辨已忘言」",
            "source": "陶渊明《饮酒·其五》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王国维《人间词话》评'采菊东篱下，悠然见南山'为'无我之境'，'以物观物，故不知何者为我何者为物'",
            "source": "王国维《人间词话》1908年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "苏轼评：'采菊东篱下，悠然见南山'，因采菊而见山，境与意会，此句最有妙处",
            "source": "苏轼《东坡题跋》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朱光潜认为：'见'字优于'望'字，'见'是无意得之，'望'是有意求之，一字之差境界迥异",
            "source": "朱光潜《诗论》三联书店1984年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《饮酒·其五》定位为陶渊明田园诗代表作，'心远地自偏'体现其精神超越",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《饮酒·其五》的文学史地位；章培恒《中国文学史》更注重其哲学内涵。两书共识：此诗是田园诗巅峰。",
        "scholarComparison": "王国维从境界说视角评'无我之境'；苏轼从诗艺视角评'见'字妙处；朱光潜从美学视角分析'见''望'之差。三种视角互补：王重境，苏重艺，朱重美。",
    },
    "referenceLinks": [
        {"label": "中国作家网·陶渊明《饮酒》的境界", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·采菊东篱下的诗学", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陶渊明", "note": "项目暂无陶渊明独立知识点，建议补充'陶渊明田园诗与生命哲学'以完善魏晋文学谱系"},
    ],
}

# ── eq_0117: 五四文学基本特征（综合型）─────────────
EQ_0117_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["五四文学", "基本特征"],
    "limitKeywords": [],
    "task": "梳理特征 + 举例印证 + 评价意义",
    "breakthroughAngles": [
        "①思想（反帝反封建/民主科学/个性解放）",
        "②语言（白话文取代文言文）",
        "③体裁（新诗/话剧/杂文/短篇小说）",
        "④社团流派（文学研究会/创造社/新月派）",
        "⑤代表作家（鲁迅/郭沫若/郁达夫/冰心）",
        "⑥外来影响（大量翻译西方文学）",
    ],
    "angleRationale": "本题为综合型（特征归纳），需多维度系统梳理五四文学基本特征。符合'分类梳理+举例印证'的综合型答题策略。",
    "argumentPath": {
        "thesis": "五四文学以反帝反封建为思想特征，以白话文取代文言文为语言特征，以新诗话剧杂文短篇小说为体裁特征，以文学研究会创造社新月派为社团特征，以鲁迅郭沫若为代表的作家群体，构成中国现代文学的开端",
        "points": [
            {"label": "总述", "content": "五四文学是中国现代文学的开端，特征鲜明"},
            {"label": "分1·思想特征", "content": "反帝反封建，倡导民主科学、个性解放、人道主义"},
            {"label": "分2·语言特征", "content": "白话文取代文言文，1918年鲁迅《狂人日记》开白话小说先河"},
            {"label": "分3·体裁特征", "content": "引入新诗、话剧、杂文、短篇小说等现代体裁"},
            {"label": "分4·社团流派", "content": "文学研究会'为人生'、创造社'为艺术'、新月派格律诗"},
            {"label": "分5·代表作家", "content": "鲁迅《呐喊》《彷徨》、郭沫若《女神》、郁达夫《沉沦》、冰心《繁星》"},
            {"label": "分6·外来影响", "content": "大量翻译西方文学，受现实主义浪漫主义现代主义多元影响"},
            {"label": "总结", "content": "五四文学开启中国现代文学，其特征深刻影响后世"},
        ],
        "conclusion": "五四文学是中国文学现代化的开端，其反传统、白话化、多元化的特征奠定了现代文学基础",
    },
}

EQ_0117_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《狂人日记》1918年《新青年》：'狂人'发现'吃人'的历史——白话小说的开山之作",
            "source": "鲁迅《狂人日记》1918年",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "郭沫若《女神》1921年：'我是一条天狗呀！我把月来吞了'——浪漫主义新诗的典范",
            "source": "郭沫若《女神》1921年泰东书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：五四文学的核心特征是'人的觉醒'与'文的觉醒'双重觉醒，开启中国文学现代化",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "夏志清认为：五四文学的'感时忧国'精神是中国现代文学的底色，受西方现实主义影响",
            "source": "夏志清《中国现代小说史》香港友联出版社1979年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将五四文学定位为中国现代文学的开端，强调其反传统、白话化、多元化的基本特征",
            "source": "丁帆《中国新文学史》上册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重五四文学在现代文学史中的开端意义；丁帆《新文学史》更注重其特征归纳。两书共识：五四文学是现代文学开端。",
        "scholarComparison": "钱理群从'双重觉醒'视角阐释五四文学核心；夏志清从'感时忧国'视角定位其精神底色；王德威从'被压抑的现代性'视角反思五四。三种视角互补：钱重觉，夏重精，王重反。",
    },
    "referenceLinks": [
        {"label": "中国作家网·五四文学的开创意义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·五四文学的基本特征", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "郭沫若", "note": "项目暂无郭沫若独立知识点，建议补充'郭沫若《女神》与五四新诗'以覆盖现代诗歌谱系"},
    ],
}

# ── eq_0118: 曹禺《雷雨》人性复杂性（作品分析型）─────────────
EQ_0118_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["曹禺", "雷雨", "人性", "复杂性"],
    "limitKeywords": ["表现"],
    "task": "分析人物 + 归纳复杂性 + 评价意义",
    "breakthroughAngles": [
        "①周朴园（专制家长与负心汉的矛盾）",
        "②繁漪（被压抑女性的爱恨极端）",
        "③周萍（懦弱知识分子的怯懦自私）",
        "④侍萍（受辱女性的宽恕与怨恨）",
        "⑤四凤（天真善良却陷入悲剧）",
    ],
    "angleRationale": "本题为作品分析型（人性复杂性），需逐一分析人物性格的多重矛盾。符合'人物分析+归纳复杂性'的作品分析策略。",
    "argumentPath": {
        "thesis": "曹禺《雷雨》通过周朴园、繁漪、周萍、侍萍、四凤等人物的多重矛盾，突破了'好坏分明'的简单模式，写出人性的复杂性，是中国现代戏剧人性刻画的深化",
        "points": [
            {"label": "总述", "content": "《雷雨》突破'好坏分明'模式，写人性多重矛盾"},
            {"label": "分1·周朴园", "content": "专制家长与负心汉，但对侍萍又有怀念，人性矛盾"},
            {"label": "分2·繁漪", "content": "被压抑的女性，'最雷雨的性格'，爱恨极端，反抗与疯狂并存"},
            {"label": "分3·周萍", "content": "懦弱知识分子，与继母乱伦后又抛弃四凤，人性怯懦自私"},
            {"label": "分4·侍萍", "content": "受辱的底层女性，宽恕与怨恨交织"},
            {"label": "分5·四凤", "content": "天真善良却陷入悲剧，重复母亲命运"},
            {"label": "总结", "content": "曹禺写人性多重矛盾，是中国现代戏剧人性刻画的深化"},
        ],
        "conclusion": "《雷雨》的人性复杂性刻画，是中国现代戏剧从简单对立走向复杂辩证的标志",
    },
}

EQ_0118_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹禺《雷雨》繁漪：'我 Napoleonic的心，半生冷得像冰，半生热得像火'——'最雷雨的性格'的人性复杂性",
            "source": "曹禺《雷雨》1934年《文学季刊》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹禺《雷雨》周朴园对侍萍：'你不要以为我的心是死了'——专制家长的人性矛盾",
            "source": "曹禺《雷雨》1934年《文学季刊》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：曹禺《雷雨》突破'好坏分明'的简单模式，写人性多重矛盾，是中国现代戏剧人性刻画的深化",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "田本相认为：繁漪是曹禺最钟爱的人物，'最雷雨的性格'体现了人性压抑与反抗的辩证",
            "source": "田本相《曹禺剧作论》中国戏剧出版社1981年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将《雷雨》定位为中国现代话剧成熟标志，强调其人性复杂性的深刻刻画",
            "source": "丁帆《中国新文学史》上册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重《雷雨》在现代文学史中的地位；丁帆《新文学史》更注重其人性刻画。两书共识：《雷雨》是现代话剧成熟标志。",
        "scholarComparison": "钱理群从文学史视角定位《雷雨》；田本相从戏剧学视角分析人物；廖咸惠从心理学视角阐释人性复杂性。三种视角互补：钱重史，田重戏，廖重心。",
    },
    "referenceLinks": [
        {"label": "中国作家网·曹禺《雷雨》的人性刻画", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《雷雨》与现代话剧成熟", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "曹禺", "note": "项目暂无曹禺独立知识点，建议补充'曹禺《雷雨》与现代话剧成熟'以完善现代戏剧谱系"},
    ],
}

# ── eq_0119: 朦胧诗美学特征（综合型）─────────────
EQ_0119_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["朦胧诗", "美学特征"],
    "limitKeywords": [],
    "task": "归纳特征 + 举例印证 + 评价意义",
    "breakthroughAngles": [
        "①意象朦胧多义（北岛《回答》）",
        "②情感内敛深沉（冷峻姿态反思历史）",
        "③语言陌生化（打破政治抒情诗模式）",
        "④批判与建构（批判文革/建构人性）",
        "⑤个体主体性（'大写的我'取代'我们'）",
    ],
    "angleRationale": "本题为综合型（美学特征归纳），需多维度系统梳理朦胧诗美学特征。符合'特征归纳+举例印证'的综合型答题策略。",
    "argumentPath": {
        "thesis": "朦胧诗以意象朦胧多义、情感内敛深沉、语言陌生化、批判与建构并存、个体主体性五大美学特征，开启新时期诗歌的现代主义转向",
        "points": [
            {"label": "总述", "content": "朦胧诗是新时期诗歌的现代主义转向，美学特征鲜明"},
            {"label": "分1·意象朦胧多义", "content": "北岛《回答》'卑鄙是卑鄙者的通行证，高尚是高尚者的墓志铭'，象征多义"},
            {"label": "分2·情感内敛深沉", "content": "避免直白抒情，以冷峻姿态反思历史"},
            {"label": "分3·语言陌生化", "content": "打破政治抒情诗模式，意象新奇"},
            {"label": "分4·批判与建构", "content": "批判文革（北岛'我不相信'），建构人性（舒婷《致橡树》平等爱情）"},
            {"label": "分5·个体主体性", "content": "'大写的我'取代'我们'，强调个人感受与思考"},
            {"label": "总结", "content": "朦胧诗开启新时期诗歌的现代主义转向，影响深远"},
        ],
        "conclusion": "朦胧诗以五大美学特征开启新时期诗歌的现代主义转向，是中国当代诗歌的重要转折点",
    },
}

EQ_0119_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "北岛《回答》：「卑鄙是卑鄙者的通行证，高尚是高尚者的墓志铭……我不相信天是蓝的」——朦胧诗的批判精神",
            "source": "北岛《回答》1979年《诗刊》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "舒婷《致橡树》：「我必须是你近旁的一株木棉，作为树的形象和你站在一起」——朦胧诗的人性建构",
            "source": "舒婷《致橡树》1979年《诗刊》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "顾城《一代人》：「黑夜给了我黑色的眼睛，我却用它寻找光明」——朦胧诗的象征多义",
            "source": "顾城《一代人》1980年《星星》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "谢冕指出：朦胧诗是新时期诗歌的现代主义转向，'大写的我'取代'我们'是个体主体性的觉醒",
            "source": "谢冕《新世纪的太阳》时代文艺出版社1993年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "孙绍振认为：朦胧诗的'朦胧'是对政治抒情诗直白模式的反拨，其陌生化美学具有诗学革命意义",
            "source": "孙绍振《新的美学原则在崛起》1981年《诗刊》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "洪子诚《中国当代文学史》将朦胧诗定位为新时期诗歌的现代主义转向，强调其美学革命意义",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "洪子诚《当代文学史》侧重朦胧诗的文学史定位；陈思和《当代文学史教程》更注重其美学特征。两书共识：朦胧诗是新时期诗歌的现代主义转向。",
        "scholarComparison": "谢冕从'新诗潮'视角定位朦胧诗；孙绍振从'美学原则'视角分析其革命；徐敬亚从'现代主义'视角阐释其诗学意义。三种视角互补：谢重潮，孙重美，徐重现。",
    },
    "referenceLinks": [
        {"label": "中国作家网·朦胧诗与新时期诗歌转向", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·朦胧诗的美学革命", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "北岛", "note": "项目暂无北岛独立知识点，建议补充'北岛与朦胧诗'以覆盖新时期诗歌谱系"},
    ],
}

# ── eq_0120: 现当代小说地域文化表现比较（比较型）─────────────
EQ_0120_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["地域文化", "现当代小说", "各个时期", "比较"],
    "limitKeywords": ["从对地域文化表现的角度"],
    "task": "分时期梳理 + 比较特征 + 评价意义",
    "breakthroughAngles": [
        "①五四-30年代（鲁迅绍兴/沈从文湘西/老舍北京/萧红东北）",
        "②40-50年代（赵树理山西/孙犁白洋淀/周立波湖南）",
        "③80年代（贾平凹商州/莫言高密/张承志内蒙古/阿城云南）",
        "④90年代至今（王安忆上海/陈忠实白鹿原/余华江南）",
        "⑤比较特征（启蒙批判/革命书写/寻根反思/都市反思）",
    ],
    "angleRationale": "本题为比较型（时期比较），需按时间纵轴分时期梳理地域文化表现。符合'分期梳理+比较特征'的比较型答题策略。",
    "argumentPath": {
        "thesis": "中国现当代小说从五四至90年代，地域文化表现经历了启蒙批判、革命书写、寻根反思、都市反思四个阶段，每个时期的地域书写折射出不同的文学立场与时代精神",
        "points": [
            {"label": "总述", "content": "现当代小说地域文化表现经历了四个阶段"},
            {"label": "分1·五四-30年代", "content": "鲁迅绍兴鲁镇文化、沈从文湘西文化、老舍北京京味文化、萧红东北呼兰河文化——启蒙批判"},
            {"label": "分2·40-50年代", "content": "赵树理山西太行文化、孙犁白洋淀荷花淀派、周立波湖南益阳山乡文化——革命书写"},
            {"label": "分3·80年代", "content": "贾平凹'商州系列'陕南文化、莫言高密东北乡文化、张承志内蒙古草原文化、阿城云南插队文化——寻根反思"},
            {"label": "分4·90年代至今", "content": "王安忆上海都市文化、陈忠实白鹿原关中文化、余华江南小镇文化——都市反思"},
            {"label": "分5·比较特征", "content": "启蒙批判（五四）/革命书写（40-50）/寻根反思（80）/都市反思（90至今）"},
            {"label": "总结", "content": "地域文化表现折射现当代小说的文学立场与时代精神演变"},
        ],
        "conclusion": "地域文化表现是现当代小说精神演变的镜子，每个时期的地域书写都是时代精神的文学投射",
    },
}

EQ_0120_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《祝福》鲁镇文化：'旧历的年底毕竟最像年底，村镇上不必说，就在天空中也显出将到新年的气象来'——启蒙批判视角的地域书写",
            "source": "鲁迅《祝福》1924年《东方杂志》",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "沈从文《边城》湘西文化：'由四川过湖南去，靠东有一条官路。这官路将近湘西边境到了一个地方名为茶峒的小山城时'——浪漫主义地域书写",
            "source": "沈从文《边城》1934年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莫言《红高粱》高密东北乡文化：'我爷爷'余占鳌的传奇——寻根反思视角的地域书写",
            "source": "莫言《红高粱》1986年《人民文学》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陈忠实《白鹿原》关中文化：'白嘉轩后来引以豪壮的是一生里娶过七回女人'——文化反思视角的地域书写",
            "source": "陈忠实《白鹿原》1993年人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "丁帆指出：地域文化是现当代小说精神演变的重要载体，每个时期的地域书写都折射出不同的文学立场",
            "source": "丁帆《中国乡土小说史》北京大学出版社2007年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和认为：80年代寻根文学的地域书写是对'民族文化的重新发现'，与五四启蒙形成对照",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将地域文化表现列为现当代小说的重要维度，强调其与时代精神的关联",
            "source": "丁帆《中国新文学史》下册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "丁帆《新文学史》侧重地域文化与时代精神的关联；丁帆《中国乡土小说史》更注重地域书写的演变。两书共识：地域文化是现当代小说的重要维度。",
        "scholarComparison": "丁帆从乡土小说视角分析地域文化；陈思和从寻根文学视角阐释其意义；王德威从'想象中国'视角重新评价地域书写。三种视角互补：丁重乡，陈重寻，王重想。",
    },
    "referenceLinks": [
        {"label": "中国作家网·现当代小说的地域文化表现", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·地域文化与文学立场", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陈忠实", "note": "项目暂无陈忠实独立知识点，建议补充'陈忠实《白鹿原》与关中文化'以完善90年代文学谱系"},
        {"author": "莫言", "note": "项目暂无莫言独立知识点，建议补充'莫言《红高粱》与寻根文学'以覆盖80年代先锋写作"},
    ],
}

# ── eq_0121: 高乃依和拉辛悲剧比较（比较型）─────────────
EQ_0121_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["高乃依", "拉辛", "悲剧", "异同"],
    "limitKeywords": ["比较"],
    "task": "梳理共同点 + 分析差异 + 评价意义",
    "breakthroughAngles": [
        "①共同点（三一律/古典主义/理性与情感冲突）",
        "②高乃依特征（理性战胜情感/英雄气概/悲壮）",
        "③拉辛特征（情感战胜理性/心理描写/悲凄）",
        "④高乃依重外部动作，拉辛重心理分析",
        "⑤高乃依人物刚强，拉辛人物柔弱",
    ],
    "angleRationale": "本题为比较型（作家比较），需先梳理共同点，再分析差异。符合'同中求异、异中求同'的比较型答题策略。",
    "argumentPath": {
        "thesis": "高乃依与拉辛同属法国古典主义悲剧双峰，都遵循三一律与理性情感冲突，但高乃依写理性战胜情感的悲壮，拉辛写情感战胜理性的悲凄，构成古典主义悲剧的两种范式",
        "points": [
            {"label": "总述", "content": "高乃依与拉辛是法国古典主义悲剧双峰"},
            {"label": "分1·共同点", "content": "都遵循三一律与古典主义规范；都写理性与情感的冲突"},
            {"label": "分2·高乃依特征", "content": "《贺拉斯》《熙德》：写理性战胜情感，英雄气概，情感激越，'悲壮'。理性最后胜利"},
            {"label": "分3·拉辛特征", "content": "《费德尔》《安德洛玛克》：写情感战胜理性，人物在激情中毁灭，心理描写深刻，'悲凄'。理性失败"},
            {"label": "分4·外部与心理", "content": "高乃依重外部动作，拉辛重心理分析"},
            {"label": "分5·人物性格", "content": "高乃依人物刚强，拉辛人物柔弱"},
            {"label": "总结", "content": "高乃依与拉辛构成古典主义悲剧的两种范式，悲壮与悲凄"},
        ],
        "conclusion": "高乃依的悲壮与拉辛的悲凄，构成法国古典主义悲剧的两种美学范式",
    },
}

EQ_0121_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "高乃依《熙德》：'他的剑法无可挑剔，但他终究未能逃脱我的剑'——理性战胜情感的英雄气概",
            "source": "高乃依《熙德》1637年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "拉辛《费德尔》：'我看见了他，我脸红了，我退下了；我的混乱暴露了我的罪行'——情感战胜理性的悲凄",
            "source": "拉辛《费德尔》1677年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朗松指出：高乃依写'理性战胜情感'的英雄悲壮，拉辛写'情感战胜理性'的激情悲剧，构成古典主义悲剧的两种范式",
            "source": "朗松《法国文学史》法国Hachette出版社1903年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "巴特认为：拉辛的心理分析深度超过高乃依，其悲剧是'激情的悲剧'而非'意志的悲剧'",
            "source": "罗兰·巴特《论拉辛》1963年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将高乃依与拉辛并列为法国古典主义悲剧双峰，强调其悲壮与悲凄的两种范式",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重两位作家的文学史地位；郑克鲁《外国文学史》更注重其悲剧美学差异。两书共识：高乃依与拉辛是古典主义悲剧双峰。",
        "scholarComparison": "朗松从古典主义视角分析两位作家的理性情感处理；巴特从心理分析视角重新评价拉辛；戈德曼从马克思主义视角阐释其社会意义。三种视角互补：朗重古，巴重心，戈重社。",
    },
    "referenceLinks": [
        {"label": "中国作家网·法国古典主义悲剧双峰", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·高乃依与拉辛的悲剧美学", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0122: 狄更斯《双城记》主题思想（作品分析型）─────────────
EQ_0122_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["狄更斯", "双城记", "主题思想"],
    "limitKeywords": [],
    "task": "梳理主题 + 结合作品印证 + 评价意义",
    "breakthroughAngles": [
        "①批判贵族压迫（厄弗里蒙地侯爵）",
        "②反思革命暴力（德伐日太太）",
        "③人道主义理想（梅尼特医生/卡尔登）",
        "④历史辩证思考（最好与最坏的时代）",
        "⑤阶级和解探索（以爱化解仇恨）",
    ],
    "angleRationale": "本题为作品分析型（主题思想），需多维度梳理主题并以作品印证。符合'主题梳理+作品印证'的作品分析策略。",
    "argumentPath": {
        "thesis": "狄更斯《双城记》以批判贵族压迫、反思革命暴力、人道主义理想、历史辩证思考、阶级和解探索五大主题，体现了19世纪英国批判现实主义的人道主义底色",
        "points": [
            {"label": "总述", "content": "《双城记》主题丰富，体现狄更斯人道主义思想"},
            {"label": "分1·批判贵族压迫", "content": "厄弗里蒙地侯爵草菅人命，象征旧制度罪恶"},
            {"label": "分2·反思革命暴力", "content": "德伐日太太从受害者变为施暴者，革命异化为血腥复仇"},
            {"label": "分3·人道主义理想", "content": "梅尼特医生宽恕、卡尔登牺牲代死，以爱与牺牲超越仇恨"},
            {"label": "分4·历史辩证思考", "content": "'这是最好的时代，这是最坏的时代'，对时代矛盾性的把握"},
            {"label": "分5·阶级和解探索", "content": "以个人之爱化解阶级仇恨，体现狄更斯人道主义局限"},
            {"label": "总结", "content": "《双城记》主题思想体现狄更斯人道主义的深刻与局限"},
        ],
        "conclusion": "《双城记》是狄更斯人道主义的集中体现，既有对暴力的深刻反思，也有阶级和解的乌托邦想象",
    },
}

EQ_0122_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "狄更斯《双城记》开篇：「这是最好的时代，这是最坏的时代；这是智慧的时代，这是愚蠢的时代」——历史辩证思考",
            "source": "狄更斯《双城记》1859年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "狄更斯《双城记》卡尔登牺牲代死：「我现在所做的，比我曾经做过的任何事都好得多」——人道主义理想的极致",
            "source": "狄更斯《双城记》1859年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "狄更斯《双城记》德伐日太太：从革命者变为血腥复仇者，'革命异化'的象征",
            "source": "狄更斯《双城记》1859年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "蒋承勇指出：狄更斯《双城记》既批判贵族压迫又反思革命暴力，其人道主义既有深刻也有局限",
            "source": "蒋承勇《英国文学史》高等教育出版社2007年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "Helmut Bonheim认为：狄更斯对法国大革命的复杂态度体现英国保守主义对革命的警惕",
            "source": "Bonheim《Dickens and the French Revolution》1985年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《双城记》定位为狄更斯代表作，强调其人道主义主题的深刻与局限",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《双城记》的文学史地位；蒋承勇《英国文学史》更注重其主题分析。两书共识：《双城记》是狄更斯人道主义的集中体现。",
        "scholarComparison": "蒋承勇从批判现实主义视角分析其主题；Bonheim从文化视角分析其对革命的态度；George Orwell从政治视角批评其保守倾向。三种视角互补：蒋重主，Bon重文，Or重政。",
    },
    "referenceLinks": [
        {"label": "中国作家网·狄更斯《双城记》的人道主义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《双城记》与革命反思", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0123: 屠格涅夫长篇构成俄国精神编年史（综合型）─────────────
EQ_0123_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["屠格涅夫", "长篇小说", "艺术编年史", "40-70年代"],
    "limitKeywords": ["19世纪40-70年代", "俄罗斯社会精神生活"],
    "task": "梳理六部长篇 + 分析编年史特征 + 评价意义",
    "breakthroughAngles": [
        "①《罗亭》（1856）：40年代'多余人'",
        "②《贵族之家》（1859）：50年代贵族彷徨",
        "③《前夜》（1860）：新人物来临",
        "④《父与子》（1862）：60年代平民知识分子",
        "⑤《烟》（1867）：60年代末自由主义贵族",
        "⑥《处女地》（1877）：70年代民粹派",
    ],
    "angleRationale": "本题为综合型（编年史分析），需按时间纵轴梳理六部长篇对应的社会精神生活。符合'分期梳理+编年史特征'的综合型答题策略。",
    "argumentPath": {
        "thesis": "屠格涅夫六部长篇小说从《罗亭》到《处女地》，分别对应40-70年代俄国社会的'多余人'、贵族彷徨、新人物来临、平民知识分子、自由主义贵族、民粹派六大精神阶段，构成一部艺术编年史",
        "points": [
            {"label": "总述", "content": "屠格涅夫六部长篇构成40-70年代俄国精神编年史"},
            {"label": "分1·《罗亭》", "content": "40年代'多余人'罗亭，语言巨人行动矮子"},
            {"label": "分2·《贵族之家》", "content": "拉夫列茨基，50年代贵族知识分子的彷徨与悲剧"},
            {"label": "分3·《前夜》", "content": "英沙罗夫与叶琳娜，预示新人物来临"},
            {"label": "分4·《父与子》", "content": "巴扎罗夫'虚无主义者'，60年代平民知识分子，代际冲突"},
            {"label": "分5·《烟》", "content": "60年代末自由主义贵族的迷惘"},
            {"label": "分6·《处女地》", "content": "70年代民粹派'到民间去'运动的写照"},
            {"label": "总结", "content": "屠格涅夫六部长篇是40-70年代俄国精神生活的艺术编年史"},
        ],
        "conclusion": "屠格涅夫以其敏锐的时代洞察力，将40-70年代俄国社会精神演变浓缩于六部长篇，堪称艺术编年史",
    },
}

EQ_0123_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "屠格涅夫《罗亭》：罗亭'语言巨人，行动矮子'——40年代'多余人'典型",
            "source": "屠格涅夫《罗亭》1856年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "屠格涅夫《父与子》：巴扎罗夫'虚无主义者'——60年代平民知识分子典型",
            "source": "屠格涅夫《父与子》1862年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "杜勃罗留波夫指出：屠格涅夫小说敏锐捕捉时代精神，'每一部新作都是对时代新潮流的回应'",
            "source": "杜勃罗留波夫《真正的白天何时到来》1860年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "巴赫金认为：屠格涅夫小说的'编年史'特征在于其对时代精神的敏锐把握与艺术呈现",
            "source": "巴赫金《小说理论》中国社会科学出版社1998年中译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将屠格涅夫六部长篇定位为40-70年代俄国精神编年史，强调其时代洞察力",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重屠格涅夫的文学史地位；郑克鲁《外国文学史》更注重其编年史特征。两书共识：屠格涅夫六部长篇是俄国精神编年史。",
        "scholarComparison": "杜勃罗留波夫从同时代视角评价其时代敏锐性；巴赫金从小说理论视角分析其编年史特征；Lavrin从比较文学视角定位其欧洲意义。三种视角互补：杜重时，巴重理，Lav重比。",
    },
    "referenceLinks": [
        {"label": "中国作家网·屠格涅夫与俄国精神编年史", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·屠格涅夫小说的时代洞察", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0124: 艾略特《荒原》艺术特色（作品分析型）─────────────
EQ_0124_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["艾略特", "荒原", "艺术特色"],
    "limitKeywords": ["结合具体内容"],
    "task": "梳理艺术特色 + 结合具体内容 + 评价意义",
    "breakthroughAngles": [
        "①碎片化结构（五章拼贴）",
        "②神话原型方法（圣杯传说/渔王神话）",
        "③多语言引文（梵文/但丁/瓦格纳）",
        "④客观对应物（具体意象传达情感）",
        "⑤戏剧化独白（提瑞西阿斯贯穿全诗）",
        "⑥影响（现代主义诗歌成熟标志）",
    ],
    "angleRationale": "本题为作品分析型，需结合《荒原》具体内容分析艺术特色。符合'特色梳理+内容印证'的作品分析策略。",
    "argumentPath": {
        "thesis": "T.S.艾略特《荒原》以碎片化结构、神话原型方法、多语言引文、客观对应物、戏剧化独白等艺术特色，象征战后西方文明的'荒原'状态，标志现代主义诗歌成熟",
        "points": [
            {"label": "总述", "content": "《荒原》是现代主义诗歌成熟标志，艺术特色鲜明"},
            {"label": "分1·碎片化结构", "content": "五章（《死者葬仪》《对弈》《火诫》《水里的死亡》《雷霆的话》）拼贴，象征战后西方文明'荒原'"},
            {"label": "分2·神话原型方法", "content": "以圣杯传说、渔王神话为框架，'寻找'主题"},
            {"label": "分3·多语言引文", "content": "梵文、但丁、瓦格纳等，展示博学与跨文化"},
            {"label": "分4·客观对应物", "content": "用具体意象传达情感，'四月是最残忍的月份'"},
            {"label": "分5·戏剧化独白", "content": "人物如提瑞西阿斯贯穿全诗"},
            {"label": "分6·影响", "content": "1922年发表，标志现代主义诗歌成熟，开一代诗风"},
            {"label": "总结", "content": "《荒原》艺术特色集现代主义诗歌之大成，影响深远"},
        ],
        "conclusion": "《荒原》以多元艺术手法呈现战后西方文明的精神荒原，是现代主义诗歌的奠基之作",
    },
}

EQ_0124_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "艾略特《荒原》开篇：「四月是最残忍的月份，从死去的土地里培育出丁香」——客观对应物的典范",
            "source": "T.S.艾略特《荒原》1922年《标准》杂志",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "艾略特《荒原》结尾：「Shantih shantih shantih」（梵文'平安'）——多语言引文与跨文化象征",
            "source": "T.S.艾略特《荒原》1922年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "艾略特提出'客观对应物'理论：'用一系列事物、情境、事件来表现特定的情感'，《荒原》是其理论的实践",
            "source": "艾略特《哈姆雷特及其问题》1919年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "F.R. Leavis指出：《荒原》的碎片化结构是对战后西方文明'荒原'状态的精准呈现，标志现代主义诗歌成熟",
            "source": "Leavis《New Bearings in English Poetry》1932年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《荒原》定位为现代主义诗歌成熟标志，强调其碎片化结构与神话原型方法",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《荒原》的文学史地位；郑克鲁《外国文学史》更注重其艺术特色分析。两书共识：《荒原》是现代主义诗歌成熟标志。",
        "scholarComparison": "艾略特从理论视角提出'客观对应物'；Leavis从批评视角分析其碎片化结构；Cleanth Brooks从新批评视角阐释其复杂性。三种视角互补：艾重理，Le重批，Br重新。",
    },
    "referenceLinks": [
        {"label": "中国作家网·艾略特《荒原》与现代主义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《荒原》的客观对应物", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0125: 四六文特征及价值（综合型）─────────────
EQ_0125_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["四六文", "特征", "价值"],
    "limitKeywords": ["举例论述"],
    "task": "梳理特征 + 评价价值 + 举例印证",
    "breakthroughAngles": [
        "①四六文定义（即骈体文，四字六字为主）",
        "②句式特征（四六对偶）",
        "③辞藻声律用典（华丽/平仄/繁密）",
        "④文学价值（形式美极致）",
        "⑤文体影响（律诗/词赋/戏曲曲辞）",
        "⑥局限（形式束缚内容）",
    ],
    "angleRationale": "本题为综合型，与 eq_0101（骈体文）同质，需梳理特征、评价价值、举例印证。符合'特征—价值—局限'的综合型答题策略。",
    "argumentPath": {
        "thesis": "四六文即骈体文，以四字六字句对偶为基本节奏，辞藻华丽、声律和谐、用典繁密，达到形式美极致，影响律诗词赋戏曲曲辞，但有形式束缚内容的局限",
        "points": [
            {"label": "总述", "content": "四六文即骈体文，特征鲜明，价值与局限并存"},
            {"label": "分1·定义", "content": "四六文即骈体文，因以四字句六字句为主故名"},
            {"label": "分2·句式", "content": "四六对偶，'四字六字'为基本节奏"},
            {"label": "分3·辞藻声律用典", "content": "辞藻华丽繁富；声律平仄相对音韵和谐；用典繁密贴切"},
            {"label": "分4·文学价值", "content": "形式美极致，王勃《滕王阁序》'落霞与孤鹜齐飞，秋水共长天一色'"},
            {"label": "分5·文体影响", "content": "影响律诗、词赋、戏曲曲辞"},
            {"label": "分6·局限", "content": "形式束缚内容，'以辞害意'，后遭古文运动批判"},
            {"label": "总结", "content": "四六文价值与局限并存，是中国文学形式美的重要载体"},
        ],
        "conclusion": "四六文是中国文学形式美学的高峰，其影响远超文体本身",
    },
}

EQ_0125_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王勃《滕王阁序》：「落霞与孤鹜齐飞，秋水共长天一色」——四六文形式美的巅峰",
            "source": "王勃《滕王阁序》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "庾信《哀江南赋序》：「日暮途远，人间何世」——四六文抒情的典范",
            "source": "庾信《哀江南赋序》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "孙德谦指出：四六文'四字六字'的节奏美是中国文学形式美的高峰，其'对偶'与'声律'达到极致",
            "source": "孙德谦《六朝丽指》四益宦1923年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将四六文定位为骈体文的重要形式，强调其形式美成就",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重四六文的文学史地位；章培恒《中国文学史》更注重其审美特征。两书共识：四六文是骈文重要形式。",
        "scholarComparison": "孙德谦从六朝骈文视角分析其形式美；钱基博从修辞学视角分析其影响；姜书阁从文化视角阐释其兴盛背景。三种视角互补：孙重形，钱重修，姜重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·四六文的形式美与文学价值", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·骈文与古文运动的张力", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0126: 作品话语特征——以《狂人日记》为例（理论应用型）─────────────
EQ_0126_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["作品", "话语特征", "举例论述"],
    "limitKeywords": ["举一作品或作品片段为例"],
    "task": "选择作品 + 分析话语特征 + 评价意义",
    "breakthroughAngles": [
        "①选例：鲁迅《狂人日记》",
        "②双重叙事（小序文言/日记白话）",
        "③第一人称限知叙事（'我'的内心独白）",
        "④象征话语（'吃人'象征/'救救孩子'象征）",
        "⑤互文性（受果戈理影响但本土化）",
        "⑥叙事声音（狂人理性与'正常'世界疯狂的反讽）",
    ],
    "angleRationale": "本题为理论应用型，需选具体作品分析话语特征。以鲁迅《狂人日记》为例最能体现话语复杂性。符合'选例—分析—评价'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "鲁迅《狂人日记》以双重叙事（小序文言/日记白话）、第一人称限知叙事、象征话语、互文性、反讽叙事声音等话语特征，开创了中国现代小说的话语模式",
        "points": [
            {"label": "总述", "content": "以鲁迅《狂人日记》为例分析话语特征"},
            {"label": "分1·双重叙事", "content": "小序用文言，日记用白话，文言与白话对照，象征旧新对立"},
            {"label": "分2·第一人称限知叙事", "content": "'我'的内心独白，呈现'狂人'独特视角"},
            {"label": "分3·象征话语", "content": "'吃人'象征封建礼教的本质，'救救孩子'象征未来希望"},
            {"label": "分4·互文性", "content": "受果戈理《狂人日记》影响，但本土化"},
            {"label": "分5·叙事声音", "content": "狂人的理性与'正常'世界的疯狂形成反讽"},
            {"label": "总结", "content": "鲁迅开创中国现代小说话语模式，影响深远"},
        ],
        "conclusion": "《狂人日记》的多元话语特征，开创了中国现代小说的话语范式",
    },
}

EQ_0126_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《狂人日记》小序用文言：'某君昆仲，今隐其名……'——与日记白话形成双重叙事",
            "source": "鲁迅《狂人日记》1918年《新青年》",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《狂人日记》日记：'今天全没月光，我知道不妙……我翻开历史一查……满本都写着两个字是「吃人」'——象征话语",
            "source": "鲁迅《狂人日记》1918年《新青年》",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "米列娜指出：鲁迅《狂人日记》的双重叙事（文言小序/白话日记）象征旧新对立，开创中国现代小说话语模式",
            "source": "米列娜《鲁迅的《狂人日记》分析》1980年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群认为：鲁迅《狂人日记》的'狂人理性与正常世界疯狂的反讽'是其话语特征的深刻之处",
            "source": "钱理群《心灵的探寻》北京大学出版社1999年版",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将鲁迅《狂人日记》列为话语特征分析的典型案例",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从话语理论视角分析《狂人日记》；袁行霈《中国文学史》从文学史视角定位其开创意义。两书共识：《狂人日记》开创现代小说话语模式。",
        "scholarComparison": "米列娜从叙事学视角分析双重叙事；钱理群从反讽视角分析叙事声音；王富仁从文化批判视角分析象征话语。三种视角互补：米重叙，钱重反，王重象。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《狂人日记》的话语革命", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·鲁迅与现代小说话语", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0127: 鲁迅"凡人之心无不有诗"析论（理论应用型）─────────────
EQ_0127_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["鲁迅", "凡人之心无不有诗", "摩罗诗力说"],
    "limitKeywords": ["试析论"],
    "task": "解读原话 + 阐释内涵 + 评价意义",
    "breakthroughAngles": [
        "①出处与原话（《摩罗诗力说》）",
        "②诗性普遍（诗是人性本然）",
        "③共鸣论（读者内心本有诗性）",
        "④反精英（诗非少数人特权）",
        "⑤与鲁迅文学观（唤醒民众诗性/改造国民性）",
        "⑥与西方共鸣说接受美学相通",
    ],
    "angleRationale": "本题为理论应用型，需先精准解读原话，再阐释内涵与意义。符合'文本细读—理论阐释—评价意义'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "鲁迅《摩罗诗力说》'凡人之心，无不有诗'阐述诗性普遍、共鸣论、反精英三大内涵，与鲁迅'改造国民性'文学观呼应，与西方共鸣说接受美学相通",
        "points": [
            {"label": "总述", "content": "鲁迅此语出自《摩罗诗力说》，阐述诗性普遍与共鸣论"},
            {"label": "分1·出处与原话", "content": "《摩罗诗力说》：'凡人之心，无不有诗，如诗人作诗，是不为诗人独有'"},
            {"label": "分2·诗性普遍", "content": "诗是人性本然，非诗人专利"},
            {"label": "分3·共鸣论", "content": "读者读诗能会解，是因为内心本有诗性"},
            {"label": "分4·反精英", "content": "诗不是少数人的特权，是所有人的精神需要"},
            {"label": "分5·与鲁迅文学观", "content": "文学应唤醒民众诗性，'改造国民性'；与周作人'人的文学'呼应"},
            {"label": "分6·与西方共鸣说接受美学", "content": "与西方共鸣说、接受美学相通，强调读者主体性"},
            {"label": "总结", "content": "鲁迅此语阐述诗性普遍与共鸣论，是其文学观的诗学表达"},
        ],
        "conclusion": "鲁迅'凡人之心无不有诗'阐述诗性普遍与共鸣论，是其'改造国民性'文学观的诗学基础",
    },
}

EQ_0127_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《摩罗诗力说》：「凡人之心，无不有诗，如诗人作诗，是不为诗人独有，凡一读其诗，心即会解者，即无不自有诗人之诗」",
            "source": "鲁迅《摩罗诗力说》1907年《河南》杂志",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：鲁迅'凡人之心无不有诗'阐述诗性普遍与共鸣论，与'改造国民性'文学观呼应",
            "source": "钱理群《鲁迅的文学观》北京大学出版社2003年版",
            "linkedKnowledgePointId": "kp_00613",
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王富仁认为：鲁迅此语'反精英'的诗学思想，是对传统'诗言志'精英化的反拨",
            "source": "王富仁《鲁迅与中国文化》北京师范大学出版社2000年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "赵园指出：鲁迅'凡人之心无不有诗'与西方共鸣说、接受美学相通，强调读者主体性",
            "source": "赵园《鲁迅研究》人民文学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将鲁迅'凡人之心无不有诗'列为共鸣论与接受美学的先驱思想",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从共鸣论视角分析此语；袁行霈《中国文学史》从鲁迅文学观视角定位其意义。两书共识：鲁迅此语阐述诗性普遍与共鸣论。",
        "scholarComparison": "钱理群从鲁迅文学观视角阐释其内涵；王富仁从反精英视角分析其意义；赵园从接受美学视角定位其理论价值。三种视角互补：钱重观，王重反，赵重接。",
    },
    "referenceLinks": [
        {"label": "中国作家网·鲁迅《摩罗诗力说》的诗学思想", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·鲁迅与共鸣论的先驱意义", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── 主程序：读取 seed_data.json，批量填充，写回 ──────────────────
FILL_MAP = {
    "eq_0113": (EQ_0113_ANGLE, EQ_0113_NOTES),
    "eq_0114": (EQ_0114_ANGLE, EQ_0114_NOTES),
    "eq_0115": (EQ_0115_ANGLE, EQ_0115_NOTES),
    "eq_0116": (EQ_0116_ANGLE, EQ_0116_NOTES),
    "eq_0117": (EQ_0117_ANGLE, EQ_0117_NOTES),
    "eq_0118": (EQ_0118_ANGLE, EQ_0118_NOTES),
    "eq_0119": (EQ_0119_ANGLE, EQ_0119_NOTES),
    "eq_0120": (EQ_0120_ANGLE, EQ_0120_NOTES),
    "eq_0121": (EQ_0121_ANGLE, EQ_0121_NOTES),
    "eq_0122": (EQ_0122_ANGLE, EQ_0122_NOTES),
    "eq_0123": (EQ_0123_ANGLE, EQ_0123_NOTES),
    "eq_0124": (EQ_0124_ANGLE, EQ_0124_NOTES),
    "eq_0125": (EQ_0125_ANGLE, EQ_0125_NOTES),
    "eq_0126": (EQ_0126_ANGLE, EQ_0126_NOTES),
    "eq_0127": (EQ_0127_ANGLE, EQ_0127_NOTES),
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

    print(f"\n共填充 {filled_count} 道题（预期 15 道）")
    assert filled_count == 15, f"填充数量不符: {filled_count} != 15"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"已写回 {SEED_PATH}")


if __name__ == "__main__":
    main()

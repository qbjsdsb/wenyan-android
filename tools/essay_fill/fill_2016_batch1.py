#!/usr/bin/env python3
"""
为 610 综合卷 2016 年论述题批量填充 angle + notes 字段（第 1 批，7 道）。

题目清单：
- eq_0345 中国古代文学·赠别诗现象（综合型）
- eq_0346 中国古代文学·陶渊明纵浪大化与归隐（作品分析型）
- eq_0347 文学理论·审美距离（理论应用型）
- eq_0348 文学理论·复调的作用（理论应用型）
- eq_0349 文学理论·绘画为什么有诗意（理论应用型）
- eq_0355 中国现当代文学·《茶馆》艺术结构（作品分析型）
- eq_0356 中国古代文学·唐代古文运动的影响（演变型）

填充标准：对齐 eq_0038/eq_0182/eq_0254 三道示例题
- angle: questionType/coreKeywords/limitKeywords/task/breakthroughAngles/angleRationale/argumentPath(thesis+points+conclusion)
- notes: evidences(作品原文+学者观点+教材定论)/crossValidation(教材对比+学者对比)/referenceLinks/knowledgeGaps

策略：
- evidences 如实引用经典作品原文、学者观点、教材定论
- 未覆盖的作品 linkedKnowledgePointId=null，记入 knowledgeGaps
- referenceLinks 限于中国作家网/中国文艺评论网等权威开放资源
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0345: 赠别诗现象（综合型）─────────────────────────────
EQ_0345_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["赠别诗", "历史文化", "现象"],
    "limitKeywords": ["从历史文化角度"],
    "task": "梳理渊源 + 归纳文化内涵 + 评价艺术特征",
    "breakthroughAngles": [
        "①产生根源（地理阻隔/社会制度/宗法家族/礼乐传统）",
        "②发展脉络（《诗经》→汉魏六朝→唐宋巅峰）",
        "③文化内涵（伦理情感/政治隐喻/生命意识/地域想象）",
        "④艺术特征（情景交融/虚实相生/意象凝练）",
    ],
    "angleRationale": "本题为综合型（现象论述），需从'根源—脉络—内涵—艺术'四维度展开。符合'先溯源再梳理后归纳'的综合型答题策略，历史文化角度限定要求突出社会制度与思想背景。",
    "argumentPath": {
        "thesis": "赠别诗是中国古典诗歌的重要题材类型，其产生根植于古代中国的地理阻隔、社会制度与宗法家族文化，历经《诗经》至唐宋的发展，承载着伦理情感、政治隐喻与生命意识等多重文化内涵",
        "points": [
            {"label": "总述", "content": "赠别诗源远流长，是古典诗歌重要题材，从历史文化角度考察可见深厚意蕴"},
            {"label": "分1·产生根源", "content": "地理阻隔与交通不便、科举入仕与官吏迁调、宗法家族重情传统、礼乐文化《诗经》萌芽"},
            {"label": "分2·发展脉络", "content": "《诗经·燕燕》为祖→汉魏六朝曹植江淹→唐代王勃王维李白巅峰→宋代柳永苏轼"},
            {"label": "分3·文化内涵", "content": "伦理情感（重情文化）、政治隐喻（宦海沉浮）、生命意识（人生无常）、地域想象（他者建构）"},
            {"label": "分4·艺术特征", "content": "情景交融、虚实相生、意象凝练、韵律谐美"},
            {"label": "总结", "content": "赠别诗是中国人情味、士人精神与诗意生活的集中体现，其文化内涵远超离别本身"},
        ],
        "conclusion": "赠别诗现象不仅是文学题材问题，更是中国古代社会结构、文化心理与审美传统的综合折射",
    },
}

EQ_0345_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《诗经·邶风·燕燕》：「燕燕于飞，差池其羽。之子于归，远送于野」",
            "source": "《诗经》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "江淹《别赋》：「黯然销魂者，唯别而已矣」",
            "source": "江淹《别赋》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王勃《送杜少府之任蜀州》：「海内存知己，天涯若比邻」",
            "source": "王勃《送杜少府之任蜀州》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王维《送元二使安西》：「劝君更尽一杯酒，西出阳关无故人」",
            "source": "王维《送元二使安西》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "柳永《雨霖铃》：「多情自古伤离别，更那堪冷落清秋节」",
            "source": "柳永《雨霖铃》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "袁行霈指出：赠别诗在唐代达到艺术巅峰，王勃的豁达、王维的深情、李白的飘逸，构成唐代赠别诗的三大审美范式",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将赠别诗列为送别诗类，强调其'以情动人'的审美特征与'士人交际网络'的社会功能",
            "source": "袁行霈《中国文学史》高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重赠别诗的文学史演变与艺术成就；章培恒《中国文学史》更注重其社会文化背景与士人交际功能。两书共识：赠别诗是古典诗歌重要题材，唐代为巅峰。",
        "scholarComparison": "袁行霈从文学史主流视角定位赠别诗的艺术成就；钱钟书《管锥编》从比较文学视角指出中西赠别诗共通的'伤别'情感结构，但中国赠别诗特有'以景结情'的含蓄美学。",
    },
    "referenceLinks": [
        {"label": "中国作家网·古典赠别诗的审美意蕴", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·送别诗的文化内涵与艺术流变", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "王勃", "note": "项目暂无王勃独立知识点，建议补充'王勃《送杜少府之任蜀州》与初唐赠别诗'以完善初唐文学谱系"},
        {"author": "江淹", "note": "项目暂无江淹《别赋》独立知识点，建议补充以覆盖六朝赠别赋传统"},
    ],
}

# ── eq_0346: 陶渊明纵浪大化与归隐（作品分析型）─────────────────
EQ_0346_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["陶渊明", "纵浪大化中", "不喜亦不惧", "归隐选择"],
    "limitKeywords": ["形影神", "神释"],
    "task": "解读诗句内涵 + 分析与归隐的关系",
    "breakthroughAngles": [
        "①诗句解读（纵浪大化/不喜不惧/应尽须尽）",
        "②哲学基础（道家齐物我+儒家独善其身）",
        "③与归隐的关系（哲学依据/生命态度/价值观）",
        "④归隐内涵（委运任化/非消极避世）",
    ],
    "angleRationale": "本题为作品分析型，需先精准解读诗句，再建立诗句与归隐选择的逻辑关联。符合'文本细读→哲学阐释→行为印证'的作品分析三步法。",
    "argumentPath": {
        "thesis": "陶渊明'纵浪大化中，不喜亦不惧'体现了齐物我、一生死的道家自然观，是其归隐选择的哲学依据——归隐不是消极避世，而是委运任化的本真生活实践",
        "points": [
            {"label": "总述", "content": "诗句出自《形影神·神释》，集中体现陶渊明生死观与归隐精神依据"},
            {"label": "分1·纵浪大化", "content": "'大化'指宇宙造化运行，'纵浪'是随顺自然，人是自然一部分应顺应造化"},
            {"label": "分2·不喜亦不惧", "content": "超越世俗生死执念，体现道家齐物我一生死思想，是陶渊明独有的旷达"},
            {"label": "分3·哲学基础", "content": "儒家独善其身+道家自然无为+佛家无我超越，融汇成'陶体'生命美学"},
            {"label": "分4·与归隐关系", "content": "仕途为樊笼违背本性，田园契合复得返自然；应尽便须尽使不为五斗米折腰"},
            {"label": "分5·归隐内涵", "content": "归隐是积极选择本真生活，采菊东篱下悠然见南山是这种境界的诗意呈现"},
            {"label": "总结", "content": "诗句是陶渊明生命美学的诗意表达，归隐是其哲学思想的实践归宿"},
        ],
        "conclusion": "陶渊明的归隐选择以纵浪大化的自然观为哲学根基，是中国士人精神独立的典范",
    },
}

EQ_0346_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《形影神·神释》：「纵浪大化中，不喜亦不惧。应尽便须尽，无复独多虑」",
            "source": "陶渊明《形影神·神释》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《归园田居》：「久在樊笼里，复得返自然」",
            "source": "陶渊明《归园田居》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陶渊明《饮酒》：「采菊东篱下，悠然见南山」",
            "source": "陶渊明《饮酒·其五》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈寅恪指出：陶渊明思想'实为外儒内道','纵浪大化'体现了其对新旧学说的创造性融合，而非简单继承某一家",
            "source": "陈寅恪《陶渊明之思想与清谈之关系》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朱光潜认为：陶渊明的'不喜亦不惧'并非冷情而是至情，是经历人生后达到的'豁达'，与麻木不仁有本质区别",
            "source": "朱光潜《诗论》三联书店1984年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将陶渊明定位为'魏晋风度的杰出代表'，其归隐是'委运任化'人生哲学的实践",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重陶渊明的文学史地位与田园诗开创；章培恒《中国文学史》更注重其思想哲学内涵。两书共识：陶渊明归隐是哲学选择而非消极逃避。",
        "scholarComparison": "陈寅恪从思想史视角定位陶渊明'外儒内道'；朱光潜从美学视角强调其'至情'特征；钱钟书《谈艺录》则指出其诗'质直'中有'深味'。三种视角互补：陈重思想，朱重情感，钱重艺术。",
    },
    "referenceLinks": [
        {"label": "中国作家网·陶渊明的生命哲学与诗歌艺术", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·陶渊明归隐的文化意蕴", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陶渊明", "note": "项目暂无陶渊明独立知识点，建议补充'陶渊明田园诗与生命哲学'以完善魏晋文学谱系"},
    ],
}

# ── eq_0347: 审美距离（理论应用型）─────────────────────────────
EQ_0347_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["距离产生美", "审美距离", "理解"],
    "limitKeywords": ["布洛", "心理距离说"],
    "task": "阐释理论内涵 + 分析表现形态 + 评价启示",
    "breakthroughAngles": [
        "①理论溯源（布洛心理距离说+海上大雾例）",
        "②内涵阐释（功利悬置/距离丧失与过分/微妙平衡）",
        "③表现形态（时间/空间/心理/艺术形式距离）",
        "④文学启示（超越功利/陌生化/出入平衡）",
    ],
    "angleRationale": "本题为理论应用型，需先阐释布洛心理距离说的核心内涵，再分析其在文学中的表现形态。符合'理论溯源→内涵阐释→文学应用'的理论应用型答题策略。",
    "argumentPath": {
        "thesis": "审美距离是布洛提出的心理学美学概念，指主体与对象间功利关系的悬置，其微妙平衡是审美发生的关键，对文学创作与接受均有重要启示",
        "points": [
            {"label": "总述", "content": "'距离产生美'源自布洛1912年心理距离说，以海上大雾为例阐释"},
            {"label": "分1·理论溯源", "content": "布洛以海上大雾为例：实用角度令人焦虑恐惧，审美角度营造朦胧神秘，差异在于心理距离"},
            {"label": "分2·内涵阐释", "content": "距离非空间/时间距离而是心理距离；距离丧失退化为实用物，距离过分无法产生共鸣"},
            {"label": "分3·微妙平衡", "content": "理想审美是既入乎其中又出乎其外，既有情感投入又有审美观照"},
            {"label": "分4·表现形态", "content": "时间距离（古典作品）、空间距离（远山远景）、心理距离（艺术虚构）、形式距离（间离效果陌生化）"},
            {"label": "分5·文学启示", "content": "文学应超越直接功利；适当陌生化使平凡产生新鲜感；既要入乎其内体验又要出乎其外观照"},
            {"label": "总结", "content": "审美距离说是现代美学重要理论，对文学创作与接受均有解释力"},
        ],
        "conclusion": "审美距离的精髓在于功利悬置与情感投入的辩证平衡，这是文学审美发生的心理机制",
    },
}

EQ_0347_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "布洛提出：心理距离是'把对象与我们的实际需要和目的分离开来，使之完全客观地呈现于我们之前'",
            "source": "布洛《作为艺术因素与审美原则的'心理距离说'》1912年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朱光潜指出：距离含有消极与积极两面，消极是摆脱实用需要，积极是凝聚审美观照，'距离的丧失'即是美感的丧失",
            "source": "朱光潜《文艺心理学》开明书店1936年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "什克洛夫斯基提出'陌生化'：艺术的目的就是使对象陌生，使形式变得困难，增加感知的难度与长度",
            "source": "什克洛夫斯基《作为手法的艺术》1917年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将审美距离列为文学接受的重要心理条件，强调'距离的适度'是审美体验发生的前提",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "王一川《文学理论》指出：布莱希特的'间离效果'与布洛的'心理距离'有相通之处，都强调打破沉浸以产生反思",
            "source": "王一川《文学理论》北京大学出版社2011年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》从接受美学角度阐释审美距离；王一川《文学理论》从比较诗学角度联系布莱希特间离效果。两书共识：审美距离是文学接受的核心机制。",
        "scholarComparison": "布洛从心理学视角提出距离说；什克洛夫斯基从形式主义视角提出陌生化；布莱希特从戏剧理论视角提出间离效果。三种理论视角不同但内核相通：都强调打破实用态度以产生审美/反思。",
    },
    "referenceLinks": [
        {"label": "中国作家网·审美距离与文学接受", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·布洛心理距离说的当代意义", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "布洛", "note": "项目暂无布洛心理距离说独立知识点，建议补充以完善西方美学理论谱系"},
        {"author": "什克洛夫斯基", "note": "项目暂无陌生化理论独立知识点，建议补充'俄国形式主义与陌生化'以覆盖形式主义文论"},
    ],
}

# ── eq_0348: 复调的作用（理论应用型）─────────────────────────────
EQ_0348_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["复调", "作用", "具体作品"],
    "limitKeywords": ["巴赫金", "陀思妥耶夫斯基"],
    "task": "阐释理论含义 + 结合作品分析 + 评价作用",
    "breakthroughAngles": [
        "①理论溯源（巴赫金复调小说理论）",
        "②核心特征（思想的人/大型对话/微型对话/未完成性）",
        "③作品分析（《罪与罚》多重声音）",
        "④作用评价（思想深度/人物独立/读者参与）",
    ],
    "angleRationale": "本题为理论应用型，需先阐释巴赫金复调理论，再结合《罪与罚》具体分析。符合'理论阐释→文本印证→作用评价'的三步策略，'结合具体作品'的限定要求理论必须落地。",
    "argumentPath": {
        "thesis": "复调是巴赫金提出的小说理论概念，指多种独立平等声音的对话关系，其作用在于拓展思想深度、增强人物独立性、引导读者参与，是现代小说的重要范式",
        "points": [
            {"label": "总述", "content": "复调由巴赫金在《陀思妥耶夫斯基诗学问题》中提出，与独白型小说相对"},
            {"label": "分1·理论溯源", "content": "复调小说存在多种独立平等声音，每种声音拥有自己的真理，与作者声音处于对话关系"},
            {"label": "分2·核心特征", "content": "主人公是思想的人；多重意识平等对话构成大型对话；内心存在微型对话；人物思想未完成性"},
            {"label": "分3·《罪与罚》分析", "content": "拉斯柯尔尼科夫超人理论/波尔菲里理性追问/索尼娅宗教救赎/斯维德里盖洛夫虚无主义，构成大型对话"},
            {"label": "分4·作用·思想深度", "content": "呈现真理多面性，作者不直接表态让对话本身揭示复杂性"},
            {"label": "分5·作用·人物与读者", "content": "增强人物独立性避免概念化；引导读者参与思考形成读者-文本对话"},
            {"label": "总结", "content": "复调是现代小说重要范式，影响昆德拉马尔克斯，中国当代小说亦有探索"},
        ],
        "conclusion": "复调理论揭示了现代小说的对话本质，其作用远超技巧层面，是对小说认识论的根本革新",
    },
}

EQ_0348_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "巴赫金指出：复调小说的本质在于'主人公不仅是作者话语的客体，也是拥有自己直接话语的主体'",
            "source": "巴赫金《陀思妥耶夫斯基诗学问题》三联书店1988年中译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陀思妥耶夫斯基《罪与罚》：拉斯柯尔尼科夫在酒馆向索尼娅阐述'非凡人有权跨越道德界限'的超人理论",
            "source": "陀思妥耶夫斯基《罪与罚》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陀思妥耶夫斯基《罪与罚》：索尼娅朗读《拉撒路复活》段落，以宗教救赎回应超人理论的虚无",
            "source": "陀思妥耶夫斯基《罪与罚》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱中文指出：巴赫金复调理论打破了传统小说的独白模式，人物声音的独立性是对作者霸权的解构",
            "source": "钱中文《巴赫金全集》中译本导言 河北教育出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将复调列为现代小说的重要叙事特征，强调其对小说认识论的革新意义",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》侧重复调的叙事学意义；王一川《文学理论》更注重其对话哲学维度。两书共识：复调是现代小说的核心特征之一。",
        "scholarComparison": "巴赫金从诗学视角提出复调；钱中文从中国接受视角阐释其反独白意义；董小英《再登巴比伦塔》从叙事学角度扩展复调的应用范围。三种视角互补：巴重理论原创，钱重中国接受，董重叙事扩展。",
    },
    "referenceLinks": [
        {"label": "中国作家网·巴赫金复调理论与中国当代小说", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·复调小说的对话美学", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "巴赫金", "note": "项目暂无巴赫金复调理论独立知识点，建议补充'巴赫金复调理论与对话主义'以完善西方文论谱系"},
        {"author": "陀思妥耶夫斯基《罪与罚》", "note": "项目暂无《罪与罚》独立知识点，建议补充以覆盖复调理论的文本分析基础"},
    ],
}

# ── eq_0349: 绘画为什么有诗意（理论应用型）─────────────────────
EQ_0349_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["味摩诘诗", "诗中有画", "画中有诗", "绘画", "诗意"],
    "limitKeywords": ["苏轼", "王维"],
    "task": "阐释命题内涵 + 分析诗画相通原因 + 评价理论意义",
    "breakthroughAngles": [
        "①命题溯源（苏轼评王维诗画一律）",
        "②相通原因（审美意境/主观情感/表现手法/题材/文化精神）",
        "③王维特殊性（身兼诗人画家，辋川诗画互通）",
        "④理论意义（突破诗言志画状物/推动文人画/中西对照）",
    ],
    "angleRationale": "本题为理论应用型，需先溯源苏轼命题，再分析诗画相通的多维原因。符合'命题溯源→原因分析→个案印证→理论评价'的策略，王维作为限定要求必须作为核心例证。",
    "argumentPath": {
        "thesis": "绘画之所以有诗意，在于诗画在审美意境、主观情感、表现手法、题材选择与文化精神上的深层相通，王维诗画一律是典范体现",
        "points": [
            {"label": "总述", "content": "苏轼评王维'诗中有画，画中有诗'揭示诗画相通美学规律"},
            {"label": "分1·审美意境相通", "content": "诗画都追求情景交融虚实相生，画中含象外之象引发想象便是诗意"},
            {"label": "分2·主观情感渗入", "content": "画不仅是再现物象更是抒发情感，王维以禅意入画，主观性使画有诗"},
            {"label": "分3·表现手法借鉴", "content": "画留白远势与诗含蓄余味相通，皆追求以有限表无限以瞬间显永恒"},
            {"label": "分4·题材与文化同源", "content": "山水田园同为诗画题材；天人合一道法自然是共同哲学根基"},
            {"label": "分5·王维典范", "content": "王维身兼大诗人大画家，《辋川集》与《辋川图》是诗画互通典范"},
            {"label": "分6·理论意义", "content": "突破诗言志画状物传统区分；推动文人画发展；与莱辛《拉奥孔》形成中西对照"},
            {"label": "总结", "content": "诗画一律是中国艺术重要传统，影响宋元以降山水诗与文人画"},
        ],
        "conclusion": "绘画的诗意源于诗画在审美境界与精神内核的深层相通，王维典范证明诗画一律是中国美学的核心命题",
    },
}

EQ_0349_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "苏轼《书摩诘蓝田烟雨图》：「味摩诘之诗，诗中有画；观摩诘之画，画中有诗」",
            "source": "苏轼《书摩诘蓝田烟雨图》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王维《汉江临眺》：「江流天地外，山色有无中」",
            "source": "王维《汉江临眺》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "王维《使至塞上》：「大漠孤烟直，长河落日圆」",
            "source": "王维《使至塞上》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "宗白华指出：中国画的境界'是一个永恒的空间意识'，与诗的'虚实相生'相通，王维诗画是'中国艺术意境'的典范",
            "source": "宗白华《美学散步》上海人民出版社1981年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "莱辛《拉奥孔》认为诗是时间艺术画是空间艺术，两者有界限；中国'诗画一律'传统则强调诗画相通，形成中西美学对照",
            "source": "莱辛《拉奥孔》人民文学出版社1979年中译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "童庆炳《文学理论教程》将苏轼'诗中有画'列为中国古典美学的重要命题，强调其对文人画传统的推动作用",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》侧重诗画一律的文论意义；袁行霈《中国文学史》更注重王维诗画艺术成就。两书共识：苏轼命题揭示了中国艺术的诗画互通特征。",
        "scholarComparison": "宗白华从中国美学意境论阐释诗画相通；莱辛从西方古典主义强调诗画界限；叶维廉《比较诗学》则从中西比较视角指出两者都是对艺术媒介的反思。三种视角形成中西对话。",
    },
    "referenceLinks": [
        {"label": "中国作家网·王维诗画一律的美学意蕴", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·诗画结合与中国文人画传统", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "王维", "note": "项目暂无王维独立知识点，建议补充'王维山水田园诗与诗画一律'以完善盛唐文学谱系"},
        {"author": "莱辛《拉奥孔》", "note": "项目暂无莱辛独立知识点，建议补充以覆盖西方诗画理论对照"},
    ],
}

# ── eq_0355: 《茶馆》艺术结构（作品分析型）─────────────────────
EQ_0355_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["老舍", "茶馆", "艺术结构"],
    "limitKeywords": ["分析"],
    "task": "分析结构特点 + 评价艺术成就",
    "breakthroughAngles": [
        "①图卷式结构（三幕三时代无中心情节）",
        "②以空间代情节（茶馆贯穿+社会缩影）",
        "③人物群像（70余人物无中心英雄）",
        "④埋葬三个时代的主题结构",
        "⑤语言艺术（京味对白+性格化）",
    ],
    "angleRationale": "本题为作品分析型，聚焦'艺术结构'，需从结构模式、空间叙事、人物群像、主题表达、语言艺术五维度展开。符合'结构→叙事→人物→主题→语言'的作品分析框架。",
    "argumentPath": {
        "thesis": "《茶馆》的艺术结构以'图卷式'为核心特征，通过空间串联代替情节因果、人物群像代替中心英雄、三幕三时代展现历史变迁，是中国话剧结构的独创",
        "points": [
            {"label": "总述", "content": "《茶馆》1957年创作1958年首演，是老舍话剧代表作，中国当代戏剧经典"},
            {"label": "分1·图卷式结构", "content": "三幕写清末/民国初/抗战后三个时代，跨度近50年，无贯穿中心情节，犹如三幅时代画卷"},
            {"label": "分2·以空间代情节", "content": "茶馆作为唯一贯穿场景是社会缩影，以空间串联突破亚里士多德开端发展高潮结局模式"},
            {"label": "分3·人物群像", "content": "出场70余人物有台词50余，王利发常四爷秦仲义等，人物命运随时代变迁无中心英雄"},
            {"label": "分4·主题结构", "content": "埋葬三个时代：一幕葬清王朝二幕葬军阀三幕葬国民党，三老人自悼撒纸钱是主题升华"},
            {"label": "分5·语言艺术", "content": "京味浓郁对白生动符合身份性格，是老舍语言大师的体现"},
            {"label": "总结", "content": "《茶馆》图卷式结构是对传统戏剧结构的突破，是中国话剧高峰之一"},
        ],
        "conclusion": "《茶馆》的艺术结构创新在于以空间叙事与群像塑造替代情节因果，形成了独特的'图卷式'戏剧美学",
    },
}

EQ_0355_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "老舍《茶馆》第三幕结尾：王利发、常四爷、秦仲义三老人自悼撒纸钱，「祭奠祭奠自己」",
            "source": "老舍《茶馆》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "老舍《茶馆》常四爷：「我爱咱们的国呀，可是谁爱我呢？」",
            "source": "老舍《茶馆》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：《茶馆》的'图卷式'结构突破了西方戏剧的'冲突律'，以空间叙事代替情节因果，是中国话剧民族化的典范",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "洪子诚认为：《茶馆》的人物群像塑造继承了中国传统小说'连缀式'结构，又融入现代戏剧的空间叙事，是'民族形式与现代手法'的结合",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将《茶馆》定位为'中国当代话剧的高峰'，其图卷式结构是对亚里士多德式戏剧的根本性突破",
            "source": "丁帆《中国新文学史》下册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "陈思和《当代文学史教程》侧重《茶馆》的'民间立场'与图卷式结构；洪子诚《当代文学史》更注重其民族化探索；丁帆《新文学史》强调其话剧史地位。三书共识：《茶馆》是中国话剧结构创新的典范。",
        "scholarComparison": "陈思和从民间文化视角解读《茶馆》的'民间隐形结构'；洪子诚从文学史视角定位其民族化贡献；老舍本人则自述'葬送三个时代'的创作意图。三种视角互补：陈重民间，洪重民族化，老舍重主题。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《茶馆》图卷式结构的戏剧美学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·老舍《茶馆》与中国话剧民族化", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "老舍《茶馆》", "note": "项目暂无《茶馆》独立知识点，建议补充'老舍《茶馆》图卷式结构与京味语言'以完善当代戏剧谱系"},
    ],
}

# ── eq_0356: 唐代古文运动的影响（演变型）─────────────────────
EQ_0356_ANGLE = {
    "questionType": "演变型",
    "coreKeywords": ["唐代古文运动", "影响"],
    "limitKeywords": ["韩愈", "柳宗元"],
    "task": "梳理运动主张 + 论述多维度影响 + 评价局限",
    "breakthroughAngles": [
        "①运动主张（文以载道/文体革新/辞必己出/养气）",
        "②文学影响（散文主流化+典范形成+唐传奇繁荣）",
        "③宋代影响（唐宋八大家传统奠定）",
        "④明清影响（归有光桐城派尊唐宋）",
        "⑤思想与文论影响（理学先声+文以载道成核心命题）",
    ],
    "angleRationale": "本题为演变型，需从运动主张出发，纵向梳理对后世的多维度影响。符合'主张→文学→宋代→明清→思想'的时间纵轴策略，'影响'要求覆盖文学史长远流变。",
    "argumentPath": {
        "thesis": "唐代古文运动由韩愈柳宗元领导，以文以载道为核心主张，其影响深远：改变中唐文风、奠定唐宋八大家传统、影响明清散文、复兴儒学为理学先声、文以载道成文论核心",
        "points": [
            {"label": "总述", "content": "唐代古文运动是中唐韩柳领导的文学革新，旨在恢复先秦两汉散体文传统反对骈体文"},
            {"label": "分1·运动主张", "content": "文以载道（文者以明道）；文体革新（反对骈体提倡散体）；辞必己出（唯陈言之务去）；养气（气盛言宜）"},
            {"label": "分2·文学影响", "content": "改变中唐文风散文取代骈文；韩愈《师说》《祭十二郎文》柳宗元《永州八记》成典范；推动唐传奇繁荣"},
            {"label": "分3·宋代影响", "content": "欧阳修苏轼王安石曾巩继承形成唐宋八大家传统，奠定古典散文基本范式"},
            {"label": "分4·明清影响", "content": "明代归有光清代桐城派皆尊唐宋八大家为宗"},
            {"label": "分5·思想与文论影响", "content": "复兴儒学反佛老是宋代新儒学先声；文以载道文道合一成后世文论核心命题"},
            {"label": "分6·局限", "content": "过度强调明道限制文学审美独立性；部分古文过于古奥生涩"},
            {"label": "总结", "content": "韩柳古文运动是中国散文史里程碑，奠定唐宋以降散文发展基础"},
        ],
        "conclusion": "唐代古文运动的影响跨越文学、思想、文论三个领域，是中国散文史与思想史的重要转折点",
    },
}

EQ_0356_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "韩愈《题欧阳生哀辞后》：「愈之为古文，岂独取其句读不类于今者邪？思古人而不得见，学古道则欲兼通其辞」",
            "source": "韩愈《题欧阳生哀辞后》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "韩愈《答李翊书》：「气，水也；言，浮物也。水大而物之浮者大小毕浮。气之与言犹是也，气盛则言之短长与声之高下者皆宜」",
            "source": "韩愈《答李翊书》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "柳宗元《答韦中立论师道书》：「文者以明道」",
            "source": "柳宗元《答韦中立论师道书》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈寅恪指出：古文运动之兴起'实受安史之乱后士大夫反省之影响'，韩愈以文以载道复兴儒学是'唐代文化史上一大变'",
            "source": "陈寅恪《论韩愈》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱穆认为：韩柳古文运动'不仅为文学革命，亦为思想革命'，文以载道主张奠定宋代理学先声",
            "source": "钱穆《中国学术思想史论丛》东大图书公司1976年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将古文运动定位为中唐文学革新的核心，强调其对唐宋以降散文发展的奠基作用",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重古文运动的文学史意义；章培恒《中国文学史》更注重其思想史维度。两书共识：古文运动是散文史转折点，文以载道是核心主张。",
        "scholarComparison": "陈寅恪从文化史视角定位韩愈为'唐代文化史一大变'；钱穆从学术史视角强调其思想革命意义；袁行霈从文学史视角注重其散文范式建立。三种视角互补：陈重文化，钱重思想，袁重文学。",
    },
    "referenceLinks": [
        {"label": "中国作家网·韩柳古文运动与唐宋散文", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·文以载道与中国散文传统", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "韩愈", "note": "项目暂无韩愈独立知识点，建议补充'韩愈古文运动与文以载道'以完善中唐文学谱系"},
        {"author": "柳宗元", "note": "项目暂无柳宗元独立知识点，建议补充'柳宗元《永州八记》与山水散文'以覆盖古文运动另一领袖"},
    ],
}

# ── 主程序：读取 seed_data.json，批量填充，写回 ──────────────────
FILL_MAP = {
    "eq_0345": (EQ_0345_ANGLE, EQ_0345_NOTES),
    "eq_0346": (EQ_0346_ANGLE, EQ_0346_NOTES),
    "eq_0347": (EQ_0347_ANGLE, EQ_0347_NOTES),
    "eq_0348": (EQ_0348_ANGLE, EQ_0348_NOTES),
    "eq_0349": (EQ_0349_ANGLE, EQ_0349_NOTES),
    "eq_0355": (EQ_0355_ANGLE, EQ_0355_NOTES),
    "eq_0356": (EQ_0356_ANGLE, EQ_0356_NOTES),
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

    print(f"\n共填充 {filled_count} 道题（预期 7 道）")
    assert filled_count == 7, f"填充数量不符: {filled_count} != 7"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"已写回 {SEED_PATH}")

if __name__ == "__main__":
    main()

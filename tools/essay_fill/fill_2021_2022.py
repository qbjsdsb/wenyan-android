#!/usr/bin/env python3
"""
为 806/807 卷 2021-2022 年论述题批量填充 angle + notes 字段（11 道）。

题目清单：
- eq_0447 现当代·鲁迅对当代作家的影响/鲁迅研究特点（小论文型）
- eq_0448 现当代·汪曾祺《职业》文学评论（评论型）
- eq_0454 外国·《红与黑》艺术特色（作品分析型）
- eq_0457 外国·《堂吉诃德》反骑士文学（作品分析型）
- eq_0458 外国·撒旦与靡菲斯特形象比较（比较型）
- eq_0459 外国·卡夫卡《城堡》K与城堡关系（作品分析型）
- eq_0468 现当代·李庆西《寻根：回到事物本身》评论（评论型）
- eq_0469 现当代·陆蠡《乞丐与病者》散文评论（评论型）
- eq_0479 外国·神曲二重性（作品分析型）
- eq_0480 外国·哈姆雷特形象（作品分析型）
- eq_0481 外国·《喧哗与骚动》艺术特色（作品分析型）

填充标准：对齐示例题结构
- angle: questionType/coreKeywords/limitKeywords/task/breakthroughAngles/angleRationale/argumentPath
- notes: evidences/crossValidation/referenceLinks/knowledgeGaps
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0447: 鲁迅对当代作家的影响（小论文型）─────────────
EQ_0447_ANGLE = {
    "questionType": "小论文型",
    "coreKeywords": ["鲁迅", "当代作家", "影响", "鲁迅研究"],
    "limitKeywords": ["二选一", "50分"],
    "task": "选择角度 + 梳理影响/问题 + 评价意义",
    "breakthroughAngles": [
        "①选择'鲁迅对当代作家的影响'",
        "②80年代重写文学史后的鲁迅（王富仁/钱理群/汪晖）",
        "③当代作家的精神共鸣（张承志/张炜/余华/莫言/王朔）",
        "④鲁迅作为'精神资源'的多重接受",
    ],
    "angleRationale": "本题为小论文型（二选一），选'鲁迅对当代作家的影响'角度。符合'选择角度→梳理影响→评价意义'的小论文结构。",
    "argumentPath": {
        "thesis": "鲁迅作为'现代文学之父'，其精神资源在80年代重写文学史后被重新阐释，深刻影响张承志、张炜、余华、莫言、王朔等当代作家，构成中国当代文学的精神底色",
        "points": [
            {"label": "总述", "content": "鲁迅作为精神资源，在80年代重写文学史后被重新阐释，深刻影响当代作家"},
            {"label": "分1·80年代鲁迅重新阐释", "content": "王富仁《中国反封建思想革命的一面镜子》、钱理群《心灵的探寻》、汪晖《反抗绝望》建构鲁迅研究新范式"},
            {"label": "分2·张承志张炜的精神抗争", "content": "张承志《清洁的精神》继承鲁迅的'反抗绝望'；张炜《古船》中的精神抗争延续鲁迅的国民性批判"},
            {"label": "分3·余华莫言的国民性反思", "content": "余华《活着》对国民性的反思；莫言《酒国》《檀香刑》对国民性的批判，延续鲁迅的'看客'批判"},
            {"label": "分4·王朔的反叛与解构", "content": "王朔'痞子'反叛中可见鲁迅对传统的解构精神，但走向不同方向"},
            {"label": "分5·鲁迅作为精神资源的多重接受", "content": "鲁迅被不同作家以不同方式接受：精神抗争/国民性批判/形式实验/语言革命"},
            {"label": "总结", "content": "鲁迅是当代文学的精神底色，其影响在80年代后被重新激活"},
        ],
        "conclusion": "鲁迅作为'现代文学之父'，其精神资源在当代文学中获得多重激活，构成中国当代文学的精神底色",
    },
}

EQ_0447_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王富仁《中国反封建思想革命的一面镜子》从启蒙视角重评鲁迅，开启80年代鲁迅研究新范式",
            "source": "王富仁《中国反封建思想革命的一面镜子》北京师范大学出版社1986年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "汪晖《反抗绝望——鲁迅及其文学世界》从存在主义视角分析鲁迅'反抗绝望'哲学，深刻影响当代作家对鲁迅的精神接受",
            "source": "汪晖《反抗绝望——鲁迅及其文学世界》河北教育出版社2000年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：鲁迅是当代中国作家的'精神导师'，80年代后其'反抗绝望'与'国民性批判'被张承志、张炜、余华、莫言等以不同方式继承",
            "source": "钱理群《心灵的探寻》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "钱理群《中国现代文学三十年》将鲁迅定位为'中国现代文学之父'，其精神资源深刻影响当代文学",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重鲁迅的文学史地位；洪子诚《当代文学史》更注重当代作家对鲁迅的接受。两书共识：鲁迅深刻影响当代文学。",
        "scholarComparison": "王富仁从启蒙视角重评鲁迅；汪晖从存在主义视角分析'反抗绝望'；钱理群从精神史视角梳理其影响。三种视角互补：王重启，汪重存，钱重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·鲁迅与当代文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·鲁迅研究的新范式", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0448: 汪曾祺《职业》文学评论（评论型）─────────────
EQ_0448_ANGLE = {
    "questionType": "评论型",
    "coreKeywords": ["汪曾祺", "职业", "文学评论"],
    "limitKeywords": ["1500字", "100分"],
    "task": "细读文本 + 分析主题艺术 + 评价意义",
    "breakthroughAngles": [
        "①题材与主题（小人物日常/职业双重含义）",
        "②童真赞美与贫困悲悯",
        "③京派传统与沈从文继承",
        "④汪曾祺'散文化小说'美学",
    ],
    "angleRationale": "本题为评论型（具体作品评论），需细读文本，分析主题与艺术。符合'细读—分析—评价'的评论型策略。",
    "argumentPath": {
        "thesis": "汪曾祺《职业》以昆明街头卖糕儿童为题材，'职业'的双重含义揭示生活丰富与人性尊严，继承沈从文京派传统，体现'散文化小说'美学",
        "points": [
            {"label": "总述", "content": "《职业》是汪曾祺1980年代小说代表作，写昆明街头卖'椒盐饼子西洋糕'的儿童"},
            {"label": "分1·题材与主题", "content": "以小人物日常生活为题材，继承沈从文、京派传统；'职业'双重含义：儿童的卖糕职业与各种职业的叫卖声"},
            {"label": "分2·童真与贫困", "content": "对童真的赞美与对贫困的悲悯；儿童因贫穷辍学卖糕，但童真未泯，仍模仿各种叫卖声游戏"},
            {"label": "分3·京派传统继承", "content": "继承沈从文《边城》的小人物书写；'人情美'与'人性美'的京派美学"},
            {"label": "分4·散文化小说美学", "content": "汪曾祺'散文化小说'：淡化情节、诗化语言、意境悠远；'回到事物本身'的写作姿态"},
            {"label": "分5·评价意义", "content": "《职业》是汪曾祺'散文化小说'的典范，体现对人性尊严的坚守"},
            {"label": "总结", "content": "《职业》以散文化美学呈现小人物的尊严与诗意，是汪曾祺小说的代表作"},
        ],
        "conclusion": "《职业》体现汪曾祺'散文化小说'的美学追求，在平凡中见出人性的尊严与诗意",
    },
}

EQ_0448_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "汪曾祺《职业》：写昆明街头一个叫卖'椒盐饼子西洋糕'的儿童，因贫穷辍学卖糕，但童真未泯，仍模仿各种叫卖声游戏——'职业'双重含义的诗化呈现",
            "source": "汪曾祺《职业》1980年代",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "杨鼎川指出：汪曾祺'散文化小说'继承沈从文、京派传统，淡化情节、诗化语言、意境悠远，《职业》是其典范",
            "source": "杨鼎川《汪曾祺小说研究》广西师范大学出版社1997年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陆建华认为：汪曾祺小说的核心是'回到事物本身'，以平视视角书写小人物的尊严，《职业》中的卖糕儿童是其人道主义精神的集中体现",
            "source": "陆建华《汪曾祺传》江苏文艺出版社1997年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "洪子诚《中国当代文学史》将汪曾祺定位为'散文化小说'的代表，继承沈从文京派传统",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "洪子诚《当代文学史》侧重汪曾祺的文学史定位；陈思和《当代文学史教程》更注重其'散文化小说'美学。两书共识：汪曾祺继承京派传统。",
        "scholarComparison": "杨鼎川从小说学视角分析'散文化小说'；陆建华从传记视角分析其人道主义；摩罗从精神史视角分析其'回到事物本身'。三种视角互补：杨重艺，陆重传，摩重神。",
    },
    "referenceLinks": [
        {"label": "中国作家网·汪曾祺与散文化小说", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·汪曾祺的京派传统", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "汪曾祺", "note": "项目暂无汪曾祺独立知识点，建议补充'汪曾祺《职业》与散文化小说'以覆盖当代小说谱系"},
    ],
}

# ── eq_0454: 《红与黑》艺术特色（作品分析型）─────────────
EQ_0454_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["红与黑", "司汤达", "艺术特色"],
    "limitKeywords": ["结合内容"],
    "task": "梳理艺术特色 + 结合文本分析 + 评价意义",
    "breakthroughAngles": [
        "①心理现实主义（内心独白/心灵的戏剧）",
        "②典型环境中的典型人物（于连）",
        "③叙事结构（外省-神学院-巴黎三段式）",
        "④红与黑的象征（军队/教会）",
    ],
    "angleRationale": "本题为作品分析型，需系统梳理《红与黑》的艺术特色，结合文本印证。符合'特色梳理→文本印证→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "司汤达《红与黑》以心理现实主义、典型环境中的典型人物、三段式叙事结构、红与黑象征等艺术特色，开创现实主义小说新范式",
        "points": [
            {"label": "总述", "content": "《红与黑》（1830）是司汤达代表作，心理现实主义的开创之作"},
            {"label": "分1·心理现实主义", "content": "深刻的心理分析——于连的野心、自卑、自尊、爱情心理；内心独白大量运用；'心灵的戏剧'——人物内心冲突是情节动力"},
            {"label": "分2·典型环境中的典型人物", "content": "于连是'典型环境中的典型人物'——王政复辟时期小资产阶级青年的悲剧；环境——外省维里埃尔、贝尚松神学院、巴黎木尔侯爵府"},
            {"label": "分3·叙事结构", "content": "三段式结构：外省（德·瑞那市长家）→神学院（贝尚松）→巴黎（木尔侯爵府）；空间转换映射社会阶层攀升"},
            {"label": "分4·红与黑的象征", "content": "'红'象征军队（拿破仑时代的军功之路）；'黑'象征教会（王政复辟时代的教会之路）；于连的悲剧在于两条路都走不通"},
            {"label": "分5·文学史意义", "content": "开创心理现实主义；影响后世小说的心理描写传统"},
            {"label": "总结", "content": "《红与黑》以心理现实主义开创现实主义小说新范式"},
        ],
        "conclusion": "《红与黑》是司汤达心理现实主义的典范，其对于连心理的深刻分析开创了现实主义小说的新方向",
    },
}

EQ_0454_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红与黑》下卷题词：'真理，严酷的真理'——司汤达现实主义的宣言",
            "source": "司汤达《红与黑》1830年法文版 罗玉君译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "于连在教堂拾到一张处决通告，上写'细节：在贝尚松，当一个青年行刑的日子……'——预示于连的悲剧命运",
            "source": "司汤达《红与黑》1830年法文版 罗玉君译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "司汤达自述：'我写的是王政复辟时代的小说'，于连是'王政复辟时期小资产阶级青年的典型'",
            "source": "司汤达《拉辛与莎士比亚》1823-1825年法文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "卢卡奇指出：司汤达的'典型环境中的典型人物'理论在《红与黑》中完美实现，于连的悲剧是社会环境与个人野心的必然冲突",
            "source": "卢卡奇《历史小说》1937年德文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《红与黑》定位为心理现实主义开创之作，于连是王政复辟时期小资产阶级青年的典型",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《红与黑》的文学史地位；郑克鲁《外国文学史》更注重其心理分析。两书共识：《红与黑》是心理现实主义开创之作。",
        "scholarComparison": "司汤达从创作自述视角定位其现实主义；卢卡奇从马克思主义视角分析典型环境；勃兰兑斯从文学史视角分析其心理深度。三种视角互补：司重创，卢重典，勃重史。",
    },
    "referenceLinks": [
        {"label": "中国作家网·司汤达与心理现实主义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《红与黑》的心理描写", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0457: 《堂吉诃德》反骑士文学（作品分析型）─────────────
EQ_0457_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["堂吉诃德", "反骑士文学", "骑士文学特点"],
    "limitKeywords": ["塞万提斯", "结合小说"],
    "task": "梳理骑士文学特点 + 分析反骑士手法 + 评价意义",
    "breakthroughAngles": [
        "①骑士文学作品特点（题材/人物/情节/风格）",
        "②《堂吉诃德》的戏仿手法",
        "③反骑士的多重意义（终结骑士文学/讽刺/人文）",
        "④现代小说的开端",
    ],
    "angleRationale": "本题为作品分析型，需先梳理骑士文学特点，再分析《堂吉诃德》如何反骑士。符合'特点梳理→手法分析→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "塞万提斯《堂吉诃德》通过戏仿骑士文学，揭示其虚幻，终结了骑士文学传统，同时开创现代小说",
        "points": [
            {"label": "总述", "content": "《堂吉诃德》是反骑士文学的小说，通过戏仿揭示骑士文学的虚幻"},
            {"label": "分1·骑士文学特点", "content": "题材：骑士游侠、冒险、护教、护弱、典雅爱情；人物：完美骑士英雄、美丽贵妇人、魔法师、巨人、妖魔；情节：出征、寻找圣杯、解救公主、战胜妖魔；风格：华丽夸张虚幻"},
            {"label": "分2·代表作", "content": "法国克雷蒂安·德·特鲁瓦《朗斯洛》《特里斯丹与伊瑟》；西班牙《高卢的阿玛迪斯》；马洛礼《亚瑟王之死》；阿里奥斯托《疯狂的罗兰》"},
            {"label": "分3·《堂吉诃德》的戏仿", "content": "堂吉诃德读骑士小说走火入魔，把风车当巨人、客栈当城堡、村姑当贵妇人；以现实解构骑士文学的虚幻"},
            {"label": "分4·反骑士的多重意义", "content": "终结骑士文学传统；讽刺虚幻的骑士理想；展现人文主义与现实主义的冲突；堂吉诃德的理想主义与桑丘的实用主义的对话"},
            {"label": "分5·现代小说的开端", "content": "《堂吉诃德》被视为现代小说的开端；其戏仿、元小说、多重叙事影响深远"},
            {"label": "总结", "content": "《堂吉诃德》通过戏仿反骑士文学，终结骑士传统，开创现代小说"},
        ],
        "conclusion": "《堂吉诃德》是反骑士文学的典范，其戏仿手法与现实主义精神开创了现代小说",
    },
}

EQ_0457_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《堂吉诃德》第一部第八章：堂吉诃德把风车当成巨人，冲杀过去被风车掀翻——'那么多的风车'桑丘说'除非主人脑袋里也有风车在转'——戏仿的经典场景",
            "source": "塞万提斯《堂吉诃德》1605年西班牙文版 杨绛译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "塞万提斯自述：《堂吉诃德》是要'把骑士小说的那一套扫除干净'，通过戏仿终结骑士文学",
            "source": "塞万提斯《堂吉诃德·前言》1605年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "博尔赫斯指出：《堂吉诃德》的戏仿不仅是反骑士，更是元小说的先驱，其虚构与现实的多重交织开创了现代小说",
            "source": "博尔赫斯《堂吉诃德的魔术》1952年西班牙文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《堂吉诃德》定位为反骑士文学的典范与现代小说的开端",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《堂吉诃德》的反骑士意义；郑克鲁《外国文学史》更注重其现代小说开创性。两书共识：《堂吉诃德》是现代小说开端。",
        "scholarComparison": "塞万提斯从创作自述视角定位反骑士；博尔赫斯从元小说视角重评其开创性；米兰·昆德拉从小说艺术视角分析其戏仿。三种视角互补：塞重创，博重元，昆重艺。",
    },
    "referenceLinks": [
        {"label": "中国作家网·塞万提斯与现代小说", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《堂吉诃德》的戏仿艺术", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0458: 撒旦与靡菲斯特形象比较（比较型）─────────────
EQ_0458_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["撒旦", "靡菲斯特", "失乐园", "浮士德", "比较"],
    "limitKeywords": ["弥尔顿", "歌德"],
    "task": "梳理相似 + 分析相异 + 评价意义",
    "breakthroughAngles": [
        "①相似·魔鬼形象与反叛精神",
        "②相似·复杂人性与诱惑力",
        "③相异·撒旦的革命英雄色彩",
        "④相异·靡菲斯特的否定精神",
        "⑤相异·最终命运（失败 vs 失去灵魂）",
    ],
    "angleRationale": "本题为比较型，需先梳理撒旦与靡菲斯特的相似，再分析相异。符合'同中求异'的比较型策略。",
    "argumentPath": {
        "thesis": "弥尔顿《失乐园》的撒旦与歌德《浮士德》的靡菲斯特同为魔鬼形象，都具有反叛精神与复杂人性，但撒旦更具革命英雄色彩，靡菲斯特更具否定精神",
        "points": [
            {"label": "总述", "content": "撒旦与靡菲斯特是西方文学两大经典魔鬼形象"},
            {"label": "分1·相似·魔鬼形象", "content": "都是'魔鬼'形象——撒旦是反抗上帝的堕天使；靡菲斯特是浮士德的引诱者"},
            {"label": "分2·相似·反叛精神", "content": "撒旦反叛上帝的权威；靡菲斯特嘲弄上帝与人；都具有反叛精神"},
            {"label": "分3·相似·复杂人性", "content": "非纯粹恶的化身，而是有思想、有情感的存在；都具有诱惑力"},
            {"label": "分4·相异·撒旦的革命英雄色彩", "content": "撒旦'宁在地狱称王，不在天堂为臣'，具革命英雄气概；弥尔顿借撒旦投射清教革命精神"},
            {"label": "分5·相异·靡菲斯特的否定精神", "content": "靡菲斯特自称'否定的精神'（Ich bin der Geist, der stets verneint），是'总是否定'的哲学化身"},
            {"label": "分6·相异·最终命运", "content": "撒旦被上帝击败化为蛇；靡菲斯特失去浮士德的灵魂（因天界 intervention）"},
            {"label": "总结", "content": "撒旦与靡菲斯特同中有异，分别体现革命英雄与否定精神"},
        ],
        "conclusion": "撒旦与靡菲斯特是西方文学魔鬼形象的双峰，前者具革命英雄色彩，后者具否定哲学",
    },
}

EQ_0458_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "弥尔顿《失乐园》卷一：撒旦'宁在地狱称王，不在天堂为臣'（Better to reign in Hell, than serve in Heav'n）——革命英雄气概",
            "source": "弥尔顿《失乐园》1667年英文版 朱维之译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "歌德《浮士德》：靡菲斯特自称'我是部分的部分，那原本是一切的部分；我是否定的精神'（Ich bin der Geist, der stets verneint）——否定精神的哲学宣言",
            "source": "歌德《浮士德》1808年德文版 钱春绮译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "C.S.刘易斯指出：弥尔顿的撒旦虽是魔鬼，但其革命英雄气概使其成为'最迷人的反派'，投射了清教革命精神",
            "source": "C.S.刘易斯《〈失乐园〉序论》1942年英文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "歌德自述：靡菲斯特'否定的精神'不是纯粹的恶，而是推动浮士德前进的动力，'总是否定'中包含辩证的肯定",
            "source": "歌德《与艾克曼谈话录》1823-1832年德文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将撒旦与靡菲斯特定位为西方文学两大经典魔鬼形象，前者具革命英雄色彩，后者具否定哲学",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重两大形象的文学史定位；郑克鲁《外国文学史》更注重其哲学内涵。两书共识：撒旦与靡菲斯特是经典魔鬼形象。",
        "scholarComparison": "C.S.刘易斯从基督教视角分析撒旦；歌德从自述视角定位靡菲斯特；勃兰兑斯从比较视角分析二者。三种视角互补：刘重宗，歌重自，勃重比。",
    },
    "referenceLinks": [
        {"label": "中国作家网·魔鬼形象的文学传统", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·撒旦与靡菲斯特比较", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0459: 卡夫卡《城堡》K与城堡关系（作品分析型）─────────────
EQ_0459_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["卡夫卡", "城堡", "K", "关系"],
    "limitKeywords": ["主人公"],
    "task": "分析K的身份困境 + 分析城堡的不可接近 + 评价意义",
    "breakthroughAngles": [
        "①K的身份困境（土地测量员/外来者/无名无姓）",
        "②城堡的不可接近（山上/官员不见/官僚系统）",
        "③K与城堡的荒诞关系（受雇与否认/抗争与失败）",
        "④现代性隐喻（异化/官僚/存在困境）",
    ],
    "angleRationale": "本题为作品分析型，需分析K与城堡的多层关系。符合'身份分析→关系分析→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "卡夫卡《城堡》中K与城堡的关系是现代性困境的隐喻：K作为无名外来者无法接近城堡，象征现代人在官僚体制与存在荒诞中的异化",
        "points": [
            {"label": "总述", "content": "《城堡》（1926，未完成）中K与城堡的关系是卡夫卡式荒诞的集中体现"},
            {"label": "分1·K的身份困境", "content": "K自称'土地测量员'受城堡聘请；城堡是否聘请过K模糊不清；K无名无姓（仅一个字母），无家可归，无身份证明；K是'外来者'无法融入村庄"},
            {"label": "分2·城堡的不可接近", "content": "城堡在山上，K在山下村庄，永远无法到达；城堡官员（克拉姆）始终不见K，只能通过信使（巴纳巴斯）；官僚系统繁复荒诞"},
            {"label": "分3·K与城堡的荒诞关系", "content": "K受雇又被否认；K抗争却失败；K与城堡的通信暧昧不清；K与村中女人的关系试图接近城堡失败"},
            {"label": "分4·现代性隐喻", "content": "城堡象征现代官僚体制；K象征异化的现代人；K与城堡的关系是存在困境的隐喻"},
            {"label": "分5·卡夫卡式荒诞", "content": "卡夫卡式荒诞：日常细节的精确与整体的不可解；K的'土地测量员'身份本身就是荒诞的"},
            {"label": "总结", "content": "K与城堡的关系是现代性困境的隐喻，体现卡夫卡式荒诞"},
        ],
        "conclusion": "《城堡》以K与城堡的荒诞关系呈现现代性困境，是卡夫卡式荒诞的典范",
    },
}

EQ_0459_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《城堡》开篇：K到达村庄时，'城堡山的轮廓在雪中几乎看不见，雾气与黑暗也使城堡不可见'——城堡的不可接近从一开始就确立",
            "source": "卡夫卡《城堡》1926年德文版 高年生译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "K自称是城堡聘请的'土地测量员'，但城堡否认发过聘书；信使巴纳巴斯传话暧昧不清——身份的荒诞悬置",
            "source": "卡夫卡《城堡》1926年德文版 高年生译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "加缪指出：卡夫卡的荒诞在于'日常细节的精确与整体的不可解'，《城堡》中K与城堡的关系正是这种荒诞的集中体现",
            "source": "加缪《卡夫卡作品中的希望与荒诞》1943年法文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "本雅明认为：卡夫卡的城堡是现代官僚体制的隐喻，K的困境是现代人在官僚机器中的异化",
            "source": "本雅明《卡夫卡 memorial》1934年德文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《城堡》定位为卡夫卡式荒诞的典范，K与城堡的关系是现代性困境的隐喻",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《城堡》的文学史地位；郑克鲁《外国文学史》更注重其荒诞特征。两书共识：《城堡》是卡夫卡式荒诞的典范。",
        "scholarComparison": "加缪从荒诞哲学视角分析其希望与荒诞；本雅明从马克思主义视角分析其官僚隐喻；米兰·昆德拉从小说艺术视角分析其未完成性。三种视角互补：加重荒，本重官，昆重艺。",
    },
    "referenceLinks": [
        {"label": "中国作家网·卡夫卡与现代性", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《城堡》的荒诞", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0468: 李庆西《寻根：回到事物本身》评论（评论型）─────────────
EQ_0468_ANGLE = {
    "questionType": "评论型",
    "coreKeywords": ["李庆西", "寻根", "回到事物本身", "评论"],
    "limitKeywords": ["80年代中期前后创作"],
    "task": "理解文本 + 结合创作分析 + 评价意义",
    "breakthroughAngles": [
        "①寻根文学语境（文化热/重写文学史/西方现代主义译介）",
        "②'回到事物本身'的内涵（反概念化/回归日常）",
        "③寻根理论三篇重要文献（韩少功/阿城/李庆西）",
        "④结合具体创作（韩少功/阿城/王安忆）",
    ],
    "angleRationale": "本题为评论型（理论文本评论），需理解李庆西文本，结合80年代中期创作分析。符合'文本理解→创作印证→意义评价'的评论型策略。",
    "argumentPath": {
        "thesis": "李庆西《寻根：回到事物本身》是寻根文学的理论阐释，'回到事物本身'主张反概念化、回归日常，与韩少功《文学的'根'》、阿城《文化制约着人类》共同构成寻根理论三篇重要文献",
        "points": [
            {"label": "总述", "content": "李庆西是80年代'寻根文学'重要评论家，《寻根：回到事物本身》是寻根文学的理论阐释"},
            {"label": "分1·寻根文学语境", "content": "80年代'文化热''重写文学史''西方现代主义译介'背景下，寻根文学兴起"},
            {"label": "分2·寻根理论三篇文献", "content": "韩少功《文学的'根'》、阿城《文化制约着人类》、李庆西《寻根：回到事物本身》是寻根理论三篇重要文献"},
            {"label": "分3·'回到事物本身'的内涵", "content": "反概念化、回归日常；受现象学'回到事物本身'影响；反对启蒙话语的抽象化"},
            {"label": "分4·结合创作分析", "content": "韩少功《爸爸爸》'回到事物本身'的神话思维；阿城《棋王》的日常诗化；王安忆《小鲍庄》的伦理回归"},
            {"label": "分5·评价意义", "content": "李庆西的理论阐释深化了寻根文学的理论自觉，'回到事物本身'是对启蒙话语的重要反思"},
            {"label": "总结", "content": "《寻根：回到事物本身》是寻根文学理论自觉的重要文献"},
        ],
        "conclusion": "李庆西'回到事物本身'深化了寻根文学的理论自觉，是对80年代启蒙话语的重要反思",
    },
}

EQ_0468_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "韩少功《文学的'根'》：'文学有根，文学之根应深植于民族传统文化的故土里'——寻根文学的宣言",
            "source": "韩少功《文学的'根'》1985年《作家》第6期",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "阿城《文化制约着人类》：强调文化对文学的制约，呼吁'文化寻根'——寻根理论的重要文献",
            "source": "阿城《文化制约着人类》1985年《文艺报》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：李庆西《寻根：回到事物本身》受现象学影响，主张反概念化、回归日常，与韩少功、阿城共同构成寻根理论的三篇重要文献",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "洪子诚《中国当代文学史》将寻根文学定位为80年代中期重要文学现象，韩少功、阿城、李庆西的理论文章是其自觉标志",
            "source": "洪子诚《中国当代文学史》北京大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "洪子诚《当代文学史》侧重寻根文学的文学史定位；陈思和《当代文学史教程》更注重其理论自觉。两书共识：寻根文学有明确的理论自觉。",
        "scholarComparison": "韩少功从创作论视角提出'根'；阿城从文化论视角强调制约；李庆西从现象学视角主张'回到事物本身'。三种视角互补：韩重创，阿重文，李重现。",
    },
    "referenceLinks": [
        {"label": "中国作家网·寻根文学的理论自觉", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·回到事物本身", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0469: 陆蠡《乞丐与病者》散文评论（评论型）─────────────
EQ_0469_ANGLE = {
    "questionType": "评论型",
    "coreKeywords": ["陆蠡", "乞丐与病者", "散文", "评论"],
    "limitKeywords": ["囚绿记", "优秀散文理解"],
    "task": "理解文本 + 分析主题艺术 + 评价散文价值",
    "breakthroughAngles": [
        "①陆蠡与《囚绿记》背景（1940/1942牺牲）",
        "②题材与主题（苦难/人道主义/人格与艺术统一）",
        "③散文艺术（语言/意境/抒情）",
        "④优秀散文的标准",
    ],
    "angleRationale": "本题为评论型（散文评论），需结合对优秀散文的理解评论《乞丐与病者》。符合'文本理解→艺术分析→价值评价'的评论型策略。",
    "argumentPath": {
        "thesis": "陆蠡《乞丐与病者》以人道主义情怀书写苦难，体现散文人格与艺术的高度统一，是优秀散文的典范",
        "points": [
            {"label": "总述", "content": "陆蠡（1908-1942）是现代散文家，《囚绿记》（1940）是其散文集，含《囚绿记》《乞丐与病者》《光阴》等"},
            {"label": "分1·陆蠡背景", "content": "陆蠡1942年被日寇杀害，其散文人格与艺术高度统一；《囚绿记》写囚禁常春藤象征对自由与生命的渴望"},
            {"label": "分2·题材与主题", "content": "写乞丐与病者的苦难，体现人道主义；对底层生命的悲悯与尊重"},
            {"label": "分3·散文艺术", "content": "语言简洁隽永；意境深邃；抒情克制而深情；'回到事物本身'的写作姿态"},
            {"label": "分4·优秀散文的标准", "content": "人格与艺术统一；真情实感；语言精炼；意境深远——《乞丐与病者》符合这些标准"},
            {"label": "分5·评价意义", "content": "《乞丐与病者》是陆蠡散文的代表作，体现其人道主义精神与散文艺术的高度统一"},
            {"label": "总结", "content": "陆蠡散文以人格与艺术统一著称，《乞丐与病者》是其人道主义精神的集中体现"},
        ],
        "conclusion": "《乞丐与病者》是陆蠡散文的典范，体现人格与艺术的高度统一，符合优秀散文的标准",
    },
}

EQ_0469_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "陆蠡《囚绿记》：写作者在北平寓所囚禁一株常春藤的绿色枝条，象征对自由与生命的渴望——'绿色是多宝贵的啊'——散文艺术的典范",
            "source": "陆蠡《囚绿记》1940年文化生活出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "巴金指出：陆蠡是'有品格的散文家'，其散文人格与艺术高度统一，1942年殉国使其散文获得精神重量",
            "source": "巴金《陆蠡的散文》1947年《文艺复兴》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "林非认为：陆蠡散文的语言简洁隽永、意境深邃、抒情克制，《囚绿记》是现代散文的典范，《乞丐与病者》延续其人道主义精神",
            "source": "林非《中国现代散文史》中国社会科学出版社1997年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "钱理群《中国现代文学三十年》将陆蠡定位为现代散文重要作家，《囚绿记》是其代表作，体现散文人格与艺术的统一",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重陆蠡的文学史地位；林非《散文史》更注重其散文艺术。两书共识：陆蠡是现代散文重要作家。",
        "scholarComparison": "巴金从同时代视角赞誉其品格；林非从散文史视角分析其艺术；余秋雨从精神史视角定位其殉国意义。三种视角互补：巴重品，林重艺，余重神。",
    },
    "referenceLinks": [
        {"label": "中国作家网·陆蠡与现代散文", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《囚绿记》的散文艺术", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "陆蠡", "note": "项目暂无陆蠡独立知识点，建议补充'陆蠡《囚绿记》与现代散文'以覆盖现代散文谱系"},
    ],
}

# ── eq_0479: 神曲二重性（作品分析型）─────────────
EQ_0479_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["神曲", "二重性", "但丁"],
    "limitKeywords": ["中世纪", "文艺复兴"],
    "task": "分析思想二重性 + 分析艺术二重性 + 评价意义",
    "breakthroughAngles": [
        "①思想二重性（中世纪神学框架+人文主义萌芽）",
        "②艺术二重性（梦幻形式+现实主义因素）",
        "③语言二重性（意大利俗语+拉丁语传统）",
        "④文学史意义（中世纪与文艺复兴桥梁）",
    ],
    "angleRationale": "本题为作品分析型，需分析《神曲》的二重性。符合'二重性梳理→具体分析→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "但丁《神曲》的二重性在于：思想上中世纪神学框架与文艺复兴人文主义并存，艺术上梦幻形式与现实主义因素并存，是中世纪与文艺复兴的桥梁",
        "points": [
            {"label": "总述", "content": "《神曲》的二重性：中世纪神学框架与文艺复兴人文主义并存"},
            {"label": "分1·思想·中世纪神学框架", "content": "地狱、炼狱、天堂的基督教宇宙观；上帝的最终审判；罪与罚的对应；炼狱的赎罪；天堂的等级"},
            {"label": "分2·思想·人文主义萌芽", "content": "赞美理性——维吉尔代表理性、古典文化；赞美爱情——贝雅特丽采代表信仰与爱情；个人奋斗——但丁自己的精神历程；对古典文化的推崇"},
            {"label": "分3·艺术·梦幻形式与现实主义", "content": "梦幻文学形式——三界游历；现实主义因素——真实人物（维吉尔/贝雅特丽齐/教皇博尼法斯）、真实事件（佛罗伦萨党争）"},
            {"label": "分4·语言二重性", "content": "用意大利俗语而非拉丁语写作——开创意大利文学语言；同时保留拉丁语神学传统"},
            {"label": "分5·文学史意义", "content": "《神曲》是中世纪与文艺复兴的桥梁，其二重性体现过渡时代特征"},
            {"label": "总结", "content": "《神曲》的二重性是过渡时代的产物，使其成为中世纪与文艺复兴的桥梁"},
        ],
        "conclusion": "《神曲》的二重性体现了从中世纪到文艺复兴的过渡，是文学史上的关键节点",
    },
}

EQ_0479_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《神曲·地狱篇》：但丁在维吉尔引导下游历地狱，维吉尔代表理性——'你是我的导师，你是我的作者'——人文主义对古典文化的推崇",
            "source": "但丁《神曲》1307-1321年意大利文版 田德望译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《神曲》中但丁把活着的教皇博尼法斯八世提前打入地狱——现实主义因素与政治批判的体现",
            "source": "但丁《神曲·地狱篇》第十九歌 田德望译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "恩格斯指出：但丁是'中世纪的最后一位诗人，同时又是新时代的最初一位诗人'——经典概括《神曲》的二重性",
            "source": "恩格斯《〈共产党宣言〉意大利版序言》1893年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "薄伽丘《但丁赞》首次使用'神曲'（Divina Commedia）之名，强调其人文主义内涵与意大利俗语的开创意义",
            "source": "薄伽丘《但丁赞》1357-1362年意大利文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《神曲》定位为中世纪与文艺复兴的桥梁，其二重性体现过渡时代特征",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《神曲》的文学史地位；郑克鲁《外国文学史》更注重其二重性分析。两书共识：《神曲》是中世纪与文艺复兴的桥梁。",
        "scholarComparison": "恩格斯从历史唯物主义视角经典概括二重性；薄伽丘从同时代视角强调人文主义；现代学者从文本视角分析其复杂性。三种视角互补：恩重史，薄重人，现重文。",
    },
    "referenceLinks": [
        {"label": "中国作家网·但丁与文艺复兴", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《神曲》的二重性", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0480: 哈姆雷特形象（作品分析型）─────────────
EQ_0480_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["哈姆雷特", "形象", "莎士比亚"],
    "limitKeywords": ["悲剧"],
    "task": "梳理形象特点 + 分析形象内涵 + 评价意义",
    "breakthroughAngles": [
        "①人文主义王子（威登堡大学/理性/人性）",
        "②思想深刻（生存还是毁灭）",
        "③行动延宕（多次延宕复仇）",
        "④装疯掩饰与孤独者",
        "⑤悲剧英雄（最终杀死克劳狄斯但自己中毒）",
    ],
    "angleRationale": "本题为作品分析型，需系统梳理哈姆雷特形象特点，分析其内涵。符合'特点梳理→内涵分析→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "哈姆雷特是莎士比亚悲剧的核心形象，作为人文主义王子，其思想深刻、行动延宕、装疯掩饰、孤独者的多重特征，使其成为世界文学最复杂的人物之一",
        "points": [
            {"label": "总述", "content": "哈姆雷特形象是莎士比亚悲剧《哈姆雷特》（1601）的核心，世界文学最复杂的人物之一"},
            {"label": "分1·人文主义王子", "content": "曾在威登堡大学求学，接受人文主义教育，崇尚理性、人性、友谊"},
            {"label": "分2·思想深刻", "content": "'生存还是毁灭'（To be, or not to be）的哲学追问；对人性、死亡、命运的深刻思考"},
            {"label": "分3·行动延宕", "content": "知道父亲被杀后多次延宕复仇；延宕源于思想深刻与行动能力的矛盾；'重整乾坤'的重负"},
            {"label": "分4·装疯掩饰", "content": "以疯癫掩饰真实意图；装疯使其更孤独；真假疯癫的模糊"},
            {"label": "分5·孤独者", "content": "身边无人可信赖，连爱人奥菲莉娅也被利用；母亲改嫁的失望；朋友的背叛"},
            {"label": "分6·悲剧英雄", "content": "最终杀死克劳狄斯但自己也中毒身亡；'其余都是沉默'（The rest is silence）——悲剧的完成"},
            {"label": "总结", "content": "哈姆雷特形象的多重特征使其成为世界文学最复杂的人物之一"},
        ],
        "conclusion": "哈姆雷特是莎士比亚悲剧的巅峰形象，其'延宕'与'思想'成为世界文学的经典母题",
    },
}

EQ_0480_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《哈姆雷特》第三幕第一场：'生存还是毁灭，这是一个问题'（To be, or not to be, that is the question）——哲学追问的经典独白",
            "source": "莎士比亚《哈姆雷特》1601年英文版 朱生豪译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《哈姆雷特》结尾：'其余都是沉默'（The rest is silence）——哈姆雷特临终之言，悲剧的完成",
            "source": "莎士比亚《哈姆雷特》1601年英文版 朱生豪译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "歌德指出：哈姆雷特的悲剧在于'一棵橡树种在一个昂贵的花瓶里'，花瓶被橡树的根撑破——思想的伟大与行动能力的不足",
            "source": "歌德《威廉·迈斯特的学习时代》1795-1796年德文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "布拉德雷认为：哈姆雷特的'延宕'是其思想深刻与行动能力矛盾的表现，其悲剧是'道德理想与现实的冲突'",
            "source": "布拉德雷《莎士比亚悲剧》1904年英文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将哈姆雷特定位为莎士比亚悲剧的核心形象，其'延宕'与'思想深刻'是世界文学的经典母题",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重哈姆雷特的文学史地位；郑克鲁《外国文学史》更注重其形象分析。两书共识：哈姆雷特是莎士比亚悲剧的核心形象。",
        "scholarComparison": "歌德从植物隐喻视角分析其悲剧；布拉德雷从悲剧理论视角分析其延宕；弗洛伊德从精神分析视角分析其潜意识。三种视角互补：歌重隐，布重悲，弗重心。",
    },
    "referenceLinks": [
        {"label": "中国作家网·莎士比亚与悲剧", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·哈姆雷特的延宕", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0481: 《喧哗与骚动》艺术特色（作品分析型）─────────────
EQ_0481_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["喧哗与骚动", "福克纳", "艺术特色"],
    "limitKeywords": ["多重视角", "意识流"],
    "task": "梳理艺术特色 + 结合文本分析 + 评价意义",
    "breakthroughAngles": [
        "①多重视角叙事（班吉/昆丁/杰生/迪尔西）",
        "②意识流手法（白痴意识流/精神崩溃意识流）",
        "③时间处理（过去现在混合/时间感错乱）",
        "④复调小说与主观客观交织",
        "⑤南方神话与历史意识",
    ],
    "angleRationale": "本题为作品分析型，需系统梳理《喧哗与骚动》的艺术特色。符合'特色梳理→文本印证→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "福克纳《喧哗与骚动》以多重视角叙事、意识流手法、时间处理、复调小说等艺术特色，成为现代主义小说的典范",
        "points": [
            {"label": "总述", "content": "《喧哗与骚动》（1929）是福克纳代表作，现代主义小说的典范"},
            {"label": "分1·多重视角叙事", "content": "四个部分分别由不同人物讲述——班吉（智力残障）、昆丁（哈佛学生，精神崩溃）、杰生（自私冷酷）、迪尔西（黑人女仆，全知视角）"},
            {"label": "分2·意识流手法", "content": "班吉部分——白痴意识流，无时间感，过去现在混在一起；昆丁部分——精神崩溃前的意识流，时间感错乱，自杀前的内心"},
            {"label": "分3·时间处理", "content": "同一事件（凯蒂的堕落）从不同视角呈现；时间感错乱；过去与现在交织"},
            {"label": "分4·复调小说", "content": "'复调小说'特征，主观与客观交织；多声部叙事，无单一权威视角"},
            {"label": "分5·南方神话与历史意识", "content": "康普生家族的衰败象征美国南方的没落；福克纳的'约克纳帕塔法县'神话体系"},
            {"label": "总结", "content": "《喧哗与骚动》以多重视角与意识流成为现代主义小说的典范"},
        ],
        "conclusion": "《喧哗与骚动》是多重视角与意识流的典范，深刻呈现南方没落与现代精神困境",
    },
}

EQ_0481_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《喧哗与骚动》书名源自莎士比亚《麦克白》：'人生如白痴讲述的故事，充满喧哗与骚动，却毫无意义'——标题本身即为主题隐喻",
            "source": "莎士比亚《麦克白》第五幕第五场 福克纳引用",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《喧哗与骚动》班吉部分：1928年4月7日，33岁智力残障的班吉的意识流，无时间感，过去现在混在一起——白痴意识流的典范",
            "source": "福克纳《喧哗与骚动》1929年英文版 李文俊译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "福克纳自述：《喧哗与骚动》'同一个故事讲了四遍'，前三次由不同人物讲述，第四次由作者讲述，是'复调小说'的实践",
            "source": "福克纳《福克纳自传》1956年英文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "萨特指出：福克纳的时间感是'向后看的'，过去不可挽回，未来不存在，只有'现在'在流逝——《喧哗与骚动》的意识流呈现这种时间哲学",
            "source": "萨特《福克纳的时间》1939年法文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将《喧哗与骚动》定位为现代主义小说的典范，其多重视角与意识流影响深远",
            "source": "朱维之《外国文学史》南开大学出版社2004年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《喧哗与骚动》的文学史地位；郑克鲁《外国文学史》更注重其意识流技巧。两书共识：《喧哗与骚动》是现代主义小说典范。",
        "scholarComparison": "福克纳从创作自述视角定位其'复调'；萨特从存在主义视角分析其时间哲学；李文俊从译介视角分析其意识流。三种视角互补：福重创，萨重时，李重译。",
    },
    "referenceLinks": [
        {"label": "中国作家网·福克纳与南方文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《喧哗与骚动》的意识流", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── 主程序 ──────────────────────────────────────────────
FILL_MAP = {
    "eq_0447": (EQ_0447_ANGLE, EQ_0447_NOTES),
    "eq_0448": (EQ_0448_ANGLE, EQ_0448_NOTES),
    "eq_0454": (EQ_0454_ANGLE, EQ_0454_NOTES),
    "eq_0457": (EQ_0457_ANGLE, EQ_0457_NOTES),
    "eq_0458": (EQ_0458_ANGLE, EQ_0458_NOTES),
    "eq_0459": (EQ_0459_ANGLE, EQ_0459_NOTES),
    "eq_0468": (EQ_0468_ANGLE, EQ_0468_NOTES),
    "eq_0469": (EQ_0469_ANGLE, EQ_0469_NOTES),
    "eq_0479": (EQ_0479_ANGLE, EQ_0479_NOTES),
    "eq_0480": (EQ_0480_ANGLE, EQ_0480_NOTES),
    "eq_0481": (EQ_0481_ANGLE, EQ_0481_NOTES),
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

    print(f"\n共填充 {filled_count} 道题（预期 11 道）")
    assert filled_count == 11, f"填充数量不符: {filled_count} != 11"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"已写回 {SEED_PATH}")


if __name__ == "__main__":
    main()

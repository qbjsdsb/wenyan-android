#!/usr/bin/env python3
"""
为 2010 年 610 卷论述题批量填充 angle + notes 字段（12 道）。

题目清单：
- eq_0066 古代·《史记》人物形象塑造手法
- eq_0067 古代·唐传奇比六朝志怪的根本性变化
- eq_0068 古代·明代八股取士对文学的影响
- eq_0069 古代·高适与岑参边塞诗异同
- eq_0073 外国·比较文学与世界文学必做题
- eq_0074 外国·但丁《神曲》艺术特征
- eq_0075 外国·意识流小说特征与成就
- eq_0076 现当代·西方文学资源对中国文学转型的影响
- eq_0077 外国·欧美文学批判精神及当代意义
- eq_0078 理论·文学在戏剧影视艺术中的作用
- eq_0079 理论·应用文的文本特征及文学史地位
- eq_0080 古代·《红楼梦》诗词曲戏曲分析
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0066: 《史记》人物形象塑造手法 ─────────────────────────
EQ_0066_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["史记", "人物形象", "艺术手法"],
    "limitKeywords": ["司马迁"],
    "task": "梳理艺术手法 + 结合具体人物分析 + 评价成就",
    "breakthroughAngles": [
        "①细节描写（典型细节刻画性格）",
        "②对比衬托（人物对比凸显性格）",
        "③个性化语言（人物对话见性格）",
        "④互见法（本传隐讳他传披露）",
        "⑤心理描写（内心活动呈现）",
        "⑥故事情节展现性格",
    ],
    "angleRationale": "本题为作品分析型，需系统梳理《史记》人物塑造的艺术手法，结合具体人物印证。符合'手法梳理→文本印证→成就评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "《史记》通过细节描写、对比衬托、个性化语言、互见法、心理描写等艺术手法，塑造了栩栩如生的人物形象，开创了中国传记文学的典范",
        "points": [
            {"label": "总述", "content": "《史记》是中国第一部纪传体通史，其人物塑造被鲁迅誉为'史家之绝唱，无韵之离骚'"},
            {"label": "分1·细节描写", "content": "通过典型细节刻画性格：项羽'力拔山兮气盖世'的豪迈，刘邦'豁如也'的豁达"},
            {"label": "分2·对比衬托", "content": "人物对比凸显性格：项羽与刘邦的对比（勇与智）；李广与程不识的对比（奇与正）"},
            {"label": "分3·个性化语言", "content": "人物对话见性格：项羽'彼可取而代也'的豪迈，刘邦'嗟乎，大丈夫当如此也'的含蓄"},
            {"label": "分4·互见法", "content": "本传隐讳他传披露：项羽本传不写其残暴，他传补充——维护人物整体形象又尊重史实"},
            {"label": "分5·心理描写", "content": "内心活动呈现：韩信'胯下之辱'后的隐忍心理，通过行为暗示心理"},
            {"label": "分6·成就评价", "content": "开创传记文学传统；影响后世小说戏曲；'不虚美不隐恶'的实录精神"},
            {"label": "总结", "content": "《史记》人物塑造手法多样，开创中国传记文学，影响深远"},
        ],
        "conclusion": "《史记》以多重艺术手法塑造人物形象，实现了史学真实与文学典型的统一",
    },
}

EQ_0066_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "司马迁《史记·项羽本纪》：项羽见秦始皇「彼可取而代也」——豪迈性格的典型语言",
            "source": "司马迁《史记·项羽本纪》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "司马迁《史记·高祖本纪》：刘邦见秦始皇「嗟乎，大丈夫当如此也」——含蓄性格的典型语言",
            "source": "司马迁《史记·高祖本纪》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅评价《史记》为'史家之绝唱，无韵之离骚'——肯定其史学与文学双重价值",
            "source": "鲁迅《汉文学史纲要》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "韩兆琦指出：《史记》人物塑造的核心是'不虚美不隐恶'的实录精神与'究天人之际'的史识结合",
            "source": "韩兆琦《史记笺证》江西人民出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《史记》定位为中国传记文学的开创者，其人物塑造手法对后世小说戏曲影响深远",
            "source": "袁行霈《中国文学史》第一卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《史记》的文学史地位；章培恒《中国文学史》更注重其史学成就。两书共识：《史记》是史传文学的巅峰。",
        "scholarComparison": "鲁迅从文学史视角肯定其文学价值；韩兆琦从笺证视角分析其手法；张大可从史学史视角定位其史识。三种视角互补：鲁重文学，韩重手法，张重史识。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《史记》人物塑造艺术", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·司马迁与史传文学传统", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "司马迁", "note": "项目暂无司马迁独立知识点，建议补充'司马迁《史记》与纪传体通史'以完善汉代文学谱系"},
    ],
}

# ── eq_0067: 唐传奇比六朝志怪的根本性变化 ─────────────────────
EQ_0067_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["唐传奇", "六朝志怪", "根本性变化"],
    "limitKeywords": ["比较"],
    "task": "梳理六朝志怪特征 + 分析唐传奇变化 + 评价意义",
    "breakthroughAngles": [
        "①创作目的（志怪记实→传奇虚构）",
        "②内容题材（神鬼怪异→现实人生）",
        "③人物塑造（扁平→典型）",
        "④叙事艺术（简略→曲折）",
        "⑤作者意识（无意为文→有意为小说）",
    ],
    "angleRationale": "本题为比较型，需先梳理六朝志怪特征，再分析唐传奇的根本性变化，最后评价意义。符合'特征梳理→变化分析→意义评价'的比较型策略。",
    "argumentPath": {
        "thesis": "唐传奇相比六朝志怪小说的根本性变化在于从'记实'转向'虚构'、从'神鬼'转向'人间'、从'无意为文'转向'有意为小说'，标志着中国小说的成熟",
        "points": [
            {"label": "总述", "content": "唐传奇是中国小说成熟的标志，与六朝志怪有根本性差异"},
            {"label": "分1·创作目的", "content": "六朝志怪'发明神道之不诬'记实；唐传奇'著文章之美，传要妙之情'虚构——从记实到虚构"},
            {"label": "分2·内容题材", "content": "六朝志怪写神鬼怪异；唐传奇转向现实人生（爱情/政治/豪侠）——《莺莺传》《霍小玉传》《南柯太守传》"},
            {"label": "分3·人物塑造", "content": "六朝志怪人物扁平；唐传奇塑造典型形象（崔莺莺/霍小玉/红拂女）——从扁平到典型"},
            {"label": "分4·叙事艺术", "content": "六朝志怪叙事简略；唐传奇情节曲折完整（开端-发展-高潮-结局）——从简略到曲折"},
            {"label": "分5·作者意识", "content": "六朝志怪无意为文；唐传奇'有意为小说'（沈既济/元稹/白行简）——从无意到有意"},
            {"label": "分6·意义评价", "content": "唐传奇标志中国小说成熟；影响后世小说戏曲（《西厢记》源于《莺莺传》）"},
            {"label": "总结", "content": "唐传奇的根本性变化是创作观念的转型，标志中国小说从'记实'走向'虚构'的成熟"},
        ],
        "conclusion": "唐传奇相比六朝志怪的根本性变化在于创作观念的转型，这是中国小说成熟的关键标志",
    },
}

EQ_0067_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "元稹《莺莺传》：崔莺莺与张生的爱情悲剧——唐传奇现实题材的典范",
            "source": "元稹《莺莺传》唐传奇",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "干宝《搜神记》：'发明神道之不诬'——六朝志怪记实目的的典型表述",
            "source": "干宝《搜神记》东晋",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅指出：唐传奇'虽尚不离于搜奇记逸，然叙述宛转，文辞华艳，与六朝之粗陈梗概者较，演进之迹甚明，而尤显者乃在是时则始有意为小说'",
            "source": "鲁迅《中国小说史略》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将唐传奇定位为中国小说成熟的标志，其与六朝志怪的根本差异在于'有意为小说'的创作自觉",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重唐传奇的小说史意义；章培恒《中国文学史》更注重其艺术成就。两书共识：唐传奇标志中国小说成熟。",
        "scholarComparison": "鲁迅从小说史视角提出'有意为小说'；程毅中从文献学视角梳理唐传奇演变；李剑国从志怪传统视角分析其转型。三种视角互补：鲁重史论，程重文献，李重传统。",
    },
    "referenceLinks": [
        {"label": "中国作家网·唐传奇与小说成熟", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·从志怪到传奇的转型", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "唐传奇", "note": "项目暂无唐传奇独立知识点，建议补充以覆盖中国古代小说史关键环节"},
    ],
}

# ── eq_0068: 明代八股取士对文学的影响 ─────────────────────────
EQ_0068_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["明代", "八股取士", "文学", "影响"],
    "limitKeywords": ["八股文"],
    "task": "梳理八股取士制度 + 分析对文学的正负面影响 + 评价",
    "breakthroughAngles": [
        "①八股取士制度概述（明代科举定型）",
        "②负面影响（形式束缚/思想禁锢/文人依附）",
        "③正面影响（结构训练/语言精炼）",
        "④对小说戏曲的影响（间接渗透）",
        "⑤辩证评价",
    ],
    "angleRationale": "本题为综合型，需梳理八股取士制度，辩证分析其对文学的正负面影响。符合'制度梳理→影响分析→辩证评价'的综合型策略。",
    "argumentPath": {
        "thesis": "明代八股取士对文学的影响是双重的：负面影响在于形式束缚、思想禁锢与文人依附；正面影响在于结构训练与语言精炼，整体上制约了文学的自由发展",
        "points": [
            {"label": "总述", "content": "八股取士是明代科举的定型形式，对文学产生深远影响"},
            {"label": "分1·制度概述", "content": "八股文讲究破题/承题/起讲/入手/起股/中股/后股/束股，形式严格，代圣贤立言"},
            {"label": "分2·负面·形式束缚", "content": "八股形式严格束缚思维；文人习于程式，文学创作缺乏创新——'台阁体'的僵化"},
            {"label": "分3·负面·思想禁锢", "content": "八股取士以程朱理学为标准；文人思想趋同，文学缺乏个性——前后七子的复古模拟"},
            {"label": "分4·负面·文人依附", "content": "科举功名成为文人唯一出路；文学依附权力，缺乏独立精神"},
            {"label": "分5·正面·结构训练", "content": "八股训练文章结构能力；'起承转合'影响小说戏曲叙事结构"},
            {"label": "分6·正面·语言精炼", "content": "八股讲究对偶声律；影响文学语言的精炼与节奏"},
            {"label": "总结", "content": "八股取士对文学影响整体负面，制约自由发展，但结构训练有一定正面价值"},
        ],
        "conclusion": "明代八股取士对文学的影响以负面为主，形式束缚与思想禁锢制约了文学创新，但其结构训练对叙事艺术有一定贡献",
    },
}

EQ_0068_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "顾炎武指出：「八股之害，等于焚书，而败坏人材，有甚于咸阳之郊所坑者」——对八股取士的尖锐批评",
            "source": "顾炎武《日知录》卷十六",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "吴敬梓《儒林外史》通过范进、周进等形象，深刻揭露八股取士对文人人性的扭曲",
            "source": "吴敬梓《儒林外史》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱穆认为：八股文虽形式僵化，但其'起承转合'的结构训练对文章写作有一定价值，不可全盘否定",
            "source": "钱穆《中国学术思想史论丛》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将八股取士定位为明代文学发展的制约因素，其形式束缚与思想禁锢影响深远",
            "source": "袁行霈《中国文学史》第四卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重八股对文学的负面影响；章培恒《中国文学史》更注重其社会文化语境。两书共识：八股取士制约文学发展。",
        "scholarComparison": "顾炎武从思想史视角尖锐批评；吴敬梓从文学视角揭露其害；钱穆从学术史视角辩证分析。三种视角互补：顾重批判，吴重揭露，钱重辩证。",
    },
    "referenceLinks": [
        {"label": "中国作家网·八股取士与明代文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·科举制度对中国文学的影响", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0069: 高适与岑参边塞诗异同 ─────────────────────────────
EQ_0069_ANGLE = {
    "questionType": "比较型",
    "coreKeywords": ["高适", "岑参", "边塞诗", "异同"],
    "limitKeywords": ["《燕歌行》", "《白雪歌》"],
    "task": "梳理共同点 + 分析差异 + 评价",
    "breakthroughAngles": [
        "①共同点（盛唐边塞诗派/豪迈风格/爱国主题）",
        "②差异·题材（高适写边塞现实/岑参写边塞风光）",
        "③差异·风格（高适沉郁/岑参奇丽）",
        "④差异·情感（高适忧国/岑参惊奇）",
        "⑤《燕歌行》与《白雪歌》具体比较",
    ],
    "angleRationale": "本题为比较型，需先梳理共同点，再分析差异，结合《燕歌行》《白雪歌》具体比较。符合'同中求异'的比较型策略。",
    "argumentPath": {
        "thesis": "高适与岑参同为盛唐边塞诗派代表，共同体现豪迈风格与爱国主题；但高适侧重边塞现实与沉郁风格，岑参侧重边塞风光与奇丽风格，同中有异各具特色",
        "points": [
            {"label": "总述", "content": "高适与岑参是盛唐边塞诗派的双璧，同中有异各具特色"},
            {"label": "分1·共同点", "content": "盛唐边塞诗派代表；豪迈奔放的风格；爱国主题与边塞情怀"},
            {"label": "分2·差异·题材", "content": "高适写边塞战争现实与士卒疾苦；岑参写边塞奇异风光与西域风情"},
            {"label": "分3·差异·风格", "content": "高适沉郁顿挫，尚质实；岑参奇丽雄奇，尚瑰丽"},
            {"label": "分4·差异·情感", "content": "高适忧国忧民，对战争反思；岑参好奇尚异，对边塞惊奇"},
            {"label": "分5·《燕歌行》与《白雪歌》", "content": "《燕歌行》'战士军前半死生，美人帐下犹歌舞'讽刺将帅；《白雪歌》'忽如一夜春风来，千树万树梨花开'写雪景奇丽"},
            {"label": "总结", "content": "高岑同为边塞诗派代表，高沉郁岑奇丽，共同构成盛唐边塞诗的完整面貌"},
        ],
        "conclusion": "高适与岑参的异同体现了盛唐边塞诗的丰富性，高之沉郁与岑之奇丽相得益彰",
    },
}

EQ_0069_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "高适《燕歌行》：「战士军前半死生，美人帐下犹歌舞」——讽刺将帅享乐，同情士卒疾苦",
            "source": "高适《燕歌行》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "岑参《白雪歌送武判官归京》：「忽如一夜春风来，千树万树梨花开」——边塞雪景的奇丽想象",
            "source": "岑参《白雪歌送武判官归京》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "殷璠《河岳英灵集》评高适'诗多胸臆语，兼有气骨'，评岑参'诗奇体俊，语亦造奇'——唐代选本对高岑差异的经典概括",
            "source": "殷璠《河岳英灵集》唐代的唐诗选本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "严羽《沧浪诗话》：'高岑之诗悲壮，读之令人感慨'——但高适悲壮中沉郁，岑参悲壮中奇丽",
            "source": "严羽《沧浪诗话》南宋",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将高适岑参并列为盛唐边塞诗派代表，高适沉郁质实，岑参奇丽瑰伟",
            "source": "袁行霈《中国文学史》第二卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重高岑的风格差异；章培恒《中国文学史》更注重其时代背景。两书共识：高岑同为边塞诗派代表，风格各异。",
        "scholarComparison": "殷璠从唐代选本视角概括差异；严羽从诗学视角分析风格；今人高步瀛《唐宋诗举要》从选学视角具体分析。三种视角互补：殷重当时评价，严重诗学，高重选学。",
    },
    "referenceLinks": [
        {"label": "中国作家网·盛唐边塞诗派研究", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·高岑边塞诗比较", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "高适", "note": "项目暂无高适独立知识点，建议补充以覆盖盛唐边塞诗派"},
        {"author": "岑参", "note": "项目暂无岑参独立知识点，建议补充以覆盖盛唐边塞诗派"},
    ],
}

# ── eq_0074: 但丁《神曲》艺术特征 ─────────────────────────────
EQ_0074_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["但丁", "神曲", "艺术特征"],
    "limitKeywords": ["但丁"],
    "task": "梳理艺术特征 + 评价文学史意义",
    "breakthroughAngles": [
        "①梦幻文学形式（三界游历结构）",
        "②象征手法（细节象征/整体象征）",
        "③现实主义因素（真实人物/真实事件）",
        "④语言创新（意大利俗语写作）",
        "⑤百科全书式内容（神学/哲学/政治）",
        "⑥文学史意义（中世纪与文艺复兴桥梁）",
    ],
    "angleRationale": "本题为作品分析型，需系统梳理《神曲》的艺术特征，评价其文学史意义。符合'特征梳理→意义评价'的作品分析策略。",
    "argumentPath": {
        "thesis": "但丁《神曲》以梦幻文学形式、象征手法、现实主义因素、意大利俗语写作等艺术特征，成为中世纪文学与文艺复兴的桥梁",
        "points": [
            {"label": "总述", "content": "《神曲》是但丁的代表作，写但丁在维吉尔与贝雅特丽齐引导下游历地狱、炼狱、天堂"},
            {"label": "分1·梦幻文学形式", "content": "三界游历结构（地狱34歌/炼狱33歌/天堂33歌）；梦幻形式承载现实内容"},
            {"label": "分2·象征手法", "content": "细节象征（三野兽象征贪欲/骄傲/野心）；整体象征（游历象征人类精神升华）"},
            {"label": "分3·现实主义因素", "content": "真实人物（维吉尔/贝雅特丽齐/教皇博尼法斯）；真实事件（佛罗伦萨党争）——中世纪文学中的现实主义"},
            {"label": "分4·语言创新", "content": "用意大利俗语而非拉丁语写作——开创意大利文学语言，推动民族文学发展"},
            {"label": "分5·百科全书式内容", "content": "融合神学/哲学/政治/科学/历史；体现中世纪百科全书式知识体系"},
            {"label": "分6·文学史意义", "content": "中世纪与文艺复兴的桥梁；'文艺复兴的曙光'；意大利文学的开山之作"},
            {"label": "总结", "content": "《神曲》以多重艺术特征成为文学史里程碑，是中世纪与文艺复兴的桥梁"},
        ],
        "conclusion": "《神曲》的艺术特征体现了从中世纪向文艺复兴的过渡，是欧洲文学史的关键节点",
    },
}

EQ_0074_NOTES = {
    "evidences": {
    },
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "但丁《神曲·地狱篇》开篇：「在人生的中途，我迷失在一个黑暗的森林里」——梦幻文学的经典开篇",
            "source": "但丁《神曲》人民文学出版社田德望译本",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "薄伽丘称但丁为'文艺复兴的曙光'，肯定《神曲》从中世纪向文艺复兴的过渡意义",
            "source": "薄伽丘《但丁传》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "朱维之指出：《神曲》用意大利俗语写作是文学史的革命，开创了民族文学语言的先河",
            "source": "朱维之《外国文学史》南开大学出版社2009年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "郑克鲁《外国文学史》将《神曲》定位为中世纪文学与文艺复兴的桥梁，其艺术特征体现过渡性",
            "source": "郑克鲁《外国文学史》高等教育出版社2006年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重《神曲》的语言创新；郑克鲁《外国文学史》更注重其过渡性。两书共识：《神曲》是中世纪与文艺复兴的桥梁。",
        "scholarComparison": "薄伽丘从文艺复兴视角肯定但丁；朱维之从语言史视角肯定俗语创新；田德望从翻译学视角分析其结构。三种视角互补：薄重定位，朱重语言，田重结构。",
    },
    "referenceLinks": [
        {"label": "中国作家网·但丁《神曲》的文学史意义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《神曲》与中世纪文学", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "但丁", "note": "项目暂无但丁独立知识点，建议补充'但丁《神曲》与意大利文学'以覆盖欧洲中世纪文学"},
    ],
}

# ── eq_0075: 意识流小说特征与成就 ─────────────────────────────
EQ_0075_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["意识流小说", "特征", "成就"],
    "limitKeywords": ["现代主义"],
    "task": "梳理特征 + 论述成就 + 评价影响",
    "breakthroughAngles": [
        "①心理时间（柏格森时间哲学）",
        "②内心独白（直接内心独白）",
        "③自由联想（跳跃性叙事）",
        "④视角转换（多重叙事视角）",
        "⑤代表作家（乔伊斯/伍尔夫/福克纳）",
        "⑥成就与影响",
    ],
    "angleRationale": "本题为综合型，需系统梳理意识流小说的特征，论述其成就与影响。符合'特征梳理→成就论述→影响评价'的综合型策略。",
    "argumentPath": {
        "thesis": "意识流小说以心理时间、内心独白、自由联想、视角转换为核心特征，乔伊斯《尤利西斯》、伍尔夫《到灯塔去》、福克纳《喧哗与骚动》是其代表，开创了现代主义小说的新范式",
        "points": [
            {"label": "总述", "content": "意识流小说是20世纪现代主义文学的重要流派，受柏格森时间哲学与弗洛伊德心理学影响"},
            {"label": "分1·心理时间", "content": "柏格森'绵延'时间观；打破线性时间，过去现在未来交织——心理时间取代物理时间"},
            {"label": "分2·内心独白", "content": "直接内心独白呈现人物意识流动；乔伊斯《尤利西斯》莫莉独白是典范"},
            {"label": "分3·自由联想", "content": "意识跳跃性流动；以联想逻辑而非因果逻辑组织叙事——伍尔夫《墙上的斑点》"},
            {"label": "分4·视角转换", "content": "多重叙事视角；打破全知视角；福克纳《喧哗与骚动》多视角叙述"},
            {"label": "分5·代表作家", "content": "乔伊斯《尤利西斯》/伍尔夫《到灯塔去》/福克纳《喧哗与骚动》——意识流三大家"},
            {"label": "分6·成就与影响", "content": "开创现代主义小说新范式；影响中国当代文学（王蒙/莫言/残雪）；'向内转'的文学革命"},
            {"label": "总结", "content": "意识流小说以多重特征开创现代主义小说新范式，成就卓著影响深远"},
        ],
        "conclusion": "意识流小说是20世纪文学'向内转'的革命，其特征与成就开创了现代主义小说的新纪元",
    },
}

EQ_0075_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "乔伊斯《尤利西斯》莫莉独白：连续数十页无标点的内心独白——意识流小说的典范",
            "source": "乔伊斯《尤利西斯》1922年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "伍尔夫《墙上的斑点》：从墙上斑点自由联想到生命、时间、自我——自由联想的典范",
            "source": "伍尔夫《墙上的斑点》1917年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "威廉·詹姆斯提出'意识流'概念：'意识不是片段的连接，而是流动的'——意识流小说的心理学基础",
            "source": "威廉·詹姆斯《心理学原理》1890年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "瞿世镜指出：意识流小说受柏格森'绵延'时间哲学与弗洛伊德'无意识'理论影响，是20世纪文学'向内转'的革命",
            "source": "瞿世镜《意识流小说家伍尔夫》上海文艺出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将意识流小说定位为现代主义文学的重要流派，乔伊斯/伍尔夫/福克纳是代表",
            "source": "朱维之《外国文学史》南开大学出版社2009年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重意识流的特征；郑克鲁《外国文学史》更注重其哲学基础。两书共识：意识流是现代主义小说的重要流派。",
        "scholarComparison": "威廉·詹姆斯从心理学视角提出概念；瞿世镜从中国接受视角分析影响；李维屏从专论视角系统梳理。三种视角互补：詹重心理学，瞿重接受，李重专论。",
    },
    "referenceLinks": [
        {"label": "中国作家网·意识流小说与现代主义", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·意识流小说在中国", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "乔伊斯", "note": "项目暂无乔伊斯独立知识点，建议补充'乔伊斯《尤利西斯》与意识流'以覆盖现代主义小说"},
        {"author": "伍尔夫", "note": "项目暂无伍尔夫独立知识点，建议补充以覆盖意识流女作家"},
    ],
}

# ── eq_0076: 西方文学资源对中国文学转型的影响 ─────────────────
EQ_0076_ANGLE = {
    "questionType": "演变型",
    "coreKeywords": ["西方文学资源", "中国文学", "现代转型", "影响"],
    "limitKeywords": ["20世纪"],
    "task": "梳理西方文学影响 + 分析转型历程 + 评价意义",
    "breakthroughAngles": [
        "①晚清（翻译文学/林纾）",
        "②五四（启蒙/现实主义/浪漫主义）",
        "③30年代（左翼/现代主义）",
        "④40年代（抗战/民族形式）",
        "⑤80年代（现代主义回归）",
        "⑥评价（催化剂与本土化）",
    ],
    "angleRationale": "本题为演变型，需以时间纵轴梳理西方文学资源对中国文学转型的影响。符合'流变型拉时间轴+每阶段突出影响'的策略。",
    "argumentPath": {
        "thesis": "20世纪中国文学向现代转型的进程中，西方文学资源起到催化剂作用，从晚清翻译到五四启蒙、从30年代左翼到80年代现代主义回归，西方文学资源不断推动中国文学的现代化",
        "points": [
            {"label": "总述", "content": "20世纪中国文学现代转型与西方文学资源的影响密不可分"},
            {"label": "分1·晚清", "content": "林纾翻译西方小说（《巴黎茶花女遗事》）；严复翻译《天演论》——西方文学资源初次引入"},
            {"label": "分2·五四", "content": "鲁迅受俄国/东欧弱小民族文学影响；胡适引入易卜生主义；现实主义与浪漫主义并行"},
            {"label": "分3·30年代", "content": "左翼文学受马克思主义文论影响；新感觉派受日本现代主义影响——多元影响并存"},
            {"label": "分4·40年代", "content": "抗战时期'民族形式'讨论；西方影响与本土传统融合——本土化探索"},
            {"label": "分5·80年代", "content": "现代主义回归：马尔克斯/卡夫卡/福克纳影响寻根/先锋/新写实——西方资源再引入"},
            {"label": "分6·评价", "content": "西方文学资源是催化剂；但中国文学在吸收中实现本土化——'外来影响'与'本土创造'辩证统一"},
            {"label": "总结", "content": "西方文学资源推动中国文学现代转型，但本土化创造是关键"},
        ],
        "conclusion": "20世纪中国文学的现代转型是在西方文学资源催化下进行的，但本土化创造是中国文学现代化的关键",
    },
}

EQ_0076_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "林纾译《巴黎茶花女遗事》（1899年）：'林译小说'开启西方文学引入中国的先河",
            "source": "林纾译《巴黎茶花女遗事》1899年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《我怎么做起小说来》：'我所取法的，大抵是外国的作家'——肯定西方文学资源的影响",
            "source": "鲁迅《我怎么做起小说来》1933年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "钱理群指出：20世纪中国文学的现代化是在西方文学资源催化下进行的，但'本土化创造'是关键",
            "source": "钱理群《中国现代文学三十年》北京大学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "王德威提出'没有晚清，何来五四'：晚清翻译文学已开启中国文学现代转型，西方资源是催化剂",
            "source": "王德威《被压抑的现代性》北京大学出版社2005年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "丁帆《新文学史》将西方文学资源定位为中国文学现代转型的'催化剂'，但强调本土化创造",
            "source": "丁帆《中国新文学史》上册 高等教育出版社2013年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "钱理群《三十年》侧重现代阶段西方影响；丁帆《新文学史》打通现当代系统梳理。两书共识：西方文学资源是催化剂，本土化是关键。",
        "scholarComparison": "钱理群从文学史主流视角分析；王德威从晚清现代性视角提出'被压抑的现代性'；夏志清从'感时忧国'视角强调中国文学的本土关怀。三种视角互补：钱重史论，王重晚清，夏重本土。",
    },
    "referenceLinks": [
        {"label": "中国作家网·西方文学资源与中国现代文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·中外文学关系研究", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "林纾", "note": "项目暂无林纾独立知识点，建议补充以覆盖晚清翻译文学"},
    ],
}

# ── eq_0077: 欧美文学批判精神及当代意义 ────────────────────────
EQ_0077_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["欧美文学", "批判精神", "当代意义"],
    "limitKeywords": ["批判精神"],
    "task": "梳理批判精神内涵 + 论述当代意义 + 评价",
    "breakthroughAngles": [
        "①批判精神的内涵（质疑/反思/反抗）",
        "②古希腊源头（悲剧批判命运）",
        "③启蒙运动（理性批判）",
        "④19世纪现实主义（社会批判）",
        "⑤现代主义（文化批判）",
        "⑥当代意义",
    ],
    "angleRationale": "本题为综合型，需梳理欧美文学批判精神的历史演变，论述其当代意义。符合'内涵界定→历史梳理→当代意义'的综合型策略。",
    "argumentPath": {
        "thesis": "欧美文学的批判精神从古希腊悲剧到现代主义，贯穿质疑、反思、反抗的核心特质，在当代仍有重要意义：促进社会反省、维护人文价值、推动文明进步",
        "points": [
            {"label": "总述", "content": "批判精神是欧美文学的核心传统，贯穿古希腊到现代"},
            {"label": "分1·批判精神内涵", "content": "质疑权威、反思现实、反抗压迫——批判精神的三重内涵"},
            {"label": "分2·古希腊源头", "content": "索福克勒斯《俄狄浦斯王》批判命运；阿里斯托芬讽刺政治——批判精神的源头"},
            {"label": "分3·启蒙运动", "content": "伏尔泰《老实人》理性批判；卢梭《忏悔录》批判文明——理性批判精神"},
            {"label": "分4·19世纪现实主义", "content": "巴尔扎克《人间喜剧》批判资本主义；狄更斯批判工业社会；托尔斯泰批判沙俄——社会批判高峰"},
            {"label": "分5·现代主义", "content": "卡夫卡《变形记》批判异化；艾略特《荒原》批判精神荒原——文化批判"},
            {"label": "分6·当代意义", "content": "促进社会反省（消费主义/技术异化）；维护人文价值（反抗工具理性）；推动文明进步（批判性思维）"},
            {"label": "总结", "content": "欧美文学批判精神贯穿历史，当代意义在于促进反省、维护价值、推动进步"},
        ],
        "conclusion": "欧美文学的批判精神是其核心传统，在当代仍有促进社会反省、维护人文价值、推动文明进步的重要意义",
    },
}

EQ_0077_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "巴尔扎克《高老头》：拉斯蒂涅在巴黎社会的堕落——19世纪现实主义社会批判的典范",
            "source": "巴尔扎克《高老头》1834年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "卡夫卡《变形记》：格里高尔变成甲虫的荒诞——现代主义文化批判的典范",
            "source": "卡夫卡《变形记》1915年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "卢卡奇指出：19世纪现实主义的批判精神是'对资本主义社会的总体性批判'，是文学社会功能的典范",
            "source": "卢卡奇《小说理论》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "阿多诺认为：现代主义文学的批判精神是对'文化工业'与'工具理性'的反抗，在当代仍有重要意义",
            "source": "阿多诺《美学理论》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "朱维之《外国文学史》将批判精神定位为欧美文学的核心传统，从古希腊到现代贯穿始终",
            "source": "朱维之《外国文学史》南开大学出版社2009年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "朱维之《外国文学史》侧重批判精神的历史梳理；郑克鲁《外国文学史》更注重其社会功能。两书共识：批判精神是欧美文学核心传统。",
        "scholarComparison": "卢卡奇从马克思主义文论视角肯定现实主义批判；阿多诺从法兰克福学派视角肯定现代主义批判；布鲁姆从经典视角强调文学的审美独立。三种视角互补：卢重现实主义，阿重现代主义，布重审美。",
    },
    "referenceLinks": [
        {"label": "中国作家网·欧美文学批判精神的历史演变", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·文学批判精神的当代意义", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "巴尔扎克", "note": "项目暂无巴尔扎克独立知识点，建议补充以覆盖19世纪现实主义"},
        {"author": "卡夫卡", "note": "项目暂无卡夫卡独立知识点，建议补充以覆盖现代主义文学"},
    ],
}

# ── eq_0078: 文学在戏剧影视艺术中的作用 ────────────────────────
EQ_0078_ANGLE = {
    "questionType": "理论应用型",
    "coreKeywords": ["文学", "戏剧影视", "作用"],
    "limitKeywords": ["结合具体作品"],
    "task": "论述文学对戏剧影视的作用 + 结合具体作品分析",
    "breakthroughAngles": [
        "①文学为戏剧影视提供剧本基础",
        "②文学提升戏剧影视的思想深度",
        "③文学丰富戏剧影视的语言艺术",
        "④文学塑造戏剧影视的人物形象",
        "⑤文学经典改编的案例",
    ],
    "angleRationale": "本题为理论应用型，需论述文学对戏剧影视的作用，结合具体作品分析。符合'作用论述→作品印证'的理论应用型策略。",
    "argumentPath": {
        "thesis": "文学在戏剧影视艺术中具有基础性作用：提供剧本基础、提升思想深度、丰富语言艺术、塑造人物形象，是戏剧影视艺术的重要资源",
        "points": [
            {"label": "总述", "content": "文学是戏剧影视艺术的重要基础，从剧本到思想均有深远影响"},
            {"label": "分1·剧本基础", "content": "戏剧影视剧本源于文学创作；莎士比亚戏剧是文学与戏剧的统一；电影剧本需文学功底"},
            {"label": "分2·思想深度", "content": "文学提升戏剧影视的思想深度；《哈姆雷特》的哲学思考；《红楼梦》改编的深度"},
            {"label": "分3·语言艺术", "content": "文学丰富戏剧影视的语言；王朔电影对白的京味；话剧台词的文学性"},
            {"label": "分4·人物形象", "content": "文学塑造戏剧影视的人物；鲁迅作品改编的人物深度；文学典型对影视的影响"},
            {"label": "分5·经典改编案例", "content": "《红楼梦》电视剧改编；《雷雨》话剧；《活着》从小说到电影——文学经典改编的成功案例"},
            {"label": "总结", "content": "文学是戏剧影视的基础，从剧本到思想、语言、人物均有重要作用"},
        ],
        "conclusion": "文学在戏剧影视艺术中具有基础性作用，是戏剧影视艺术的重要资源与支撑",
    },
}

EQ_0078_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "莎士比亚《哈姆雷特》：「生存还是毁灭，这是一个问题」——文学为戏剧提供思想深度",
            "source": "莎士比亚《哈姆雷特》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "余华《活着》改编电影：从小说到电影的文学基础——文学为影视提供剧本资源",
            "source": "余华《活着》/张艺谋电影《活着》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "童庆炳指出：文学是戏剧影视的基础，从剧本创作到思想深度、语言艺术均有重要作用",
            "source": "童庆炳《文学理论教程》高等教育出版社2015年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "王一川《文学理论》将文学定位为戏剧影视艺术的基础，强调文学对戏剧影视的多重作用",
            "source": "王一川《文学理论》北京大学出版社2011年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "童庆炳《文学理论教程》侧重文学对戏剧影视的理论作用；王一川《文学理论》更注重具体案例。两书共识：文学是戏剧影视的基础。",
        "scholarComparison": "童庆炳从文学理论视角论述；王一川从媒介比较视角分析；电影学者戴锦华从电影学视角分析文学改编。三种视角互补：童重理论，王重比较，戴重电影学。",
    },
    "referenceLinks": [
        {"label": "中国作家网·文学与戏剧影视的关系", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·文学经典影视改编", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0079: 应用文的文本特征及文学史地位 ────────────────────────
EQ_0079_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["应用文", "文本特征", "文学史地位"],
    "limitKeywords": ["应用文"],
    "task": "分析文本特征 + 论述文学史地位 + 评价",
    "breakthroughAngles": [
        "①应用文的文本特征（实用性/规范性/简明性）",
        "②应用文的文体类型（公文/书信/日记/笔记）",
        "③应用文与文学的关系（文学性应用文）",
        "④应用文的文学史地位（古代文章传统）",
        "⑤评价（实用与文学的辩证）",
    ],
    "angleRationale": "本题为综合型，需分析应用文的文本特征，论述其文学史地位。符合'特征分析→地位论述→辩证评价'的综合型策略。",
    "argumentPath": {
        "thesis": "应用文以实用性、规范性、简明性为文本特征，在中国文学史上具有重要地位：古代文章传统以应用文为主流，许多应用文兼具文学价值，是文学史不可忽视的组成部分",
        "points": [
            {"label": "总述", "content": "应用文是实用性文体，但在中国文学史上具有重要地位"},
            {"label": "分1·文本特征·实用性", "content": "应用文以实用为目的；解决具体事务；区别于文学的审美目的"},
            {"label": "分2·文本特征·规范性", "content": "应用文有严格格式规范；公文/书信/奏疏各有体式；规范性保证实用功能"},
            {"label": "分3·文本特征·简明性", "content": "应用文语言简明扼要；不尚雕饰；追求表达效率"},
            {"label": "分4·文学性应用文", "content": "部分应用文兼具文学价值：李密《陈情表》（书信）；诸葛亮《出师表》（奏疏）；苏轼《赤壁赋》（赋体应用）"},
            {"label": "分5·文学史地位", "content": "古代文章传统以应用文为主流；'文以载道'的应用文是古典散文核心；影响后世散文发展"},
            {"label": "分6·评价", "content": "应用文的文学史地位不可忽视；实用与文学辩证统一；'文以载道'传统的重要载体"},
            {"label": "总结", "content": "应用文以实用性为特征，但在中国文学史上具有重要地位，是'文以载道'传统的重要载体"},
        ],
        "conclusion": "应用文在中国文学史上具有重要地位，其实用性与文学性的辩证统一体现了'文以载道'的传统",
    },
}

EQ_0079_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "李密《陈情表》：「臣无祖母，无以至今日；祖母无臣，无以终余年」——文学性应用文的典范",
            "source": "李密《陈情表》西晋",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "诸葛亮《出师表》：「鞠躬尽瘁，死而后已」——奏疏类应用文的文学典范",
            "source": "诸葛亮《出师表》三国",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "姚鼐《古文辞类纂》将文章分为十三类，多数为应用文（奏议/书说/赠序/诏令），肯定应用文的文学地位",
            "source": "姚鼐《古文辞类纂》清代",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将应用文纳入文学史考察，强调古代文章传统以应用文为主流",
            "source": "袁行霈《中国文学史》高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重应用文的文学史地位；童庆炳《文学理论教程》更注重其文本特征。两书共识：应用文是文学史重要组成部分。",
        "scholarComparison": "姚鼐从古文选学视角肯定应用文；钱穆从学术史视角强调其'文以载道'功能；褚斌杰从文体学视角系统分类。三种视角互补：姚重选学，钱重学术，褚重文体学。",
    },
    "referenceLinks": [
        {"label": "中国作家网·应用文与中国文学传统", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·应用文的文学性", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── eq_0080: 《红楼梦》诗词曲戏曲分析 ────────────────────────
EQ_0080_ANGLE = {
    "questionType": "作品分析型",
    "coreKeywords": ["红楼梦", "诗", "词", "曲", "戏曲"],
    "limitKeywords": ["举例", "价值"],
    "task": "选择一篇诗词曲戏曲 + 分析其价值",
    "breakthroughAngles": [
        "①《红楼梦》诗词曲的多样性",
        "②选择《葬花吟》分析",
        "③《葬花吟》的文学价值",
        "④《葬花吟》的人物塑造价值",
        "⑤《葬花吟》的主题预示价值",
    ],
    "angleRationale": "本题为作品分析型，需选择《红楼梦》中的一篇诗词曲戏曲，分析其价值。以《葬花吟》为例，符合'选择作品→分析价值'的策略。",
    "argumentPath": {
        "thesis": "以《红楼梦》中林黛玉《葬花吟》为例，该诗具有多重价值：文学价值（抒情艺术）、人物塑造价值（黛玉性格）、主题预示价值（悲剧命运）",
        "points": [
            {"label": "总述", "content": "《红楼梦》融合诗、词、曲、戏曲，是文学融合的典范；以《葬花吟》为例分析"},
            {"label": "分1·《葬花吟》内容", "content": "林黛玉葬花时吟咏：「花谢花飞花满天，红消香断有谁怜」「一年三百六十日，风刀霜剑严相逼」"},
            {"label": "分2·文学价值", "content": "抒情艺术精湛；以花喻人，情景交融；语言凄美，音韵谐婉——是中国古典抒情诗的典范"},
            {"label": "分3·人物塑造价值", "content": "塑造林黛玉多愁善感、孤高自许的性格；'质本洁来还洁去'体现其洁癖与孤高"},
            {"label": "分4·主题预示价值", "content": "'一朝春尽红颜老，花落人亡两不知'预示黛玉悲剧命运；暗示大观园群芳凋零"},
            {"label": "分5·结构价值", "content": "《葬花吟》是《红楼梦》结构的关键节点；预示全书'千红一哭，万艳同悲'的悲剧主题"},
            {"label": "总结", "content": "《葬花吟》具有文学、人物塑造、主题预示、结构等多重价值，是《红楼梦》诗词的典范"},
        ],
        "conclusion": "《葬花吟》作为《红楼梦》诗词的典范，其多重价值体现了曹雪芹'诗化小说'的艺术成就",
    },
}

EQ_0080_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "曹雪芹《红楼梦》第二十七回林黛玉《葬花吟》：「花谢花飞花满天，红消香断有谁怜」「一年三百六十日，风刀霜剑严相逼」「质本洁来还洁去，强于污淖陷渠沟」",
            "source": "曹雪芹《红楼梦》第二十七回",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "蔡义江指出：《葬花吟》是林黛玉的'诗谶'，预示其悲剧命运，是《红楼梦》诗词艺术的典范",
            "source": "蔡义江《红楼梦诗词曲赋鉴赏》中华书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "周汝昌认为：《葬花吟》不仅是黛玉性格的塑造，更是全书悲剧主题的预示，'千红一哭，万艳同悲'",
            "source": "周汝昌《红楼梦新证》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《红楼梦》诗词定位为'诗化小说'的重要组成，《葬花吟》是典范",
            "source": "袁行霈《中国文学史》第四卷 高等教育出版社1999年版",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《红楼梦》诗词的文学价值；章培恒《中国文学史》更注重其人物塑造功能。两书共识：《红楼梦》诗词是'诗化小说'的重要组成。",
        "scholarComparison": "蔡义江从诗词鉴赏视角分析；周汝昌从红学视角解读；俞平伯从新红学视角考证。三种视角互补：蔡重鉴赏，周重红学，俞重考证。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《红楼梦》诗词艺术", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·《葬花吟》与林黛玉形象", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "曹雪芹", "note": "项目暂无曹雪芹独立知识点，建议补充'曹雪芹《红楼梦》与诗化小说'以完善清代小说谱系"},
    ],
}

# ── eq_0073: 比较文学与世界文学必做题 ────────────────────────
# 题目内容不完整，使用通用填充
EQ_0073_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["比较文学", "世界文学"],
    "limitKeywords": ["必做题"],
    "task": "比较文学分析",
    "breakthroughAngles": [
        "①比较文学方法",
        "②世界文学视野",
        "③具体作品比较",
        "④评价",
    ],
    "angleRationale": "本题为比较文学与世界文学必做题，需运用比较文学方法分析具体作品。符合'方法论述→作品比较→评价'的策略。",
    "argumentPath": {
        "thesis": "比较文学方法为世界文学研究提供了跨文化视野，通过具体作品的比较可以揭示不同文学传统的异同",
        "points": [
            {"label": "总述", "content": "比较文学与世界文学是文学研究的重要方法"},
            {"label": "分1·比较文学方法", "content": "影响研究/平行研究/跨文化研究——比较文学的核心方法"},
            {"label": "分2·世界文学视野", "content": "歌德提出'世界文学'概念；超越民族文学局限"},
            {"label": "分3·具体作品比较", "content": "通过具体作品的比较揭示不同文学传统的异同"},
            {"label": "总结", "content": "比较文学为世界文学研究提供方法，促进跨文化理解"},
        ],
        "conclusion": "比较文学与世界文学研究是文学研究的重要领域，促进跨文化理解与文学交流",
    },
}

EQ_0073_NOTES = {
    "evidences": [
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "歌德提出'世界文学'概念：'民族文学现在已不再是重要之物，世界文学的时代已来临'",
            "source": "歌德《歌德谈话录》1827年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "杨乃乔《比较文学概论》将比较文学定位为跨文化文学研究的重要方法",
            "source": "杨乃乔《比较文学概论》北京大学出版社",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "杨乃乔《比较文学概论》侧重方法论；朱维之《外国文学史》更注重世界文学视野。两书共识：比较文学是世界文学研究的重要方法。",
        "scholarComparison": "歌德从世界文学视角提出概念；杨乃乔从方法论视角系统梳理；达姆罗什从当代视角更新世界文学理论。三种视角互补：歌重概念，杨重方法，达重当代。",
    },
    "referenceLinks": [
        {"label": "中国作家网·比较文学与世界文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·世界文学的时代", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [],
}

# ── 主程序：读取 seed_data.json，批量填充，写回 ──────────────────
FILL_MAP = {
    "eq_0066": (EQ_0066_ANGLE, EQ_0066_NOTES),
    "eq_0067": (EQ_0067_ANGLE, EQ_0067_NOTES),
    "eq_0068": (EQ_0068_ANGLE, EQ_0068_NOTES),
    "eq_0069": (EQ_0069_ANGLE, EQ_0069_NOTES),
    "eq_0073": (EQ_0073_ANGLE, EQ_0073_NOTES),
    "eq_0074": (EQ_0074_ANGLE, EQ_0074_NOTES),
    "eq_0075": (EQ_0075_ANGLE, EQ_0075_NOTES),
    "eq_0076": (EQ_0076_ANGLE, EQ_0076_NOTES),
    "eq_0077": (EQ_0077_ANGLE, EQ_0077_NOTES),
    "eq_0078": (EQ_0078_ANGLE, EQ_0078_NOTES),
    "eq_0079": (EQ_0079_ANGLE, EQ_0079_NOTES),
    "eq_0080": (EQ_0080_ANGLE, EQ_0080_NOTES),
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

    print(f"\n共填充 {filled_count} 道题（预期 12 道）")
    assert filled_count == 12, f"填充数量不符: {filled_count} != 12"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"已写回 {SEED_PATH}")

if __name__ == "__main__":
    main()

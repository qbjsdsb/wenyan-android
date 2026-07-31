#!/usr/bin/env python3
"""
为 604/605 卷 2007/2009 年论述题批量填充 angle + notes 字段（2 道）。

题目清单：
- eq_0019 (2007, 604) 三大专业论述题合集：
  · 古代·《红楼梦》艺术成就
  · 现当代·中国现当代抒情小说的艺术发展
  · 外国·19世纪欧美文学的发展脉络和成就
- eq_0057 (2009, 605) 三大专业论述题合集：
  · 古代·《史记》"史家之绝唱，无韵之离骚"
  · 现当代·中国现当代小说对知识分子形象的塑造
  · 外国·希腊精神与希伯来精神在欧美文学史中的体现与差异

填充标准：对齐示例题结构
- angle: questionType/coreKeywords/limitKeywords/task/breakthroughAngles/angleRationale/argumentPath
- notes: evidences/crossValidation/referenceLinks/knowledgeGaps

注意：这两道题为三大专业论述题合集，每题包含3个子问题（古代/现当代/外国），
angle与notes需综合覆盖三个子问题，便于考研学生跨专业复习。
"""
import json
from pathlib import Path

SEED_PATH = Path("/workspace/app/src/main/assets/seed_data.json")

# ── eq_0019: 2007年604卷三大专业论述题合集（综合型）─────────────
EQ_0019_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["红楼梦", "抒情小说", "19世纪欧美文学", "艺术成就", "发展脉络"],
    "limitKeywords": ["三大专业必做", "每题30分"],
    "task": "三选一作答：红楼梦艺术成就 / 抒情小说发展 / 19世纪欧美脉络",
    "breakthroughAngles": [
        "①古代·《红楼梦》艺术成就（人物/结构/叙事/语言/悲剧）",
        "②现当代·抒情小说发展（五四/30年代/40年代/80年代/90年代）",
        "③外国·19世纪欧美脉络（浪漫主义/批判现实主义/自然主义/唯美/象征）",
    ],
    "angleRationale": "本题为三大专业论述题合集，考生按专业三选一。综合覆盖三个方向便于跨专业复习。符合'综合梳理→分期/分维度展开→意义评价'的策略。",
    "argumentPath": {
        "thesis": "2007年604卷论述题三选一：《红楼梦》以人物塑造、网状结构、悲剧意识取得艺术巅峰；中国现当代抒情小说经历五四、30年代、80年代等多阶段发展；19世纪欧美文学经历浪漫主义、批判现实主义、自然主义等流派演变",
        "points": [
            {"label": "总述", "content": "本题为三大专业论述题合集，三选一作答"},
            {"label": "古代·《红楼梦》人物塑造", "content": "四百多人物个性鲜明，打破'千人一面'；宝黛的叛逆、王熙凤的精明"},
            {"label": "古代·《红楼梦》结构与叙事", "content": "网状结构，主线宝黛爱情与贾府盛衰交织，'草蛇灰线，伏脉千里'；全知与限知结合，'假语村言'"},
            {"label": "古代·《红楼梦》语言与悲剧", "content": "白话与诗词曲赋交融，雅俗共赏；'千红一哭，万艳同悲'，打破大团圆模式"},
            {"label": "现当代·五四抒情小说", "content": "鲁迅《伤逝》、郁达夫《沉沦》开抒情小说先河"},
            {"label": "现当代·30-40年代抒情小说", "content": "沈从文《边城》田园牧歌抒情、萧红《呼兰河传》散文化叙事；萧红、师陀延续抒情传统"},
            {"label": "现当代·80-90年代抒情小说", "content": "汪曾祺《受戒》复归抒情、张承志《黑骏马》、阿城《棋王》；迟子建、苏童《妻妾成群》融入抒情"},
            {"label": "外国·浪漫主义", "content": "19世纪初-30年代：拜伦、雪莱、雨果、普希金、惠特曼"},
            {"label": "外国·批判现实主义", "content": "30年代-末：巴尔扎克《人间喜剧》、狄更斯、托尔斯泰、陀思妥耶夫斯基"},
            {"label": "外国·自然主义唯美象征", "content": "后期：左拉自然主义；王尔德唯美主义；波德莱尔《恶之花》象征主义"},
            {"label": "总结", "content": "三个方向分别覆盖古代艺术成就、现当代发展脉络、外国文学流派"},
        ],
        "conclusion": "本题为三大专业必做论述题，三选一，覆盖古代/现当代/外国文学核心知识点",
    },
}

EQ_0019_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《红楼梦》第五回：'千红一哭，万艳同悲'——悲剧意识的纲领，打破大团圆模式",
            "source": "曹雪芹《红楼梦》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《伤逝》：'人必生活着，爱才有所附丽'——五四抒情小说的开山之作",
            "source": "鲁迅《伤逝》1925年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "沈从文《边城》：'由四川过湖南去，靠东有一条官路。这官路将近湘西边境到了一个地方名为茶峒的小山城时'——田园牧歌抒情的典范",
            "source": "沈从文《边城》1934年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "巴尔扎克《人间喜剧·前言》：'法国社会将成为历史家，我只应当充当它的秘书'——批判现实主义的宣言",
            "source": "巴尔扎克《人间喜剧·前言》1842年法文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅评《红楼梦》：'自有《红楼梦》出来以后，传统的思想和写法都打破了'——对其艺术革命性的经典评价",
            "source": "鲁迅《中国小说史略》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "杨义指出：中国现当代抒情小说经历五四启蒙抒情、30年代田园抒情、80年代散文化抒情多阶段发展，沈从文、汪曾祺是其关键节点",
            "source": "杨义《中国现代小说史》人民文学出版社1998年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》将《红楼梦》定位为中国古典小说巅峰；钱理群《三十年》梳理抒情小说发展；朱维之《外国文学史》梳理19世纪欧美文学脉络",
            "source": "袁行霈/钱理群/朱维之三教材",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《红楼梦》的文学史地位；钱理群《三十年》侧重抒情小说发展；朱维之《外国文学史》侧重19世纪欧美脉络。三教材分工覆盖三个方向。",
        "scholarComparison": "鲁迅从小说史视角评价《红楼梦》；杨义从现代小说史视角分析抒情小说；勃兰兑斯从欧洲文学史视角梳理19世纪。三种视角互补：鲁重古，杨重现，勃重外。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《红楼梦》艺术成就", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·抒情小说的发展", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "曹雪芹", "note": "项目暂无曹雪芹独立知识点，建议补充'曹雪芹《红楼梦》与古典小说巅峰'以完善清代文学谱系"},
        {"author": "沈从文", "note": "项目暂无沈从文独立知识点，建议补充'沈从文《边城》与京派小说'以覆盖30年代文学谱系"},
    ],
}

# ── eq_0057: 2009年605卷三大专业论述题合集（综合型）─────────────
EQ_0057_ANGLE = {
    "questionType": "综合型",
    "coreKeywords": ["史记", "知识分子形象", "希腊精神", "希伯来精神"],
    "limitKeywords": ["三大专业必做", "30分"],
    "task": "三选一作答：史记评价 / 知识分子形象塑造 / 希腊与希伯来精神",
    "breakthroughAngles": [
        "①古代·《史记》'史家之绝唱，无韵之离骚'（史学+文学双重价值）",
        "②现当代·知识分子形象塑造（五四/30年代/50-70年代/80-90年代）",
        "③外国·希腊精神与希伯来精神（理性vs信仰/现世vs彼岸/人文vs神本）",
    ],
    "angleRationale": "本题为三大专业论述题合集，考生按专业三选一。综合覆盖三个方向便于跨专业复习。符合'综合梳理→分期/分维度展开→意义评价'的策略。",
    "argumentPath": {
        "thesis": "2009年605卷论述题三选一：鲁迅评《史记》'史家之绝唱，无韵之离骚'揭示其史学与文学双重价值；中国现当代小说对知识分子形象的塑造经历启蒙者-革命者-受难者-反思者演变；希腊精神与希伯来精神构成西方文化两大源头",
        "points": [
            {"label": "总述", "content": "本题为三大专业论述题合集，三选一作答"},
            {"label": "古代·史家之绝唱", "content": "史学价值——开创纪传体，十二本纪三十世家七十列传十表八书；'究天人之际，通古今之变，成一家之言'；记载上起黄帝下至汉武帝三千年历史"},
            {"label": "古代·无韵之离骚", "content": "文学价值——人物塑造栩栩如生，项羽'力拔山兮气盖世'、荆轲'风萧萧兮易水寒'；'互见法'刻画人物；叙事'不虚美，不隐恶'"},
            {"label": "古代·鲁迅评价", "content": "鲁迅八字评价揭示《史记》史学与文学双重价值，是中国史传文学的最高峰"},
            {"label": "现当代·五四知识分子", "content": "鲁迅《在酒楼上》《孤独者》写彷徨知识分子"},
            {"label": "现当代·30-40年代知识分子", "content": "茅盾《蚀》三部曲写大革命失败后知识分子动摇；叶圣陶《倪焕之》写知识分子革命之路；路翎《财主底儿女们》写抗战中知识分子精神探索"},
            {"label": "现当代·50-80年代知识分子", "content": "杨沫《青春之歌》写林道静从个人主义到革命者；张贤亮《绿化树》写右派知识分子苦难；王朔写痞子化反叛"},
            {"label": "外国·希腊精神", "content": "阿波罗精神：理性、和谐、节制、现世、人文主义；荷马史诗英雄主义、希腊悲剧的命运观、亚里士多德'中道'"},
            {"label": "外国·希伯来精神", "content": "耶稣精神：信仰、救赎、超越、禁欲、彼岸；《圣经·约伯记》信仰考验、奥古斯丁《忏悔录》、中世纪宗教文学"},
            {"label": "外国·二者差异与融合", "content": "理性vs信仰、现世vs彼岸、人文vs神本；文艺复兴后两者融合，构成西方文化两大源头"},
            {"label": "总结", "content": "三个方向分别覆盖古代史学文学、现当代知识分子、外国文化精神"},
        ],
        "conclusion": "本题为三大专业必做论述题，三选一，覆盖古代/现当代/外国文学核心知识点",
    },
}

EQ_0057_NOTES = {
    "evidences": [
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "司马迁《史记·太史公自序》：'究天人之际，通古今之变，成一家之言'——史学的最高抱负",
            "source": "司马迁《史记·太史公自序》中华书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《史记·项羽本纪》：项羽'力拔山兮气盖世'——人物塑造的典范",
            "source": "司马迁《史记·项羽本纪》中华书局",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "鲁迅《孤独者》：魏连殳的悲剧——五四后彷徨知识分子的写照",
            "source": "鲁迅《孤独者》1925年",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "WORK_TEXT",
            "label": "作品原文",
            "content": "《圣经·约伯记》：约伯虽受苦难仍坚守信仰——希伯来精神信仰考验的典范",
            "source": "《圣经·约伯记》",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "鲁迅《汉文学史纲要》评《史记》：'史家之绝唱，无韵之《离骚》'——史学与文学双重价值的经典评价",
            "source": "鲁迅《汉文学史纲要》人民文学出版社",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "马修·阿诺德《文化与无政府状态》指出：希腊精神（Hebraism）与希伯来精神（Hellenism）是西方文化两大源头，前者重理性与事物本来面目，后者重信仰与行为",
            "source": "马修·阿诺德《文化与无政府状态》1869年英文版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "SCHOLAR_OPINION",
            "label": "学者观点",
            "content": "陈思和指出：中国现当代小说的知识分子形象经历启蒙者（五四）-革命者（30-50年代）-受难者（50-70年代）-反思者（80-90年代）的演变",
            "source": "陈思和《中国当代文学史教程》复旦大学出版社1999年版",
            "linkedKnowledgePointId": None,
        },
        {
            "type": "TEXTBOOK_CONSENSUS",
            "label": "教材定论",
            "content": "袁行霈《中国文学史》定位《史记》为史传文学巅峰；钱理群《三十年》梳理知识分子形象；朱维之《外国文学史》梳理希腊与希伯来精神",
            "source": "袁行霈/钱理群/朱维之三教材",
            "linkedKnowledgePointId": None,
        },
    ],
    "crossValidation": {
        "textbookComparison": "袁行霈《中国文学史》侧重《史记》的史学文学双重价值；钱理群《三十年》侧重知识分子形象演变；朱维之《外国文学史》侧重希腊与希伯来精神。三教材分工覆盖三个方向。",
        "scholarComparison": "鲁迅从文学史视角评价《史记》；陈思和从当代文学史视角分析知识分子形象；马修·阿诺德从文化学视角分析两大精神。三种视角互补：鲁重古，陈重现，马重外。",
    },
    "referenceLinks": [
        {"label": "中国作家网·《史记》的史学与文学", "url": "https://www.chinawriter.com.cn"},
        {"label": "中国文艺评论网·知识分子形象的演变", "url": "https://www.zgwypl.com"},
    ],
    "knowledgeGaps": [
        {"author": "司马迁", "note": "项目暂无司马迁独立知识点，建议补充'司马迁《史记》与纪传体通史'以完善汉代文学谱系"},
    ],
}

# ── 主程序 ──────────────────────────────────────────────
FILL_MAP = {
    "eq_0019": (EQ_0019_ANGLE, EQ_0019_NOTES),
    "eq_0057": (EQ_0057_ANGLE, EQ_0057_NOTES),
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

    print(f"\n共填充 {filled_count} 道题（预期 2 道）")
    assert filled_count == 2, f"填充数量不符: {filled_count} != 2"

    with open(SEED_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"已写回 {SEED_PATH}")


if __name__ == "__main__":
    main()

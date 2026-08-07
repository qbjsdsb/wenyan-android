#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""整理并严格合并 2023—2026 年南师大文学类真题。

原则：
1. 2023 年压缩包内原始试卷与 2024—2026 年公开回忆资料分级记录；
2. 题目逐题保留答案框架，但不把无法复核的 805 外国文学题目猜写进 App；
3. 旧真题 ID、正文、答案框架和知识点数据不改，只追加 eq_0482 之后的新题；
4. 只有显式传入 --apply 才写入 seed_data.json。
"""

from __future__ import annotations

import argparse
import copy
import json
import re
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
SEED_PATH = REPO_ROOT / "app/src/main/assets/seed_data.json"
CANDIDATE_PATH = REPO_ROOT / "tools/content_supplement/exam_2023_2026_candidates_v2_22.json"
REPORT_PATH = REPO_ROOT / "docs/research/exam-2023-2026-v2.22.json"

VALID_SUBJECTS = {"中国古代文学", "中国现当代文学", "外国文学", "文学理论"}
VALID_TYPES = {
    "TERM_EXPLANATION",
    "SHORT_ANSWER",
    "ESSAY",
    "WRITING",
    "ANALYSIS",
}


def q(subject: str, question_type: str, content: str, framework: str, score: int = 0) -> dict[str, Any]:
    return {
        "subject": subject,
        "question_type": question_type,
        "content": content,
        "score": score,
        "answer_framework": framework,
    }


def build_groups() -> list[dict[str, Any]]:
    groups: list[dict[str, Any]] = []

    def add_group(
        key: str,
        year: int,
        code: str,
        confidence: str,
        source_kind: str,
        source_locator: str,
        note: str,
        questions: list[dict[str, Any]],
    ) -> None:
        groups.append(
            {
                "key": key,
                "year": year,
                "exam_paper_code": code,
                "confidence": confidence,
                "source_kind": source_kind,
                "source_locator": source_locator,
                "note": note,
                "questions": questions,
            }
        )

    local_source = "tools_unpacked/output/file_033.json（TEXTBOOK_NATIVE，压缩包原始试卷）"
    add_group(
        "2023_610_local",
        2023,
        "610",
        "HIGH",
        "local_native_exam",
        local_source,
        "压缩包中 2023 年南师大原始试卷，题干、分值和科目分区均可复现。",
        [
            q("中国古代文学", "SHORT_ANSWER", "1. 举例论述南朝乐府民歌艺术和北朝乐府民歌思想内容。", "①南朝乐府以吴声、西曲为主，多写爱情与日常情感，体制短小，语言清丽，多用双关隐语，代表作有《子夜歌》《西洲曲》。②北朝乐府题材更广，集中表现战争、游牧生活、爱情婚姻和女性命运，风格质朴豪健，代表作《木兰诗》《敕勒歌》。③结合作品说明南北民歌的地域气质与社会生活差异，最后指出二者共同丰富了五言诗传统。", 15),
            q("中国古代文学", "SHORT_ANSWER", "2. 论述周邦彦词艺术特点。", "①周邦彦集北宋婉约词之大成，长于慢词铺叙。②结构上善于时空转换、回环曲折和层层深入，常以写景推动抒情。③语言典雅精工，善炼字、用典、化用前人诗句，音律和声韵配合严密。④情感多写羁旅、离愁与个人身世，但含有深厚的历史与审美感。⑤说明其对南宋姜夔、吴文英等格律词人的影响。", 20),
            q("中国古代文学", "SHORT_ANSWER", "3. 分析《儒林外史》中三类否定性人物形象对科举制度和功名利禄观念的批判。", "①可从科举痴迷型、逐利堕落型和假名士/伪君子型组织材料：周进、范进表现科举制度对人格的异化，匡超人表现士人由朴实到逐名逐利的蜕变，严贡生等表现功名话语与贪婪、虚伪的结合。②结合夸张、对比、白描和冷峻反讽分析人物。③指出作品并非否定一切读书人，而是批判八股取士、功名观和礼教名目的相互勾连，并以王冕、杜少卿等正面形象形成反衬。", 15),
            q("中国现当代文学", "SHORT_ANSWER", "4. 鲁迅说冯至是“中国最杰出的抒情诗人”，请结合其创作分析诗歌特色。", "①结合《昨日之歌》《十四行集》等说明冯至抒情由青春感伤转向对生命、自然、历史和个体存在的哲思。②诗歌善于把日常经验提升为存在体验，意象凝练，常以自然物承载时间意识和生命感。③《十四行集》吸收十四行诗形式，又融入中国古典诗歌的含蓄与节奏，形成思辨抒情、沉静内省的风格。④可补充诗人对战争、民族和人的尊严的关注，说明其抒情的个人性与时代性。", 15),
            q("中国现当代文学", "SHORT_ANSWER", "5. “新写实小说”是20世纪八九十年代的重要小说流派，请结合代表性作家作品，分析新写实小说的特点和局限。", "①代表作家可举池莉、方方、刘震云、刘恒等，作品把普通人的日常生存、物质压力和琐屑经验置于叙事中心。②艺术上采用平实克制的叙述、细节堆积、生活流结构和少加评判的叙事姿态，弱化传奇性与宏大激情。③它拓展了现实主义的日常维度，恢复了普通人的经验和欲望。④局限是可能把生活平面化、把生存逻辑自然化，历史纵深、价值追问和形式创新有时不足，需结合具体作品辩证评价。", 15),
            q("文学理论", "SHORT_ANSWER", "6. 结合具体作品，分析你对“典型环境中典型人物”的理解。", "①典型人物不是抽象类型，而是在独特个性中集中体现一定社会关系、时代精神和阶级/群体经验的人物形象。②典型环境包括人物活动的社会关系、历史条件、生活场域和具体情境；它既制约人物，又由人物行动显出矛盾。③典型化要求个性化与概括性统一、环境与人物互动，不能把典型等同于标签。④可结合《阿Q正传》、巴尔扎克小说或《红楼梦》说明人物如何在具体关系中获得典型意义。", 20),
            q("文学理论", "SHORT_ANSWER", "7. 期待视野有哪几个层次？请具体分析。", "①读者个人层：由生活经历、审美经验、阅读记忆形成的期待。②社会历史层：共同体在特定时代形成的价值观、情感结构和文化心理。③文学艺术层：由体裁规范、语言惯例、母题传统和已有作品形成的形式期待。④新作品既可能满足、部分满足，也可能改变或打破这些期待；期待视野的变化说明文学接受具有历史性和主体性。", 20),
            q("外国文学", "SHORT_ANSWER", "8. 塞万提斯小说创作的艺术特色是什么？", "①以《堂吉诃德》为核心，利用骑士小说的模仿、戏仿与反讽形成现实和幻想的对照。②堂吉诃德与桑丘既有理想/现实差异，又在行动中相互影响，人物具有复杂性和发展性。③采用框架、插入故事、多重叙述和元小说式自反，打破单一叙事权威。④语言兼具口语性、幽默和多种文体，广泛展示西班牙社会。⑤指出其把传统冒险叙事推进为现代小说的重要贡献。", 15),
            q("外国文学", "SHORT_ANSWER", "9. 什么是“社会问题剧”？请举例说明。", "①社会问题剧以当代社会矛盾、伦理冲突和制度问题为核心，把舞台作为公共讨论空间，重视现实环境、对白辩论和问题揭示。②代表可举易卜生《玩偶之家》《群鬼》、萧伯纳《华伦夫人的职业》等。③分析《玩偶之家》中家庭制度与女性主体意识的冲突，说明人物冲突如何转化为社会问题。④评价其现实批判、启蒙意义，同时注意过度论辩和人物功能化的可能。", 15),
        ],
    )

    add_group(
        "2023_805_local",
        2023,
        "805",
        "HIGH",
        "local_native_exam",
        local_source,
        "压缩包中 2023 年 805 外国文学史原始试卷，名词、简答和论述均可复现。",
        [
            q("外国文学", "TERM_EXPLANATION", "1. 希腊神话", "①古希腊关于神、英雄和宇宙起源的神话体系，包含奥林匹斯神系与英雄传说。②神祇具有人的形态、欲望和性格，体现古希腊人的人本意识，同时保留命运、神谕和自然崇拜因素。③它为荷马史诗、古希腊悲剧和后世欧洲文学提供了人物、母题与象征资源。", 6),
            q("外国文学", "TERM_EXPLANATION", "2. 坎特伯雷故事集", "①英国作家乔叟的诗体故事集，写朝圣者赴坎特伯雷途中讲故事。②以总引和故事套故事构成框架，汇集骑士、商人、教士、工匠等社会阶层，形成中世纪英国社会的横截面。③语言以中古英语为主，人物语言个性鲜明，既有宗教传统又有世俗生活和人文因素，是英国现实主义叙事的重要先声。", 6),
            q("外国文学", "TERM_EXPLANATION", "3. 湖畔派", "①18世纪末至19世纪初英国第一代浪漫主义诗人群体，代表为华兹华斯、柯勒律治和骚塞。②因多在英国湖区生活或写作而得名，1798年《抒情歌谣集》及其序言是重要标志。③重视自然、情感和普通人的生活，反对古典主义的理性束缚，倡导较为平易的诗歌语言。", 6),
            q("外国文学", "TERM_EXPLANATION", "4. 迷惘的一代", "①第一次世界大战后美国作家群体及其文学现象，名称源自格特鲁德·斯泰因，海明威《太阳照常升起》使之广为人知。②作品表现战争创伤、价值失落、精神漂泊和对传统文明的怀疑。③代表作家有海明威、菲茨杰拉德、帕索斯等，艺术上常以简洁语言、反英雄人物和现代都市经验呈现战后困境。", 6),
            q("外国文学", "TERM_EXPLANATION", "5. 表现主义文学", "①20世纪初在德国、奥地利兴起的现代主义文学思潮，反对自然主义式的外部摹写。②强调主观感受、内心焦虑和精神真实，常用变形、夸张、象征、梦境和强烈对比表现现代人的异化。③涉及戏剧、小说、诗歌等多种体裁，卡夫卡等作家的部分创作具有表现主义特征。", 6),
            q("外国文学", "SHORT_ANSWER", "1. 欧里庇得斯的美狄亚人物形象。", "①美狄亚既是被爱情和婚姻背叛的女性，也是具有理性、意志和行动能力的悲剧主体。②她在爱、母性、尊严与复仇之间撕裂，既有清醒的自我意识，也被激情推向极端。③形象揭示古希腊父权婚姻与女性处境，突破单纯善恶评价。④欧里庇得斯把神话人物世俗化、心理化，使悲剧从神意冲突转向人的情感和社会关系。", 15),
            q("外国文学", "SHORT_ANSWER", "2. 狄更斯《双城记》的艺术特色。", "①以伦敦和巴黎的对照组织空间与历史叙事，写法国大革命前后社会矛盾。②采用复调式人物线索、悬念和伏笔，形成强烈的戏剧性。③通过卡顿与达尔内的对照、替身和牺牲塑造人物，突出“爱与救赎”。④反复意象、对比、象征和富有节奏的叙述增强感染力。⑤评价作品既批判贵族压迫，也反思暴力复仇的循环，体现狄更斯的人道主义立场。", 15),
            q("外国文学", "SHORT_ANSWER", "3. 法国前期象征主义的成就和特点。", "①以前期象征主义诗人波德莱尔、魏尔伦、兰波、马拉美为代表，继承并推进象征和通感传统。②反对自然主义的外部摹写，主张通过暗示、象征、音乐性和联想表现不可直陈的内心与“对应”关系。③重视词语声音、节奏、色彩和意象之间的互感，追求诗的独立性。④它拓展了现代诗歌的表现空间，但也可能因过度封闭和晦涩削弱公共沟通。", 15),
            q("外国文学", "SHORT_ANSWER", "4. 伍尔夫《到灯塔去》的结构。", "①全书分为《窗》《岁月流逝》《灯塔》三部分，构成家庭生活、战争时间和重返灯塔的三段式结构。②中段以高度压缩的叙述写岁月、死亡和历史从家庭空间中流逝。③两端重在人物意识流、内心独白和多重视角，外部事件与心理时间交织。④灯塔既是具体目的地，又象征愿望、记忆、失落与重新理解，结构由延宕、断裂走向有限的完成。", 15),
            q("外国文学", "ESSAY", "1. 拉伯雷《巨人传》的人文主义价值表现。", "①以巨人形象和夸张、狂欢化叙事肯定人的生命力、身体欲望和现世幸福。②通过对经院哲学、教会教条和旧教育的嘲讽，批判压抑人的制度与蒙昧观念。③理想教育强调观察、实践、广博知识和身心协调，德廉美修道院则以自由原则构想新的共同体。④作品语言丰富、想象奔放、雅俗并陈，体现民间文化与书面文化的融合。⑤结尾应指出其人文主义具有时代局限，仍保留乌托邦和男性中心色彩。", 20),
            q("外国文学", "ESSAY", "2. 以《新爱洛伊丝》为例，谈谈卢梭的创作艺术。", "①书信体让人物以第一人称直接呈现情感和思想，形成多声部和亲密的心理表达。②以爱情、自然、教育和社会等级冲突组织情节，把个人真情置于社会规范中考察。③善于写景抒情，使自然成为情感的回声和道德想象的空间。④细腻的心理描写、内心辩论和道德自省构成感伤主义特色。⑤作品在歌颂真情、自然和个体主体性的同时，也以婚姻伦理和社会责任限制纯粹激情，体现卢梭思想的复杂性。", 20),
            q("外国文学", "ESSAY", "3. 《第二十二条军规》是否有隐喻作用，谈谈你的观点。", "①可明确主张它具有多重隐喻：标题中的循环逻辑把战争制度、官僚权力和荒诞生存规则压缩为一种无法逃脱的悖论。②小说以约塞连的逃生愿望和军队不断增加的任务形成循环，表现制度把荒谬伪装成理性。③黑色幽默、碎片化时间、多重叙述和重复场景使战争的死亡与日常行政并置。④它也可隐喻现代社会中权力、资本和组织对个人的异化，但不能把作品简化为单一政治寓言；应结合人物行动和叙事形式说明其开放性。", 20),
        ],
    )

    add_group(
        "2023_801_local",
        2023,
        "801",
        "HIGH",
        "local_native_exam",
        local_source,
        "压缩包中 2023 年 801 专业写作原始试卷，按方向拆成独立可练习题。",
        [
            q("文学理论", "ANALYSIS", "文艺学专业写作：赏析叶芝《旋转》，500字。", "①先界定“旋转/陀螺”意象与历史循环、秩序崩解的关系，再结合诗中意象、语调和节奏细读。②注意诗歌把个人经验置于文明转折和末世想象中，形成冷峻、预言式的抒情。③分析象征、反复、对比和意象跳跃如何制造不稳定感。④结尾评价叶芝把现代历史意识、神话结构与现代诗形式结合的艺术。", 30),
            q("文学理论", "WRITING", "文艺学专业论文：试论“掩耳盗铃”和文学中的虚构，1500字。", "①区分现实中的自欺/欺骗与文学虚构：前者否认事实并造成认识错误，后者是在约定、形式和审美目的中有意识地建构可能世界。②说明文学虚构并非事实复制，而是选择、变形、典型化和象征化；它以不真实的故事揭示真实的人性与社会关系。③结合神话、小说或戏剧说明“相信”是读者进入作品的审美机制，而不是被作者欺骗。④讨论虚构的边界：历史责任、伦理责任、事实写作与文学想象的张力。", 120),
            q("中国古代文学", "ANALYSIS", "中国古代文学专业写作：根据《静夜思》的两个版本，以文学的典型化为主题，写评论文章（不少于800字）。", "①准确比较“看月光/明月光”“山月/明月”等版本差异，分析词语如何改变视觉层次、动作节奏和情感的普遍性。②“典型化”不是简单改得更通俗，而是把具体夜景、动作和乡愁凝聚为可共享的典型经验。③从意象、对偶、复沓、口语化和四句结构说明作品的高度凝练。④兼顾版本流传与接受史，避免把某一版本的流行直接等同于唯一的原作真相。", 60),
            q("中国古代文学", "ANALYSIS", "中国古代文学专业写作：根据作者的写作心态和写作技巧，鉴赏张岱《湖心亭看雪》。", "①从明遗民身份和“痴”的自我意识切入，但不把作者心态简化成单一的亡国哀痛。②分析“大雪三日、万籁俱寂”的氛围、白描和极简数量词，说明远景到近景、整体到微物的镜头式推进。③写舟子评价与作者自我形象的反差，分析偶遇饮酒如何在孤独中形成短暂的知音关系。④结合作品的文言节奏、留白、虚实相生和冷逸审美，说明叙事与抒情的统一。", 90),
            q("中国现当代文学", "WRITING", "中国现当代文学专业写作：王安忆、贾平凹、余华、格非四位作家任选其一，结合创作历程评价其转型得失，并分析其新世纪创作与此前创作的联系和区别。", "①先选定一位作家，列出80年代、90年代和新世纪的创作阶段，避免只作作品罗列。②王安忆可抓都市日常与历史记忆，贾平凹可抓乡土经验与现代转型，余华可抓先锋叙事向历史/现实书写的变化，格非可抓叙事试验、历史想象与知识分子经验。③每阶段都要用代表作证明题意，比较叙事方式、人物观、历史观和语言风格。④结尾评价转型的获得与代价，强调连续性中的变化而非简单“前后断裂”。", 0),
            q("中国现当代文学", "ANALYSIS", "中国现当代文学专业写作：阅读冯至《山水·后记》，写1500字以上文学评论。", "①以文本细读为中心，先概括“山水”在文中的叙事/抒情功能，再联系冯至创作中自然、生命、时间与人的思辨关系。②分析散文的行旅或回望结构、景物描写、议论抒情转换、语言节奏和意象层次。③把山水既看作具体经验，也看作主体认识自身与时代的媒介，说明自然书写如何超越风景描摹。④评价作品在现代散文中的哲思抒情特色，所有情节判断须以试卷附文为准，不凭空补写文本细节。", 0),
        ],
    )

    public_2024 = "https://view.inews.qq.com/a/20240106A00DAS00"
    add_group(
        "2024_610_public",
        2024,
        "610",
        "MEDIUM",
        "public_recall",
        public_2024,
        "公开真题汇总页的南师大 610 回忆版；题干可用，原卷扫描与分值未全部公开，seed 中未猜填分值。",
        [
            q("中国现当代文学", "SHORT_ANSWER", "1. 京派“田园牧歌”乡土小说。", "①界定京派与“田园牧歌”：以京派作家对乡土、自然和人性之美的审美想象为核心，不等于脱离现实的田园粉饰。②可举沈从文、废名等，分析湘西/乡土世界、民间伦理、自然生命和人性温情。③艺术上重抒情、诗化语言、象征和淡化戏剧冲突，常以城市现代性作为隐性对照。④指出其审美理想与社会批判之间的张力，以及地域化书写可能的理想化局限。"),
            q("中国现当代文学", "SHORT_ANSWER", "2. 80年代以来中国女性主义写作的三个代表性主题。", "①主体意识与自我成长：女性从被观看对象转为叙述者，追问自我身份和主体权利。②身体、情感与家庭/婚姻：书写欲望、劳动、照护和家庭权力，揭示私人生活的社会结构。③历史记忆、城乡经验与生存困境：把女性经验放入改革、城市化和历史转型中，形成个人史与社会史的交叉。④每个主题都应结合具体作家作品，避免把女性写作同质化，并指出其内部差异。"),
            q("文学理论", "SHORT_ANSWER", "3. 文学的话语蕴藉属性。", "①话语蕴藉指文学话语常具有表层语义之外的多层含义、情感暗示和审美意味，不能被简单的概念释义耗尽。②它通过含蓄、象征、反讽、意象、语境和形式结构实现言外之意。③这种属性要求读者调动经验进行阐释，但不等于任意解释，仍受文本语言和结构制约。④可用诗歌意象、小说反讽或戏剧潜台词说明意义的多义与不确定。"),
            q("文学理论", "SHORT_ANSWER", "4. 叙事作品人物“行动元”与“角色”的二重特性。", "①行动元是叙事功能层面的抽象位置，如主体/客体、发送者/接受者、帮助者/反对者；角色是具备姓名、身份、性格和心理的具体人物。②同一角色可在不同情节中承担多个行动元，一个行动元也可由多个角色共同承担。③二者分别说明人物的功能关系和个体形象，不能互相替代。④结合《红楼梦》、神话或侦探小说说明人物既被结构定位，又具有超出功能表的生命经验。"),
            q("外国文学", "SHORT_ANSWER", "5. 悲剧《俄狄浦斯王》的主题内涵。", "①作品围绕弑父娶母的神谕和追查展开，表现命运、知识与人的行动之间的矛盾。②俄狄浦斯追求真相、承担责任的行动既体现人的理性和尊严，也把他推向自我发现与毁灭。③悲剧还涉及城邦共同体、污染与责任、人的有限性等问题。④应避免把主题归结为“命运不可抗拒”，同时看到索福克勒斯对人的勇气、正义和自我认识的肯定。"),
            q("外国文学", "SHORT_ANSWER", "6. 19世纪欧洲自然主义文学的艺术特点及代表作品。", "①受实证主义、遗传学和环境决定论影响，强调以观察、实验和近似科学记录的方式表现社会。②关注贫困、疾病、欲望、劳动和社会底层，突出遗传与环境对人物的制约。③艺术上重细节、地方语言、群体场景和因果链，叙述趋于冷静。④代表作家以左拉为核心，可举《萌芽》《金钱》或《小酒店》，并说明自然主义继承现实主义又把决定论推向极端。"),
            q("中国古代文学", "SHORT_ANSWER", "7. 《诗经》在中国文学史上的地位和影响。", "①《诗经》是我国第一部诗歌总集，保存西周初年至春秋中叶的作品，奠定先秦诗歌传统。②风、雅、颂是音乐/作品类别，赋、比、兴形成重要表现方式；作品兼具社会生活记录与抒情传统。③其现实主义精神、风雅传统、比兴手法和四言节奏深刻影响后世诗歌、文论和经学。④评价时既要承认经典化与教化阐释，也要回到诗歌本身的情感和艺术多样性。"),
            q("中国古代文学", "SHORT_ANSWER", "8. 李白诗歌的艺术个性。", "①以强烈主体性和浪漫想象为核心，善用夸张、比喻、神话、梦境和时空跳跃。②语言清新明快而气势奔放，绝句和歌行尤其自然流转，形成豪迈飘逸的风格。③山水、月、酒、长风等意象承载自由理想与人生失意，既有蔑视权贵，也有对现实和友谊的深情。④把古乐府传统、民歌语言与个人天才结合，说明其艺术个性并非脱离时代的纯幻想。"),
            q("中国古代文学", "ESSAY", "9. 《红楼梦》的叙事艺术。", "①以贾府兴衰、宝黛爱情和大观园日常交织成多线结构，主线与支线相互映照。②叙事视角灵活，既有全知性安排，也大量通过人物感知、对话和诗词呈现，形成含蓄的多声部。③善用伏笔、照应、梦境、象征、反讽和“假语村言”等手段，形成表层日常与深层悲剧的张力。④人物在关系网络中展开，语言个性化，细节推动情节和性格。⑤结合作品评价其宏阔结构与开放性，同时注意版本和后四十回问题。"),
        ],
    )
    add_group(
        "2024_801_public",
        2024,
        "801",
        "MEDIUM",
        "public_recall",
        public_2024,
        "同一公开汇总页的南师大 801 专业写作回忆版；未把不可复核的细节当作原卷内容。",
        [
            q("中国现当代文学", "WRITING", "话题写作：根据黄子平、钱理群《论二十世纪中国文学》，谈“二十世纪中国文学”的内涵及其起点。", "①先界定“二十世纪中国文学”是时间概念，也是现代性、语言变革、主体经验和文学制度变化共同构成的历史概念。②起点可比较晚清文学转型、五四新文学和更早的现代性因素等不同说法，说明各自背后的文学史观。③结合黄子平、钱理群关于晚清—五四连续性、启蒙与革命的讨论，分析起点划分如何影响作家、作品和问题的选择。④结论不必强行唯一化，应说明断裂与连续并存，并提出自己的论证依据。", 0),
            q("中国现当代文学", "ANALYSIS", "根据蹇先艾《水葬》写不少于1500字文学评论。", "①以骆毛被村民施行水葬、母亲仍在等待儿子这一核心悲剧为文本抓手，分析旧习俗、群体麻木和小人物命运。②“水葬”既是情节事件，也是传统权力和集体观看机制的象征；村民的看热闹与母亲的无知形成强烈反差。③分析蹇先艾简朴、冷静的叙述，地域风俗的具体化，以及克制笔调中的哀痛。④可联系鲁迅对作品“贵州乡间习俗的冷酷”和“母性之爱”的评价，但评论主体必须回到文本细节。", 0),
        ],
    )

    public_2025 = "https://www.sjds.net/a/462969.html"
    add_group(
        "2025_610_public",
        2025,
        "610",
        "MEDIUM",
        "public_recall",
        public_2025,
        "公开真题汇总页的南师大 610 回忆版，题干和分值均按页面记录；原卷仍建议后续复核。",
        [
            q("中国古代文学", "SHORT_ANSWER", "1. 《左传》的艺术成就。", "①叙事常围绕政治、战争和外交冲突展开，善于剪裁材料、设置悬念和组织前因后果。②人物塑造通过行动、语言、细节和对比完成，既写谋略也写性格。③外交辞令和战事描写富有文学性，语言简洁有力、铺陈有度。④指出其历史叙事与道德评价相互交织，同时具有后世叙事文学的原型意义。", 15),
            q("中国古代文学", "SHORT_ANSWER", "2. 杜甫诗歌艺术成就。", "①以沉郁顿挫为总体风格，把个人身世、民生疾苦和国家命运结合起来。②善于在写实中融入抒情、议论和叙事，形成史诗性与个体性统一。③律诗格律严整而气脉深厚，善用对偶、炼字、时空转换和多重视角。④结合“三吏三别”、秋兴、登高等说明其悲悯的人道精神和诗史地位。", 20),
            q("中国古代文学", "SHORT_ANSWER", "3. 吴伟业“梅村体”诗歌艺术特色。", "①“梅村体”多指吴伟业以七言歌行叙事的诗体，善于铺陈人物和历史事件。②融合唐代歌行、元明戏曲与民间传说，重视情节、场面、声音和人物命运。③语言绮丽而有叙事推进，常以感慨、反讽和哀艳写明清易代的兴亡与个人身世。④结合《圆圆曲》等说明其叙事性、传奇性与历史感，并注意其文人审美的复杂性。", 15),
            q("中国现当代文学", "SHORT_ANSWER", "4. 鲁迅《野草》的思想内容与艺术特色。", "①作品以散文诗形式表现自我与黑暗、虚无、死亡、希望和行动之间的持续搏斗。②思想上既有对旧世界和国民性的批判，也有对孤独、牺牲和“无路可走”处境的自觉。③艺术上大量使用梦境、象征、悖论、反讽和变形，语言冷峻而富有诗性。④《秋夜》《过客》《墓碣文》等篇章显示不同声音，不能把《野草》读成单一的乐观或绝望。", 15),
            q("中国现当代文学", "SHORT_ANSWER", "5. 当代朦胧诗的艺术内容与艺术特色。", "①以北岛、舒婷、顾城等为代表，食指是重要先声，诗歌重建个体主体、尊严、爱情、自由和历史反思的表达。②艺术上使用象征、隐喻、通感、跳跃性意象和复调语调，形成含蓄而富有张力的现代抒情。③“朦胧”并非故意晦涩，而是把复杂历史经验转化为多义意象，保留读者阐释空间。④可说明其对传统政治抒情模式的突破，同时指出部分作品公共性与个人化之间的张力。", 15),
            q("文学理论", "SHORT_ANSWER", "6. 文学意境的特点。", "①意境是情景交融、虚实相生而形成的整体审美境界，不是景物简单相加。②它以有限形象唤起无限联想，包含时空、情感、气氛和意味。③常通过留白、象征、含蓄和主体情感的渗透实现“言有尽而意无穷”。④答题应结合诗词或散文说明景中有情、情中见景，以及读者在补充想象中的参与。", 20),
            q("文学理论", "SHORT_ANSWER", "7. 文学消费的二重性。", "①文学消费既是读者的精神审美活动，也是受出版、市场和媒介组织的文化商品消费。②它具有满足审美、认知、情感和交往需要的一面，也可能被商业逻辑、标准化和消费主义支配。③消费不是生产的被动终点，读者选择、阐释和传播会反过来影响创作与生产。④应辩证分析市场扩大文学传播的积极作用与商品化导致同质化、迎合和不平等的风险。", 20),
            q("外国文学", "SHORT_ANSWER", "8. 卢梭文学作品的思想内容及其对19世纪初浪漫主义文学的影响。", "①卢梭作品强调自然、真情、个体感受、主体自由和对社会不平等的反思，《新爱洛伊丝》《忏悔录》《漫步遐想录》体现不同文体面向。②写作把个人内心、自然景观和道德自省结合起来，强化第一人称和情感表达。③他为浪漫主义提供了崇尚自然、反理性束缚、重视自我和感伤主义的思想资源，影响歌德、拜伦等。④同时说明卢梭并非简单反社会，作品中个人欲望与道德、共同体责任始终存在张力。", 15),
            q("外国文学", "SHORT_ANSWER", "9. 狄更斯《双城记》的艺术特色。", "①以伦敦/巴黎双城结构和革命历史背景形成空间、阶级与价值对照。②叙事中大量使用悬念、伏笔、重复意象和戏剧化场面，把个人爱情、家族命运与历史暴力交织。③卡顿的自我牺牲与达尔内的替身关系，集中体现人物对照、救赎和人道主义。④小说既批判封建压迫，也不赞成无边界的复仇，把革命正义与暴力循环置于复杂历史中考察。", 15),
        ],
    )
    add_group(
        "2025_801_public",
        2025,
        "801",
        "MEDIUM",
        "public_recall",
        public_2025,
        "公开真题汇总页的 801 专业写作分方向回忆版；世界文学部分按页面列出的五个名词和四个简答入库。",
        [
            q("中国古代文学", "ANALYSIS", "中国古代文学专业写作：柳宗元《渔翁》鉴赏，800字以上。", "①从西岩、清湘、楚竹、烟销日出、山水和云等意象建立的幽静空间切入，分析渔翁形象的孤高、清醒与自守。②注意诗歌前后动静、远近、明暗和人景关系，说明渔翁独来独往的姿态如何与自然融为一体。③分析七言古诗的自由节奏、白描和留白，理解“欸乃一声”以及结尾云水相逐的开放性。④可联系柳宗元贬谪处境，但不能用作者生平替代文本细读，应说明景物如何被诗化。", 60),
            q("中国古代文学", "WRITING", "中国古代文学专业写作：根据李东阳、李梦阳关于“真诗”的材料写评论，1200字以上。", "①先解释“真诗”强调真情、真声、真切感受，反对徒事模拟、堆砌辞藻和脱离生活的伪古典。②放回明代复古思潮中，分析李东阳、李梦阳对诗歌法度、古典学习与真实情感的不同侧重。③结合材料说明“真”不是不要艺术规范，而是让法度服务于真实生命经验。④评价其反对浮泛文风的意义，同时指出复古仍可能形成新的法式束缚，结论以材料原文为准。", 90),
            q("文学理论", "ANALYSIS", "文艺学专业写作：赏析泰德·休斯《乌鸦的最后据点》，500字以上。", "①结合“燃烧—太阳—乌鸦的眼睛/焦黑堡垒”等核心意象，分析乌鸦作为生命韧性、感知和原初力量的象征。②诗歌把神话式叙事、末世景象和自然元素结合，形成既残酷又顽强的生态想象。③注意断行、重复、强烈动词和简洁粗粝的语言，说明形式如何制造压力与停顿。④可联系《Crow》组诗对宗教秩序、人类中心主义和文明自信的反思，但要以该诗文本为中心。", 30),
            q("文学理论", "WRITING", "文艺学专业论文：论“文”与“事”的关系，1500字以上。", "①区分“事”的事件、经验、事实材料与“文”的语言、结构、修辞和形式组织。②文学不是把事原样搬运，而是通过取舍、叙述视角、秩序安排和语体把事件转化为可感的意义。③“文”不能脱离“事”成为空转技巧，“事”也只有经过形式才能进入审美和认识。④可结合《左传》叙事、史传文学、小说改写或散文说明二者的互动，结尾讨论虚构、真实和伦理责任。", 120),
            q("外国文学", "TERM_EXPLANATION", "《罗兰之歌》", "①法国中世纪英雄史诗，属于武功歌传统，写查理大帝远征和罗兰在隆塞沃战斗中的牺牲。②以忠诚、荣誉、骑士责任和基督教信仰为核心价值，具有民族史诗和宗教色彩。③叙事采用夸张、重复和类型化人物，罗兰、奥利维耶等形象体现不同骑士伦理。"),
            q("外国文学", "TERM_EXPLANATION", "大学才子派", "①16世纪英国伊丽莎白时代受大学教育、以戏剧创作为主的一批作家，常举马洛、格林、皮尔、洛奇和纳什。②他们吸收古典文学与人文主义思想，推动英国公共剧场发展。③艺术上重视无韵诗、宏大激情、修辞和复杂情节，为莎士比亚戏剧的成熟准备了条件。"),
            q("外国文学", "TERM_EXPLANATION", "狂飙突进运动", "①18世纪70年代德国文学运动，反对封建专制、古典主义清规和理性主义的僵化。②强调感情、自然、天才、个性和自由，崇尚强烈生命力。③青年歌德、席勒及其作品具有代表性，运动为德国浪漫主义和现实主义文学提供了重要资源。"),
            q("外国文学", "TERM_EXPLANATION", "波尔金诺之秋", "①指普希金1830年秋在波尔金诺庄园度过的一段创作高峰期。②他在此完成《叶甫盖尼·奥涅金》的重要部分，并写下诗歌、小说、戏剧等多种作品。③这一时期显示其文体成熟、题材多样和由浪漫主义向更深现实主义观察发展的趋势。"),
            q("外国文学", "TERM_EXPLANATION", "约克纳帕塔法世系", "①福克纳以美国南方密西西比为原型虚构的县域世界及其作品谱系。②《喧哗与骚动》《我弥留之际》《八月之光》等作品共享地理、家族、历史和人物。③这一“地方神话”书写种族、阶级、南方传统与现代变迁，使地方经验获得普遍文学意义。"),
            q("外国文学", "SHORT_ANSWER", "1. 18世纪书信体小说列举两部并谈谈理解。", "①可举理查逊《帕米拉》/《克拉丽莎》、卢梭《新爱洛伊丝》或歌德《少年维特的烦恼》。②书信形式让人物以第一人称、片段化和即时性表达内心，形成多声部和心理深描。③它适合表现私人情感、社会礼法与主体成长，也保留信息不完整、视角受限和情感偏向。④结合两部作品比较英国道德小说与感伤主义小说的差异。"),
            q("外国文学", "SHORT_ANSWER", "2. 雨果《克伦威尔序言》的浪漫主义原则，并结合代表作品说明。", "①反对古典主义的僵化等级和三一律，主张艺术自由与题材、体裁、语言的解放。②提出美与丑、崇高与滑稽、悲剧与喜剧可以共存，要求表现历史和社会生活的复杂总体。③强调个性、想象、色彩和人民生活，文学应进入现实历史。④可结合《欧那尼》《巴黎圣母院》《悲惨世界》说明“美丑对照”、历史场景和人道主义。"),
            q("外国文学", "SHORT_ANSWER", "3. 俄国文学中的“多余人”形象系列。", "①“多余人”多出身贵族、受过教育、敏感聪明而缺少现实行动能力，在社会转型中感到无所归属。②从普希金奥涅金、莱蒙托夫毕巧林，到屠格涅夫鲁亭、冈察洛夫奥勃洛莫夫，形象由冷漠旁观、主动试探到彻底停滞，呈现演变。③他们既有个人性格弱点，也受农奴制、贵族社会和历史条件制约。④答题要避免只列人物，应联系时代结构和“思想—行动”矛盾。"),
            q("外国文学", "SHORT_ANSWER", "4. “艺术不是再现而是表现”，请结合作品谈理解。", "①“表现”强调艺术通过形式、色彩、节奏、视角和变形把主体感受外化，不是照相式复制对象。②可结合表现主义绘画/戏剧、梵高《星月夜》或现代主义文学，说明夸张和变形如何揭示内在真实。③表现不等于任意主观化，仍需由作品形式和可感形象建立交流。④与现实主义再现观比较，指出二者不是绝对对立，优秀作品常在现实经验和主体形式之间转换。"),
            q("中国现当代文学", "WRITING", "中国现当代文学专业写作：结合《故事新编》分析“新编/改写”现象。", "①以鲁迅《故事新编》为核心参照，说明改写不是复述旧故事，而是让古代神话/传说与现代经验、语言和历史意识发生碰撞。②分析古今杂糅、反讽、错位、戏拟和多重语体，说明经典在新语境中被重新解释。③讨论改写对权威叙事、英雄神话和现代现实的反思，同时注意文本对原典的依赖。④联系当代历史/神话重写，比较继承、变形和消费经典的不同路径。", 0),
            q("中国现当代文学", "ANALYSIS", "中国现当代文学专业写作：鉴赏王安忆《羊》，写一篇评论文章。", "①围绕文工团中一只羊的命运和日常观察展开文本细读，把非人动物视角、集体生活和普通人的微小情感联系起来。②分析王安忆克制、细密的叙述如何把宏大历史降落到物质细节、身体感受和人际关系中。③关注羊作为生命、被观看对象和时代隐喻的多重意义，但不能把象征意义凌驾于故事细节。④评价作品对特殊年代的书写方式：不靠宏大判断，而以日常伦理、情感缝隙和叙事留白呈现复杂人性。", 0),
        ],
    )

    public_2026 = "https://www.wyztk.com/thread-4362-1-1.html"
    add_group(
        "2026_801_public",
        2026,
        "801",
        "MEDIUM",
        "public_recall",
        public_2026,
        "公开真题回忆页的 2026 南师大 801 文学基础；页面为回忆资料，原卷和分值待复核。",
        [
            q("中国古代文学", "TERM_EXPLANATION", "1. 《昭明文选》", "①南朝梁昭明太子萧统主持编选的诗文总集，按文体分类，选录先秦至梁代重要作品。②重视文学作品的艺术性和文体意识，对诗文选本、作家作品传播和后世文学批评影响深远。③“文选学”及其评点、注释传统成为古典文学研究的重要资源。"),
            q("中国古代文学", "TERM_EXPLANATION", "2. 南戏", "①南宋至元明在东南沿海民间发展起来的戏曲形态，源于民间歌舞、说唱和地方曲调。②篇幅、角色和曲牌组织较为灵活，重视生旦情节与市民审美，代表作品有《永乐大典戏文三种》及《荆钗记》等。③南戏对传奇和明清戏曲发展具有重要奠基作用。"),
            q("文学理论", "TERM_EXPLANATION", "3. 艺术交往论", "①把艺术活动理解为创作者、作品、接受者及社会文化环境之间的交往过程，而非单向传递。②艺术交往既有信息和情感交流，也有价值、经验、身份和审美方式的协商。③它要求把生产、传播、接受放在具体媒介和社会关系中考察，说明艺术的公共性与主体间性。"),
            q("文学理论", "TERM_EXPLANATION", "4. 文学批评", "①以文学作品和文学现象为对象的分析、阐释、评价和判断活动。②它既要尊重文本的语言、结构和审美特征，也要联系作家、读者、历史和文化语境。③文学批评的标准通常包括思想内容、审美价值、艺术创新和历史意义，关键在于以论据形成有解释力的判断。"),
            q("中国古代文学", "SHORT_ANSWER", "1. 《离骚》的艺术成就。", "①以自叙性抒情为骨架，把个人身世、政治理想、忠贞人格和神游想象结合起来。②继承并发展楚辞传统，篇幅宏大，善用香草美人比喻、神话、象征和反复。③语言瑰丽奇崛，句式灵活，形成强烈节奏和浪漫想象。④作品把个体人格理想提升为民族文化中坚持理想、独立不屈的精神资源。"),
            q("中国古代文学", "SHORT_ANSWER", "2. 韩愈古文的艺术成就。", "①散文内容关注现实、政治和道德，提倡文以明道又不废文章本身。②善于叙事、议论和抒情结合，结构开合变化，气势雄健。③语言突破骈文束缚，兼用口语、排比、对偶和奇险句式，形成多样文风。④《原道》《师说》《祭十二郎文》等分别体现论说、议论和抒情文的成就，并指出其对古文运动和后世散文的影响。"),
            q("中国现当代文学", "SHORT_ANSWER", "3. 老舍《茶馆》的艺术成就。", "①以裕泰茶馆为中心，通过三个历史时期的日常场景展示社会变迁和普通人的命运。②采用“人像展览式”结构，人物进出、对话和细节构成时代横截面，弱化单一中心情节。③语言京味浓郁，人物身份、性格和阶层通过口语、称谓与机锋显现。④悲喜剧结合、象征性空间与历史纵深相统一，表现旧制度下人的困顿，也保留民间生命力。"),
            q("外国文学", "SHORT_ANSWER", "4. 《巴黎圣母院》如何体现雨果的“美丑对照原则”？", "①人物上以卡西莫多的外貌丑陋与内心善良、爱斯梅拉达的美与社会偏见、克洛德的外表/身份与内心欲望形成对照。②情节和场景把圣母院、狂欢节、刑场等崇高与丑怪、神圣与暴力并置。③形式上融合崇高、滑稽、恐怖和抒情，突破古典主义单一美学等级。④对照不是外貌二分，而是借外在形象反讽制度和人心，凸显真正的人道主义美。"),
            q("文学理论", "SHORT_ANSWER", "5. 小说的基本特征。", "①小说以叙事为基本方式，通过人物、事件、环境和叙述组织较为完整或开放的生活世界。②人物具有行动、心理、关系和发展，情节体现因果、冲突或时间变化，环境承载社会历史与氛围。③小说具有虚构性、社会性、开放性和多样叙事视角，篇幅和形式没有单一固定规范。④应结合具体小说说明叙述如何把经验转化为审美结构，而不是只背“长篇散文”定义。"),
            q("中国古代文学", "ESSAY", "1. 《金瓶梅》的小说史成就。", "①把《水浒传》中西门庆、潘金莲等人物展开为独立世情叙事，把视线从英雄传奇转向日常家庭、商业、欲望和社会关系。②采用网状结构、生活流细节和多层人物群像，展示明代市民社会的复杂面貌。③人物既有欲望、算计，也有伦理关系和心理变化，形成中国古代小说由类型化向世情化发展的重要一步。④语言俗白而有讽刺性，善于用日常细节揭示制度和人情。⑤评价其现实主义成就，同时说明性别书写、道德观看和版本问题的复杂性。"),
            q("中国现当代文学", "ESSAY", "2. 鲁迅、茅盾、沈从文乡土世界书写的侧重点差异。", "①鲁迅以故乡经验为切入口，重点揭示乡土社会的精神麻木、礼教压迫和国民性问题，叙述冷峻而富有启蒙批判。②茅盾把乡土置于社会经济、阶级关系和现代化冲突中，关注结构性变化与历史运动。③沈从文以湘西世界、人性和自然生命为中心，书写民间伦理、生命美和现代文明的冲突，语言更具诗性。④三者并非绝对割裂，都把乡土作为现代中国的观察窗口；比较时要从主题、人物、叙事语调和文学史立场展开。"),
            q("文学理论", "ESSAY", "3. 审美意象的基本特征。", "①审美意象是经过艺术加工、承载情感和意义的感性形象，不是客观物的机械复制。②它具有主观情感与客观物象融合、感性具体与意义生成统一的特点。③意象常通过象征、联想、想象、变形和组合形成多义性，并在作品结构中相互照应。④它既有个体经验，又能在文化传统和读者接受中获得共享意味；答题要用诗歌、小说或绘画举例说明形式如何生成意义。"),
            q("外国文学", "ESSAY", "4. 17世纪欧洲古典主义文学的特点。", "①古典主义受君主专制国家秩序、理性主义和古希腊罗马传统影响，强调规范、理性、秩序和普遍法则。②戏剧重视三一律、体裁等级、典雅语言和冲突的道德化，法国悲剧以高乃依、拉辛，喜剧以莫里哀为代表。③作品常写责任、荣誉、欲望与理性的冲突，人物具有类型化与规范性。④不能把古典主义等同于没有现实批判；应说明其形式纪律与社会秩序之间的联系及其局限。"),
        ],
    )
    add_group(
        "2026_610_public",
        2026,
        "610",
        "MEDIUM",
        "public_recall",
        public_2026,
        "公开真题回忆页的 2026 南师大 610 专业写作现当代方向部分；题干可用，分值待原卷复核。",
        [
            q("中国现当代文学", "TERM_EXPLANATION", "1. 东北流亡作家群", "①指九一八事变后流亡关内、以东北沦陷和民族苦难为重要题材的一批作家。②作品把流亡经验、故土记忆、抗战意识和民族身份结合起来，形成特殊的地域想象与历史见证。③可联系萧红、端木蕻良、骆宾基等作家及其作品，注意不同作家的创作道路和艺术风格不能被简单合并。"),
            q("中国现当代文学", "TERM_EXPLANATION", "2. 盘峰诗会", "①指1999年4月在北京平谷盘峰宾馆举行的诗会，会议之后形成持续的“盘峰论争”。②论争关联“知识分子写作”与“民间写作”等诗学分歧，涉及诗歌与读者、历史处理、语言、主体、现实性和公共性。③答题应交代会议时间、论争延展、主要观念和新世纪诗歌史影响，避免把诗会等同于一个风格完全统一的诗派。"),
            q("中国现当代文学", "WRITING", "3. 2025年是中国人民抗日战争暨世界反法西斯战争胜利80周年：梳理文学史相关作家作品，论述文学的抗战贡献。", "①按时间与区域组织材料：抗战前后的民族危机书写、国统区/解放区/沦陷区文学及诗歌、小说、戏剧、报告文学等文体。②可举茅盾、巴金、老舍、郭沫若、曹禺、田汉、艾青、臧克家、赵树理等，必须用具体作品而非只列姓名。③从民族动员、公共记忆、人物塑造、语言大众化、揭露战争苦难和保存个体经验等方面论述贡献。④同时保持历史辨析：抗战文学有宣传与模式化问题，但不能因此抹去其社会动员、民族认同和审美创造价值。"),
            q("中国现当代文学", "ANALYSIS", "4. 诗歌鉴赏：西川《在哈尔盖仰望星空》。", "①从哈尔盖的地域空间、夜空和仰望动作切入，分析诗中自然景观如何转化为主体的精神经验。②关注“星空”与人的尺度、孤独、沉默、时间和宇宙感之间的张力，避免只作景物描写。③分析自由诗的行分、停顿、口语与哲思语调，说明日常语言如何产生辽阔感。④结尾评价西川诗歌把现实地理、知识意识和抒情想象结合的特点，具体意象以试卷附诗为准。"),
        ],
    )
    return groups


PENDING = [
    {
        "year": 2024,
        "paper_code": "805",
        "subject": "外国文学",
        "status": "NOT_IMPORTED",
        "reason": "公开页面可确认题目页面存在，但正文为会员/登录限制，当前没有可交叉复现的完整题干；不猜写。",
        "source": "https://www.kaoyany.top/post/235363.html",
    },
    {
        "year": 2025,
        "paper_code": "805",
        "subject": "外国文学",
        "status": "NOT_IMPORTED",
        "reason": "现有公开资料将 805 外国文学史与 801 世界文学专业写作方向材料混列，缺少可核对的完整原卷；先保留待核。",
        "source": "公开资料方向代码不一致，待原卷或用户补充扫描件",
    },
]


def load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as handle:
        return json.load(handle)


def dump_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(value, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def canonical(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def normalize_text(value: str) -> str:
    return re.sub(r"[\s《》〈〉“”‘’\"'、，,。！？：:；;（）()【】\[\]…—–\-]", "", value).lower()


def max_exam_number(exams: list[dict[str, Any]]) -> int:
    numbers = []
    for exam in exams:
        match = re.fullmatch(r"eq_(\d+)", str(exam.get("id", "")))
        if match:
            numbers.append(int(match.group(1)))
    return max(numbers, default=0)


def flatten_groups(groups: list[dict[str, Any]], start_number: int) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    number = start_number
    for group in groups:
        for question in group["questions"]:
            number += 1
            record = {
                "id": f"eq_{number:04d}",
                "year": group["year"],
                "subject": question["subject"],
                "question_type": question["question_type"],
                "content": question["content"],
                "score": question["score"],
                "exam_paper_code": group["exam_paper_code"],
                "answer_framework": question["answer_framework"],
                "_source_key": group["key"],
                "_source_confidence": group["confidence"],
                "_source_kind": group["source_kind"],
                "_source_locator": group["source_locator"],
            }
            records.append(record)
    return records


def verify_groups(
    groups: list[dict[str, Any]],
    seed: dict[str, Any],
    errors: list[str],
    verify_applied_mode: bool = False,
) -> list[dict[str, Any]]:
    base_exams = seed.get("exam_questions", [])
    if not verify_applied_mode:
        if seed.get("metadata", {}).get("version") != "2.20.0":
            errors.append(f"基线 seed 版本不是 2.20.0: {seed.get('metadata', {}).get('version')!r}")
        if len(base_exams) != 485:
            errors.append(f"基线真题数量不是 485: {len(base_exams)}")

    # 写入后 seed 已经包含新题，不能再用当前最大编号推导新题起点。
    start_number = 481 if verify_applied_mode else max_exam_number(base_exams)
    records = flatten_groups(groups, start_number)
    existing_ids = {item.get("id") for item in base_exams} if not verify_applied_mode else set()
    existing_norm = {normalize_text(str(item.get("content", ""))) for item in base_exams} if not verify_applied_mode else set()
    seen_ids: set[str] = set()
    seen_norm: set[str] = set()
    source_keys = {group["key"] for group in groups}
    assigned_groups = {record["_source_key"] for record in records}
    if source_keys != assigned_groups:
        errors.append(f"存在未使用或丢失的来源组: declared={sorted(source_keys)} assigned={sorted(assigned_groups)}")

    for index, record in enumerate(records):
        label = f"candidate[{index}]"
        if record["id"] in existing_ids or record["id"] in seen_ids:
            errors.append(f"{label}: ID 重复: {record['id']}")
        seen_ids.add(record["id"])
        if record["subject"] not in VALID_SUBJECTS:
            errors.append(f"{label}: subject 无效: {record['subject']}")
        if record["question_type"] not in VALID_TYPES:
            errors.append(f"{label}: question_type 无效: {record['question_type']}")
        if not isinstance(record["year"], int) or record["year"] not in {2023, 2024, 2025, 2026}:
            errors.append(f"{label}: 年份无效: {record['year']}")
        if not isinstance(record["content"], str) or len(record["content"].strip()) < 2:
            errors.append(f"{label}: 题干为空: {record['id']}")
        if not isinstance(record["answer_framework"], str) or len(record["answer_framework"].strip()) < 60:
            errors.append(f"{label}: 答案框架过短: {record['id']}")
        if any(token in record["answer_framework"] for token in ("TODO", "待补", "AI生成", "无法回答")):
            errors.append(f"{label}: 答案框架含未完成占位词: {record['id']}")
        normalized = normalize_text(record["content"])
        if normalized in existing_norm:
            errors.append(f"{label}: 与旧题题干重复: {record['content']}")
        if normalized in seen_norm:
            errors.append(f"{label}: 本批题干重复: {record['content']}")
        seen_norm.add(normalized)
        if record["_source_key"] not in source_keys:
            errors.append(f"{label}: 来源组不存在: {record['_source_key']}")
        if record["_source_confidence"] not in {"HIGH", "MEDIUM"}:
            errors.append(f"{label}: 来源置信度无效: {record['_source_confidence']}")
        if record["_source_kind"] == "public_recall" and not str(record["_source_locator"]).startswith("https://"):
            errors.append(f"{label}: 公共回忆题没有 URL: {record['id']}")

    expected_first = start_number + 1
    expected_last = start_number + len(records)
    if records and records[0]["id"] != f"eq_{expected_first:04d}":
        errors.append(f"新题起始 ID 错误: {records[0]['id']}")
    if records and records[-1]["id"] != f"eq_{expected_last:04d}":
        errors.append(f"新题结束 ID 错误: {records[-1]['id']}")
    return records


def build_candidate(seed: dict[str, Any], records: list[dict[str, Any]]) -> dict[str, Any]:
    candidate = copy.deepcopy(seed)
    old_ids = {item["id"] for item in candidate["exam_questions"]}
    for record in records:
        candidate_record = {key: value for key, value in record.items() if not key.startswith("_")}
        candidate["exam_questions"].append(candidate_record)
    metadata = candidate.setdefault("metadata", {})
    metadata["version"] = "2.22.0"
    metadata["generated_at"] = "2026-08-07T00:00:00.000000"
    note = "v2.22.0 补充 2023—2026 年可复核真题及答案框架：2023 原始试卷高可信入库，2024—2026 公开回忆题分级记录；未核实的 805 外国文学题目不猜写。"
    description = metadata.get("description", "")
    if note not in description:
        metadata["description"] = f"{description} | {note}"
    fixes = metadata.setdefault("fixes", [])
    if note not in fixes:
        fixes.append(note)
    if {item["id"] for item in candidate["exam_questions"] if item["id"] in old_ids} != old_ids:
        raise AssertionError("构造候选结果时旧真题 ID 集合发生变化")
    return candidate


def strip_internal(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return [{key: value for key, value in record.items() if not key.startswith("_")} for record in records]


def build_audit(groups: list[dict[str, Any]], records: list[dict[str, Any]], errors: list[str]) -> dict[str, Any]:
    return {
        "batch_id": "exam-2023-2026-v2.22",
        "base_version": "2.20.0",
        "target_version": "2.22.0",
        "generated_at": "2026-08-07T00:00:00.000000",
        "question_count": len(records),
        "question_count_by_year": {
            str(year): sum(1 for record in records if record["year"] == year)
            for year in (2023, 2024, 2025, 2026)
        },
        "question_count_by_source_confidence": {
            confidence: sum(1 for record in records if record["_source_confidence"] == confidence)
            for confidence in ("HIGH", "MEDIUM")
        },
        "groups": [
            {
                key: value
                for key, value in group.items()
                if key != "questions"
            }
            | {"question_ids": [record["id"] for record in records if record["_source_key"] == group["key"]]}
            for group in groups
        ],
        "pending_not_imported": PENDING,
        "questions": [
            {
                **{key: value for key, value in record.items() if not key.startswith("_")},
                "source_key": record["_source_key"],
                "source_confidence": record["_source_confidence"],
                "source_kind": record["_source_kind"],
                "source_locator": record["_source_locator"],
            }
            for record in records
        ],
        "errors": errors,
    }


def verify_applied(seed: dict[str, Any], records: list[dict[str, Any]], errors: list[str]) -> None:
    by_id = {item.get("id"): item for item in seed.get("exam_questions", [])}
    fixes = seed.get("metadata", {}).get("fixes", [])
    v222_note = any(isinstance(item, str) and item.startswith("v2.22.0 补充 2023—2026 年可复核真题") for item in fixes)
    if not v222_note:
        errors.append("写入后缺少 v2.22.0 真题补充记录，无法确认本批仍在种子中")
    if len(seed.get("exam_questions", [])) != 485 + len(records):
        errors.append(f"写入后真题数量错误: {len(seed.get('exam_questions', []))}")
    for record in records:
        current = by_id.get(record["id"])
        label = f"applied[{record['id']}]"
        if current is None:
            errors.append(f"{label}: 缺少新题")
            continue
        expected = {key: value for key, value in record.items() if not key.startswith("_")}
        if canonical(current) != canonical(expected):
            errors.append(f"{label}: 写入后字段不一致")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=Path, default=SEED_PATH)
    parser.add_argument("--candidates", type=Path, default=CANDIDATE_PATH)
    parser.add_argument("--report", type=Path, default=REPORT_PATH)
    parser.add_argument("--snapshot", type=Path)
    parser.add_argument("--dry-run", action="store_true", help="显式执行写入前验证，不修改 seed")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verify-applied", action="store_true")
    args = parser.parse_args()

    seed = load_json(args.seed)
    groups = build_groups()
    errors: list[str] = []
    records = verify_groups(groups, seed, errors, verify_applied_mode=args.verify_applied)
    audit = build_audit(groups, records, errors)
    dump_json(args.candidates, audit)

    if args.verify_applied:
        verify_applied(seed, records, errors)
        audit["mode"] = "verify-applied"
        audit["errors"] = errors
        dump_json(args.report, audit)
        print(json.dumps({"mode": "verify-applied", "questions": len(records), "errors": len(errors)}, ensure_ascii=False))
        if errors:
            for error in errors:
                print(f"- {error}", file=sys.stderr)
            return 2
        print("写入后验证通过")
        return 0

    candidate = build_candidate(seed, records)
    old_exam_by_id = {item["id"]: item for item in seed.get("exam_questions", [])}
    candidate_old_by_id = {item["id"]: item for item in candidate.get("exam_questions", []) if item["id"] in old_exam_by_id}
    if set(old_exam_by_id) != set(candidate_old_by_id):
        errors.append("旧真题 ID 集合发生变化")
    for exam_id, old_exam in old_exam_by_id.items():
        if canonical(old_exam) != canonical(candidate_old_by_id[exam_id]):
            errors.append(f"旧真题出现未授权字段变化: {exam_id}")

    audit["mode"] = "apply" if args.apply else "dry-run"
    audit["errors"] = errors
    audit["old_exam_count"] = len(seed.get("exam_questions", []))
    audit["new_exam_count"] = len(candidate.get("exam_questions", []))
    audit["old_ids_preserved"] = not any("旧真题" in error for error in errors)
    dump_json(args.report, audit)
    print(json.dumps({"mode": audit["mode"], "old": audit["old_exam_count"], "new": audit["new_exam_count"], "added": len(records), "errors": len(errors)}, ensure_ascii=False))
    if errors:
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 2
    if not args.apply:
        print("验证通过；未写入 seed_data.json（dry-run）")
        return 0
    if args.snapshot:
        args.snapshot.parent.mkdir(parents=True, exist_ok=True)
        args.snapshot.write_text(args.seed.read_text(encoding="utf-8"), encoding="utf-8")
    dump_json(args.seed, candidate)
    print(f"已写入 {args.seed}: {len(seed.get('exam_questions', []))} -> {len(candidate.get('exam_questions', []))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

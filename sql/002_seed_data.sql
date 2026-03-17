-- Story Weaver 初始数据脚本
-- 版本: 1.0.0
-- 创建日期: 2026-03-16

USE story_weaver;

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO user (username, password, email, nickname, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@storyweaver.com', '系统管理员', 1),
('author', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'author@storyweaver.com', '创作作者', 1),
('testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'test@storyweaver.com', '测试用户', 1);

-- 插入示例项目
INSERT INTO project (name, description, user_id, status, genre, tags) VALUES
('星辰之剑', '一部关于魔法与剑的奇幻史诗，讲述少年剑士的成长之路', 1, 1, '奇幻', '魔法,剑士,成长,史诗'),
('未来都市', '赛博朋克风格的科幻小说，探讨科技与人性的关系', 2, 1, '科幻', '赛博朋克,未来,科技,人性'),
('江湖风云录', '传统武侠小说，讲述江湖恩怨与侠义精神', 3, 0, '武侠', '江湖,侠义,恩怨,传统');

-- 插入示例章节
INSERT INTO chapter (project_id, title, content, order_num, status, word_count) VALUES
(1, '第一章：命运的召唤', '在遥远的星辰大陆，有一个名叫艾伦的少年。他生活在一个平静的小村庄，每天帮助父亲打理铁匠铺。然而，一场突如其来的灾难改变了他的命运...', 1, 2, 1560),
(1, '第二章：神秘的老者', '受伤的艾伦被一位神秘老者所救。老者告诉他，他体内流淌着古老的星辰血脉，注定要成为拯救世界的英雄...', 2, 2, 1420),
(1, '第三章：初试魔法', '在老者的指导下，艾伦开始学习基础的魔法知识。他发现自己对火元素有着特殊的亲和力...', 3, 1, 1380),
(2, '第一章：霓虹之夜', '夜之城永远灯火通明。李阳穿梭在拥挤的人群中，他的义眼扫描着周围的一切。作为一名网络侦探，他接到了一个神秘委托...', 1, 2, 1480),
(2, '第二章：数据迷宫', '李阳潜入公司的数据库，却发现这里比他想象的更加复杂。层层加密的防火墙，还有巡逻的AI守卫...', 2, 1, 1520);

-- 插入示例人物
INSERT INTO character (project_id, name, description, profile, status) VALUES
(1, '艾伦·星辉', '主角，拥有星辰血脉的少年', '年龄：16岁\n性别：男\n性格：善良、勇敢、有责任感\n外貌：棕色短发，蓝色眼睛，中等身材\n特长：剑术、火系魔法\n背景：铁匠之子，父母在灾难中失踪', 1),
(1, '梅林·智慧', '神秘的老魔法师', '年龄：未知（外表约60岁）\n性别：男\n性格：睿智、神秘、严厉但慈祥\n外貌：白色长须，深蓝色长袍，手持橡木法杖\n特长：全系魔法、预言术\n背景：星辰学院的创始人之一', 1),
(1, '莉莉丝·暗影', '反派，黑暗女巫', '年龄：28岁\n性别：女\n性格：冷酷、野心勃勃、善于操纵\n外貌：黑色长发，紫色眼眸，穿着暗紫色长裙\n特长：暗影魔法、精神控制\n背景：被驱逐的前星辰学院学生', 1),
(2, '李阳', '主角，网络侦探', '年龄：32岁\n性别：男\n性格：冷静、机智、正义感强\n外貌：黑色短发，左眼为红色义眼，穿着黑色风衣\n特长：黑客技术、格斗、侦查\n装备：神经接口、数据匕首、隐身斗篷\n背景：前公司安全顾问，因发现公司秘密被追杀', 1),
(2, '零', '神秘AI助手', '年龄：未知\n性别：无（声音为中性）\n性格：逻辑性强、忠诚、有时会表现出人性化\n外貌：无实体，以全息投影形式出现\n特长：数据分析、系统入侵、信息检索\n背景：由匿名黑客创造的超级AI', 1);

-- 插入示例世界设定
INSERT INTO world_setting (project_id, title, content, category, order_num) VALUES
(1, '星辰大陆地理', '星辰大陆分为五个主要区域：\n1. 北境冰原：终年积雪，居住着冰霜巨人\n2. 中央平原：人类王国所在地，土地肥沃\n3. 东方森林：精灵族的家园，充满魔法\n4. 南方沙漠：游牧民族和古老遗迹\n5. 西方群岛：海盗和海上贸易中心', '地理', 1),
(1, '魔法体系', '星辰大陆的魔法分为六大元素：\n1. 火元素：攻击性强，适合战斗\n2. 水元素：治疗和防御\n3. 风元素：速度和辅助\n4. 土元素：防御和建造\n5. 光元素：神圣和治疗\n6. 暗元素：诅咒和操控\n魔法师需要与元素精灵签订契约才能使用高级魔法', '魔法', 2),
(1, '历史背景', '千年前，星辰之神创造了这个世界。\n五百年前，黑暗势力入侵，引发第一次圣战。\n三百年前，星辰学院建立，培养魔法师保卫世界。\n五十年前，黑暗女巫莉莉丝崛起，威胁世界和平。', '历史', 3),
(2, '夜之城介绍', '夜之城是22世纪最大的都市之一，人口超过3000万。\n城市分为三个区域：\n1. 上城区：公司总部和富人区，科技先进\n2. 中城区：商业和居住区，人口密集\n3. 下城区：贫民窟和黑市，治安混乱\n城市由三大公司控制：天穹科技、神经网络、生化基因', '城市', 1),
(2, '科技设定', '主要科技：\n1. 神经接口：直接连接大脑和网络\n2. 义体改造：替换身体器官增强能力\n3. 全息投影：三维立体影像技术\n4. 悬浮车辆：磁悬浮交通工具\n5. AI助手：个性化人工智能\n6. 虚拟现实：完全沉浸式体验', '科技', 2);

-- 插入示例AI写作记录
INSERT INTO ai_writing_record (project_id, chapter_id, prompt, response, model, token_usage, status) VALUES
(1, 1, '继续写一段奇幻小说的开头，主角是一个铁匠之子，突然发现自己有魔法天赋', '艾伦感到手掌发热，低头一看，竟然有微弱的火焰在掌心跳动。他惊讶地睁大眼睛，火焰不仅没有烧伤他，反而随着他的意念变化形状。这时，门外传来急促的敲门声...', 'gpt-4', 120, 1),
(1, 2, '写一段魔法训练的场景，主角第一次尝试控制火元素', '在老者的指导下，艾伦集中精神，试图让火焰形成一个小球。汗水从他的额头滑落，火焰忽大忽小，极不稳定。突然，火焰失控地窜向书架，老者一挥法杖，一道水幕及时出现...', 'gpt-4', 95, 1),
(2, 4, '写一段赛博朋克风格的追逐戏，主角在被公司特工追捕', '李阳在拥挤的街道上狂奔，身后的公司特工紧追不舍。他跳上一辆悬浮出租车，对司机喊道："去下城区，快！" 车辆刚启动，一枚电磁脉冲弹就在身后爆炸...', 'claude-3', 110, 1);

-- 插入系统配置
INSERT INTO system_config (config_key, config_value, description) VALUES
('site_name', 'Story Weaver', '网站名称'),
('site_description', 'AI小说创作平台', '网站描述'),
('default_ai_model', 'gpt-4', '默认AI模型'),
('max_chapter_length', '5000', '章节最大字数'),
('rag_enabled', 'true', 'RAG功能是否启用'),
('auto_save_interval', '300', '自动保存间隔（秒）'),
('default_theme', 'light', '默认主题'),
('registration_enabled', 'true', '是否允许注册');

-- 插入操作日志示例
INSERT INTO operation_log (user_id, module, action, target_id, description, ip_address) VALUES
(1, 'project', 'create', 1, '创建项目"星辰之剑"', '192.168.1.100'),
(1, 'chapter', 'create', 1, '创建章节"第一章：命运的召唤"', '192.168.1.100'),
(2, 'character', 'create', 4, '创建人物"李阳"', '192.168.1.101'),
(3, 'ai_writing', 'generate', 1, '使用AI生成章节内容', '192.168.1.102');

echo "初始数据插入完成";

-- 显示统计信息
SELECT 
    (SELECT COUNT(*) FROM user) as user_count,
    (SELECT COUNT(*) FROM project) as project_count,
    (SELECT COUNT(*) FROM chapter) as chapter_count,
    (SELECT COUNT(*) FROM character) as character_count,
    (SELECT COUNT(*) FROM ai_writing_record) as ai_record_count;

echo "数据库初始化完成！";
#!/bin/bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_DIR="$ROOT_DIR/sql"
SQL_STEPS=(
  "001_init_database.sql"
  "002_seed_data.sql"
  "003_align_legacy_dev_schema.sql"
  "004_world_setting_and_naming_config.sql"
  "005_world_setting_association_and_character_attributes.sql"
  "006_account_security_and_user_management.sql"
  "007_character_reuse_and_chapter_binding.sql"
  "008_outline_module.sql"
  "009_ai_writing_chat_and_workflow.sql"
)

run_sql_step() {
  local step_name="$1"
  local sql_file="$2"

  echo
  echo "➡️  [$step_name] 执行 $sql_file"
  if ! mysql -u root -p"$MYSQL_PASSWORD" < "$SQL_DIR/$sql_file"; then
    echo "❌ [$step_name] 执行失败: $sql_file"
    exit 1
  fi
  echo "✅ [$step_name] 执行成功: $sql_file"
}

echo "========================================"
echo "Story Weaver 数据库初始化脚本"
echo "========================================"

echo "1. 检查MySQL客户端..."
if ! command -v mysql >/dev/null 2>&1; then
  echo "❌ 未找到MySQL客户端，请确保MySQL已安装并添加到PATH"
  exit 1
fi

echo
read -s -p "2. 请输入MySQL root密码: " MYSQL_PASSWORD
echo
if [ -z "$MYSQL_PASSWORD" ]; then
  echo "❌ 密码不能为空"
  exit 1
fi

echo "3. 测试数据库连接..."
if ! mysql -u root -p"$MYSQL_PASSWORD" -e "SELECT 1" >/dev/null 2>&1; then
  echo "❌ 数据库连接失败，请检查密码和MySQL服务状态"
  exit 1
fi

for i in "${!SQL_STEPS[@]}"; do
  step_no=$((i + 4))
  run_sql_step "$step_no" "${SQL_STEPS[$i]}"
done

echo
echo "========================================"
echo "✅ 数据库初始化完成！"
echo "数据库: story_weaver"
echo "字符集: utf8mb4"
echo "========================================"

#!/bin/bash
# 规则引擎 demo 一键验收脚本（只用 curl + grep，不依赖 python / openpyxl）
#
#   bash scripts/rule-demo-test.sh
#   BASE=http://localhost:9008/eurekaclient bash scripts/rule-demo-test.sh   # 换个地址
#
# 覆盖：类型元数据 / 参数预览 / 脏参数拦截 / 发布立即生效 / 试算 /
#       决策表模板下载 → 上传试编译 → 发布 → 试算 / 非 xlsx 上传被拒 / 清理
set -u
BASE="${BASE:-http://localhost:9008/eurekaclient}"
JSON='Content-Type: application/json'
TMP="$(mktemp -d)"
PASS=0
FAIL=0

ok()   { PASS=$((PASS+1)); echo "  ✓ $1"; }
bad()  { FAIL=$((FAIL+1)); echo "  ✗ $1"; }
check(){ if echo "$2" | grep -q "$3"; then ok "$1"; else bad "$1  (期望匹配: $3)"; echo "     实际: $(echo "$2" | head -c 300)"; fi; }

echo "===== 0) 服务是否在跑 ====="
CODE=$(curl -s -m 10 -o /dev/null -w '%{http_code}' "$BASE/rule/state" || echo 000)
if [ "$CODE" != "200" ]; then
  echo "  ✗ ${BASE} 没有响应（http=${CODE}）"
  echo "    先起服务：cd $(cd "$(dirname "$0")/.." && pwd) && mvn spring-boot:run"
  echo "    JAVA_HOME 要指到 JDK21（Drools 10 需要 JDK17+）"
  exit 1
fi
ok "服务在跑 : $BASE"

echo
echo "===== 1) 规则类型元数据（页面表单的数据源） ====="
R=$(curl -s -m 10 "$BASE/rule/type/meta")
for T in VIP_DISCOUNT REGION_SURCHARGE STOCK_CHECK BIG_ORDER_TAG; do
  check "类型 ${T} 存在" "$R" "\"ruleType\":\"${T}\""
done
check "字段元数据带中文 label" "$R" '"fieldName":"客户等级"'

echo
echo "===== 2) 参数模板：预览 DRL ====="
R=$(curl -s -m 10 -X POST -H "$JSON" \
  -d '{"ruleType":"VIP_DISCOUNT","params":{"level":"GOLD","rate":"0.22"}}' "$BASE/rule/rule/preview")
check "渲染出 GOLD 折扣 DRL" "$R" 'customerLevel == \\"GOLD\\"'
check "折扣率写进 RHS" "$R" 'setDiscount(0.22)'

echo
echo "===== 3) 脏参数必须被拦下（折扣率 7） ====="
CODE=$(curl -s -m 10 -o "$TMP/bad.json" -w '%{http_code}' -X POST -H "$JSON" \
  -d '{"ruleType":"VIP_DISCOUNT","params":{"level":"GOLD","rate":"7"}}' "$BASE/rule/rule/publish")
[ "$CODE" = "400" ] && ok "返回 400" || bad "期望 400，实际 $CODE"
check "错误文案带中文原因" "$(cat "$TMP/bad.json")" "0 到 1"

echo
echo "===== 4) 发布规则（校验→渲染→编译→换版） ====="
R=$(curl -s -m 30 -X POST -H "$JSON" \
  -d '{"ruleType":"VIP_DISCOUNT","ruleKey":"it-gold","params":{"level":"GOLD","rate":"0.22"},"updatedBy":"it"}' \
  "$BASE/rule/rule/publish")
check "规则名/版本" "$R" '"ruleName":"VIP_DISCOUNT_it-gold"'
check "引擎规则数 4" "$R" '"ruleCount":4'
ID=$(echo "$R" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
echo "    (规则 id=${ID}，最后会删掉)"

echo
echo "===== 5) 试算：新规则立刻生效（无需重启） ====="
R=$(curl -s -m 10 -X POST -H "$JSON" \
  -d '{"customerLevel":"GOLD","region":"北京","amount":1000,"itemCount":1}' "$BASE/rule/run")
check "命中 it-gold 规则" "$R" '"VIP_DISCOUNT_it-gold"'
check "折扣 0.22" "$R" '"discount":0.22'
check "应付 780" "$R" '"finalAmount":780'

echo
echo "===== 6) 决策表：下载模板 ====="
CODE=$(curl -s -m 10 -o "$TMP/template.xlsx" -w '%{http_code}' "$BASE/rule/dt/template?assetKey=order_discount")
[ "$CODE" = "200" ] && ok "下载成功" || bad "期望 200，实际 $CODE"
SIZE=$(wc -c < "$TMP/template.xlsx" | tr -d ' ')
[ "$SIZE" -gt 3000 ] && ok "模板大小 ${SIZE}B" || bad "模板太小: ${SIZE}B"

echo
echo "===== 7) 决策表：上传试编译（回显将生成的 DRL） ====="
R=$(curl -s -m 30 -X POST -F "file=@$TMP/template.xlsx" "$BASE/rule/dt/preview?assetKey=order_discount")
check "结构+取值校验通过" "$R" '"ok":true'
check "生成 BLUE 规则" "$R" 'customerLevel == \\"BLUE\\"'
check "生成 setDiscount(0.2)" "$R" 'setDiscount(0.2)'

echo
echo "===== 8) 决策表：上传并发布 ====="
R=$(curl -s -m 30 -X POST -F "file=@$TMP/template.xlsx" -F "updatedBy=it" \
  "$BASE/rule/dt/publish?assetKey=order_discount")
check "资产已发布" "$R" '"status":1'
check "引擎规则数 5" "$R" '"ruleCount":5'

echo
echo "===== 9) 试算：决策表规则生效 ====="
R=$(curl -s -m 10 -X POST -H "$JSON" \
  -d '{"customerLevel":"BLUE","region":"北京","amount":1000,"itemCount":1}' "$BASE/rule/run")
check "折扣 0.2（来自决策表）" "$R" '"discount":0.2'
check "应付 800" "$R" '"finalAmount":800'

echo
echo "===== 10) 安全：非 xlsx / 脏文件必须被拒 ====="
echo "this is not an excel file" > "$TMP/fake.xlsx"
CODE=$(curl -s -m 20 -o "$TMP/fake.json" -w '%{http_code}' -X POST \
  -F "file=@$TMP/fake.xlsx" "$BASE/rule/dt/publish?assetKey=order_discount")
[ "$CODE" = "400" ] && ok "伪装的 xlsx 被拒（400）" || bad "期望 400，实际 $CODE"
check "提示重新下载模板" "$(cat "$TMP/fake.json")" "模板"

echo
echo "===== 11) 启停：停用规则后效果立刻消失 ====="
curl -s -m 20 -X POST -H "$JSON" -d "{\"id\":$ID,\"status\":2}" "$BASE/rule/rule/status" > /dev/null
R=$(curl -s -m 10 -X POST -H "$JSON" \
  -d '{"customerLevel":"GOLD","region":"北京","amount":1000,"itemCount":1}' "$BASE/rule/run")
check "GOLD 折扣归零" "$R" '"discount":0.0'

echo
echo "===== 12) 清理：删除测试规则 + 停用决策表 ====="
curl -s -m 20 -X POST -H "$JSON" -d "{\"id\":$ID,\"status\":0}" "$BASE/rule/rule/delete" > /dev/null
curl -s -m 20 -X POST "$BASE/rule/dt/disable?assetKey=order_discount" > /dev/null
R=$(curl -s -m 10 "$BASE/rule/state")
check "恢复到 3 条种子规则" "$R" '"publishedRules":3'
check "引擎规则数回到 3" "$R" '"ruleCount":3'

rm -rf "$TMP"
echo
echo "================= 结果：通过 $PASS / 失败 $FAIL ================="
[ "$FAIL" -eq 0 ] || exit 1

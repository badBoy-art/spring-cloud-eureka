package com.example.rule.dt;

import com.example.rule.model.DecisionTableSpec;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 生成 Drools 决策表模板 xlsx（页面"下载模板"给的就是它）。
 * 布局与官方样例 drools-decisiontables/src/test/resources/data/Sample2.drl.xlsx 完全一致：
 *   A1 RuleSet / A2 Import / A3 Notes / A5 RuleTable <名> /
 *   A6..C6 NAME|CONDITION|ACTION / 第7行 模式 / 第8行 取值模板 / 第9行 列说明 / 第10行起 规则行
 * 业务方只能改"取值"列（B、C），结构行由系统固定 —— 这也是 DecisionTableGuard 校验的基准。
 */
@Component
public class DecisionTableXlsxWriter {

    public byte[] write(DecisionTableSpec spec, List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Rules");

            put(sheet, 0, 0, "RuleSet");
            put(sheet, 0, 1, spec.getRuleSetPackage());
            put(sheet, 1, 0, "Import");
            put(sheet, 1, 1, spec.getImportClass());
            put(sheet, 2, 0, "Notes");
            put(sheet, 2, 1, "由管理页面生成，只允许修改 B/C 两列的取值");

            put(sheet, 4, 0, "RuleTable " + spec.getTableName());

            put(sheet, 5, 0, "NAME");
            put(sheet, 5, 1, "CONDITION");
            put(sheet, 5, 2, "ACTION");

            put(sheet, 6, 1, spec.getPattern());
            put(sheet, 7, 1, spec.getConditionTemplate());
            put(sheet, 7, 2, spec.getActionTemplate());
            put(sheet, 8, 1, spec.getConditionLabel());
            put(sheet, 8, 2, spec.getActionLabel());

            int rowIndex = 9;
            if (rows != null) {
                for (String[] rowData : rows) {
                    put(sheet, rowIndex, 0, rowData[0]);
                    put(sheet, rowIndex, 1, rowData[1]);
                    put(sheet, rowIndex, 2, rowData[2]);
                    rowIndex++;
                }
            }
            if (rowIndex == 9) {
                // 空模板也给一行示例，避免业务方从空表开始填。
                // 示例等级取白名单最后一个（BLUE）：它没有被种子 DRL 规则覆盖，模板原样上传跑出来的
                // 结果就是确定的，不会和"客户等级折扣"规则抢同一个 LHS。
                String sampleLevel = spec.getConditionWhitelist().isEmpty()
                        ? "NORMAL"
                        : spec.getConditionWhitelist().get(spec.getConditionWhitelist().size() - 1);
                put(sheet, rowIndex, 0, "示例-" + sampleLevel + "客户");
                put(sheet, rowIndex, 1, sampleLevel);
                put(sheet, rowIndex, 2, "0.2");
            }

            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            sheet.setColumnWidth(2, 44 * 256);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("生成决策表模板失败: " + e.getMessage(), e);
        }
    }

    private void put(Sheet sheet, int rowIndex, int colIndex, String value) {
        if (value == null) {
            return;
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        row.createCell(colIndex).setCellValue(value);
    }
}

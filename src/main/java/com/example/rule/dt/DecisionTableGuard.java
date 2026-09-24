package com.example.rule.dt;

import com.example.rule.model.DecisionTableSpec;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;

/**
 * 上传 xlsx 的守门人。**这一步是安全边界，不是格式检查**：
 * 决策表的 CONDITION / ACTION 单元格会被 Drools 编译成规则 RHS，等于任意 Java 代码
 * （"$o.setRejected(true); Runtime.getRuntime().exec(\"rm -rf /\")" 也编得过）。
 * 所以结构行必须逐字节等于模板，取值列必须落在白名单/正则内，且一律禁公式。
 */
@Component
public class DecisionTableGuard {

    private static final Pattern RULE_NAME = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9_\\-]{1,32}$");
    private static final int MAX_RULE_ROWS = 500;
    private static final DataFormatter FORMATTER = new DataFormatter();

    /**
     * POI 默认的 zip 炸弹防护阈值（MIN_INFLATE_RATIO=0.01）会误杀正常的 Excel 文件：
     * 用 Excel/WPS 另存出来的 xlsx，styles.xml 往往高度可压缩、比率低于阈值，
     * 直接抛 "Zip bomb detected!"。业务方上传的就是他们自己另存的文件，必须放宽阈值。
     * 用静态块而不是 @PostConstruct：单元测试里直接 new 也能生效。
     */
    static {
        ZipSecureFile.setMinInflateRatio(0.001);
    }

    /** 校验不通过直接抛 IllegalArgumentException（页面显示中文原因）。 */
    public void check(DecisionTableSpec spec, byte[] xlsx) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheetAt(0);
            checkStructure(spec, sheet);
            checkRows(spec, sheet);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("xlsx 解析失败，请使用页面下载的模板重新填写: " + e.getMessage(), e);
        }
    }

    /** 结构行逐格比对（业务方不能动这些行）。 */
    private void checkStructure(DecisionTableSpec spec, Sheet sheet) {
        expect(sheet, 0, 0, "RuleSet");
        expect(sheet, 0, 1, spec.getRuleSetPackage());
        expect(sheet, 1, 0, "Import");
        expect(sheet, 1, 1, spec.getImportClass());

        String tableHeader = text(sheet, 4, 0);
        if (!tableHeader.startsWith("RuleTable")) {
            throw new IllegalArgumentException("A5 必须是 RuleTable <表名>，当前: " + tableHeader);
        }
        expect(sheet, 5, 0, "NAME");
        expect(sheet, 5, 1, "CONDITION");
        expect(sheet, 5, 2, "ACTION");
        expect(sheet, 6, 1, spec.getPattern());
        expect(sheet, 7, 1, spec.getConditionTemplate());
        expect(sheet, 7, 2, spec.getActionTemplate());
        expect(sheet, 8, 1, spec.getConditionLabel());
        expect(sheet, 8, 2, spec.getActionLabel());
    }

    /** 取值行校验：只允许改 NAME / CONDITION / ACTION 三列的取值，且取值受限。 */
    private void checkRows(DecisionTableSpec spec, Sheet sheet) {
        int ruleRows = 0;
        for (int rowIndex = 9; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            rejectExtraColumns(row);

            String ruleName = text(row.getCell(0));
            String conditionValue = text(row.getCell(1));
            String actionValue = text(row.getCell(2));
            if (ruleName.isEmpty() && conditionValue.isEmpty() && actionValue.isEmpty()) {
                continue;
            }
            int excelRow = rowIndex + 1;
            checkPattern(ruleName, RULE_NAME, "第 " + excelRow + " 行规则名");
            checkConditionValue(spec, conditionValue, excelRow);
            checkPattern(actionValue, Pattern.compile(spec.getActionPattern()), "第 " + excelRow + " 行动作取值");

            ruleRows++;
            if (ruleRows > MAX_RULE_ROWS) {
                throw new IllegalArgumentException("规则行数超过上限 " + MAX_RULE_ROWS + "，请拆表");
            }
        }
        if (ruleRows == 0) {
            throw new IllegalArgumentException("决策表里没有任何规则行（第 10 行起填写）");
        }
    }

    /** D 列及以后不允许出现任何内容：挡住"往右边偷偷加一列 ACTION 写代码"这条路。 */
    private void rejectExtraColumns(Row row) {
        for (int colIndex = 3; colIndex < row.getLastCellNum(); colIndex++) {
            String value = text(row.getCell(colIndex));
            if (!value.isEmpty()) {
                throw new IllegalArgumentException("第 " + (row.getRowNum() + 1) + " 行第 " + (colIndex + 1)
                        + " 列不允许填写内容，只能修改 A/B/C 三列");
            }
        }
    }

    private void checkConditionValue(DecisionTableSpec spec, String value, int excelRow) {
        if (spec.getConditionWhitelist() != null && !spec.getConditionWhitelist().isEmpty()) {
            if (!spec.getConditionWhitelist().contains(value)) {
                throw new IllegalArgumentException("第 " + excelRow + " 行条件取值[" + value + "]不在允许范围: "
                        + String.join("/", spec.getConditionWhitelist()));
            }
            return;
        }
        checkPattern(value, Pattern.compile(spec.getValuePattern()), "第 " + excelRow + " 行条件取值");
    }

    private void checkPattern(String value, Pattern pattern, String where) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(where + "[" + value + "]格式非法，只允许字母/数字/下划线/短横或规定数值");
        }
    }

    private void expect(Sheet sheet, int rowIndex, int colIndex, String expected) {
        String actual = text(sheet, rowIndex, colIndex);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("模板结构不匹配：第 " + (rowIndex + 1) + " 行第 " + (colIndex + 1)
                    + " 列应为 [" + expected + "]，实际 [" + actual + "]。请重新下载模板填写");
        }
    }

    private String text(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? "" : text(row.getCell(colIndex));
    }

    /** 公式单元格一律拒绝（Excel 公式与 OLE 对象是另一条注入口子）。 */
    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            throw new IllegalArgumentException("单元格 " + cell.getAddress() + " 不允许使用公式");
        }
        if (type == CellType.BLANK) {
            return "";
        }
        return FORMATTER.formatCellValue(cell).trim();
    }
}

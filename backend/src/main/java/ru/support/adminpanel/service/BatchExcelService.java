package ru.support.adminpanel.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Генерация Excel-шаблона и разбор загруженного файла "Счёт / Банк" для массового
 * запуска задачи "Выдача прав доступа..." сразу по всем банкам эталонной роли
 * (см. ExecutionService.executeBatch). Формат — ровно как в шаблоне: колонка A —
 * "Счет", колонка B — "Банк", данные начиная со 2-й строки.
 */
@Component
public class BatchExcelService {

    private static final String HEADER_ACCOUNT = "Счет";
    private static final String HEADER_BANK = "Банк";

    public record AccountRow(int rowNumber, String account, String bank) {
    }

    /** Формирует шаблон .xlsx с заголовками и выпадающим списком допустимых банков роли. */
    public byte[] buildTemplate(List<String> bankNames) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Счета");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            Cell accCell = header.createCell(0);
            accCell.setCellValue(HEADER_ACCOUNT);
            accCell.setCellStyle(headerStyle);
            Cell bankCell = header.createCell(1);
            bankCell.setCellValue(HEADER_BANK);
            bankCell.setCellStyle(headerStyle);

            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 20 * 256);

            if (bankNames != null && !bankNames.isEmpty()) {
                DataValidationHelper dvHelper = sheet.getDataValidationHelper();
                DataValidationConstraint constraint = dvHelper.createExplicitListConstraint(
                        bankNames.toArray(new String[0]));
                // Строки 2..201 — с запасом, чтобы список подсказки работал на весь
                // разумный объём загружаемых счетов.
                CellRangeAddressList addressList = new CellRangeAddressList(1, 200, 1, 1);
                DataValidation validation = dvHelper.createValidation(constraint, addressList);
                validation.setShowErrorBox(true);
                validation.createErrorBox("Неизвестный банк",
                        "Выберите банк из списка: " + String.join(", ", bankNames));
                sheet.addValidationData(validation);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сформировать Excel-шаблон: " + e.getMessage(), e);
        }
    }

    /** Разбирает загруженный файл. Бросает IllegalArgumentException с понятным
     *  сообщением о номере строки при некорректном формате. */
    public List<AccountRow> parse(MultipartFile file) {
        List<AccountRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String account = formatter.formatCellValue(row.getCell(0)).trim();
                String bank = formatter.formatCellValue(row.getCell(1)).trim();
                if (account.isEmpty() && bank.isEmpty()) continue; // полностью пустая строка — пропускаем
                if (account.isEmpty() || bank.isEmpty()) {
                    throw new IllegalArgumentException("Строка " + (i + 1)
                            + ": заполнены не оба столбца (Счёт и Банк обязательны)");
                }
                rows.add(new AccountRow(i + 1, account, bank));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать Excel-файл: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("В файле нет ни одной заполненной строки со счётом и банком");
        }
        return rows;
    }
}

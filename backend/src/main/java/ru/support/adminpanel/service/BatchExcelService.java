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

    /** Номер счёта — ровно 20 символов (правило бизнеса, см. также ExecutionService.execute
     *  для обычного, не массового, запуска — там та же проверка для поля "accounts"). */
    public static final int ACCOUNT_LENGTH = 20;

    public record AccountRow(int rowNumber, String account, String bank) {
    }

    /** Формирует шаблон .xlsx с заголовками, текстовым форматом столбца "Счет"
     *  (иначе Excel при вводе длинного числа переводит его в экспоненциальную
     *  запись и теряет значащие цифры) и выпадающим списком банков роли. */
    public byte[] buildTemplate(List<String> bankNames) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Счета");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Текстовый формат для ВСЕГО столбца "Счет" (не только уже созданных ячеек),
            // чтобы и строки, которые пользователь допишет сам, не превращались Excel'ем
            // из "40702810500012345678" в "4,07028E+20".
            CellStyle accountColumnStyle = wb.createCellStyle();
            accountColumnStyle.setDataFormat(wb.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(0, accountColumnStyle);

            Row header = sheet.createRow(0);
            Cell accCell = header.createCell(0);
            accCell.setCellValue(HEADER_ACCOUNT);
            accCell.setCellStyle(headerStyle);
            Cell bankCell = header.createCell(1);
            bankCell.setCellValue(HEADER_BANK);
            bankCell.setCellStyle(headerStyle);

            sheet.setColumnWidth(0, 24 * 256);
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

    /** Разбирает загруженный файл и проверяет каждую строку (заполненность обоих
     *  столбцов, длина счёта — ровно {@link #ACCOUNT_LENGTH} символов). Если
     *  найдены ошибки — бросает IllegalArgumentException со списком ВСЕХ
     *  проблемных строк сразу (чтобы не заставлять пользователя грузить файл
     *  заново после исправления каждой строки по одной). */
    public List<AccountRow> parse(MultipartFile file) {
        List<AccountRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String account = formatter.formatCellValue(row.getCell(0)).trim();
                String bank = formatter.formatCellValue(row.getCell(1)).trim();
                if (account.isEmpty() && bank.isEmpty()) continue; // полностью пустая строка — пропускаем
                int rowNumber = i + 1;
                if (account.isEmpty() || bank.isEmpty()) {
                    errors.add("строка " + rowNumber + ": заполнены не оба столбца (Счёт и Банк обязательны)");
                    continue;
                }
                if (account.length() != ACCOUNT_LENGTH) {
                    errors.add("строка " + rowNumber + ": номер счёта \"" + account + "\" — " + account.length()
                            + " символ(ов) вместо " + ACCOUNT_LENGTH
                            + " (проверьте, что столбец \"Счет\" отформатирован как текст, и исправьте номер)");
                    continue;
                }
                rows.add(new AccountRow(rowNumber, account, bank));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось прочитать Excel-файл: " + e.getMessage());
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Ошибки в файле — исправьте и загрузите заново:\n"
                    + String.join("\n", errors));
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("В файле нет ни одной заполненной строки со счётом и банком");
        }
        return rows;
    }
}

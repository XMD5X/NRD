package ru.support.adminpanel.dto;

import java.util.List;

/** Один банк из результата массового запуска "Все банки" (см. ExecutionService.executeBatch) —
 *  какие именно счета были прочитаны из Excel-файла и по какому банку, для наглядного
 *  подтверждения пользователю "что загружено и готово к отправке" на UI. */
public class BatchBankSummary {
    private final String bank;
    private final List<String> accounts;

    public BatchBankSummary(String bank, List<String> accounts) {
        this.bank = bank;
        this.accounts = accounts;
    }

    public String getBank() { return bank; }
    public List<String> getAccounts() { return accounts; }
    public int getAccountCount() { return accounts.size(); }
}

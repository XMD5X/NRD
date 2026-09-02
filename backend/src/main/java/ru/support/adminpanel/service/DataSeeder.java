package ru.support.adminpanel.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.support.adminpanel.config.AppProperties;
import ru.support.adminpanel.entity.Role;
import ru.support.adminpanel.entity.ScriptEntity;
import ru.support.adminpanel.entity.ScriptType;
import ru.support.adminpanel.entity.User;
import ru.support.adminpanel.repository.ScriptRepository;
import ru.support.adminpanel.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Начальные (демо) данные: администратор, бизнес-пользователь и два примера скриптов —
 * чтобы MVP можно было опробовать сразу после первого запуска "из коробки".
 * Пароли — см. README.md проекта. Это временные учётные данные MVP (см. HLD раздел 8).
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ScriptRepository scriptRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public DataSeeder(UserRepository userRepository, ScriptRepository scriptRepository,
                       PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.scriptRepository = scriptRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        seedUsers();
        seedScripts();
        deprecateOldPermissionsDemo();
    }

    /**
     * Деактивирует записи из более ранних версий этой задачи, замененные текущим форматом
     * "категория — банк": (1) самый первый демо-скрипт "Генерация запроса на полномочия по
     * счетам" (bash); (2) промежуточный формат имени "Выдача прав доступа на счета для
     * эталонных ролей — БАНК", когда была заведена только одна категория (ГРО) и категория
     * не входила в название. На инсталляциях, где это уже было засеяно раньше, такие записи
     * деактивируются, чтобы не дублировать банки рядом с новыми "category — bank" записями.
     */
    private void deprecateOldPermissionsDemo() {
        scriptRepository.findAll().stream()
                .filter(s -> s.isActive() && (
                        "Генерация запроса на полномочия по счетам".equals(s.getName())
                        || s.getName().startsWith("Выдача прав доступа на счета для эталонных ролей — ")))
                .forEach(s -> {
                    s.setActive(false);
                    scriptRepository.save(s);
                });
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User();
        admin.setLogin("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin12345"));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        User business = new User();
        business.setLogin("business");
        business.setPasswordHash(passwordEncoder.encode("business12345"));
        business.setRole(Role.BUSINESS_USER);
        userRepository.save(business);
    }

    private void seedScripts() throws Exception {
        User admin = userRepository.findByLogin("admin").orElseThrow();

        Path scriptsDir = Path.of(props.getScriptsDir());
        Files.createDirectories(scriptsDir);

        seedAccessRightsScripts(scriptsDir, admin);
        seedEcpGenerationScripts(scriptsDir, admin);

        String statusDemoName = "Проверка статуса пользователя";
        if (!scriptRepository.existsByName(statusDemoName)) {
            Path statusScript = copyClasspathScript("sample-scripts/check_user_status.py", scriptsDir);
            ScriptEntity s2 = new ScriptEntity();
            s2.setName(statusDemoName);
            s2.setDescription("Демонстрационный Python-скрипт: проверяет статус пользователя по ID.");
            s2.setFilePath(statusScript.toString());
            s2.setScriptType(ScriptType.PYTHON);
            s2.setParametersConfig("[{\"name\":\"user_id\",\"label\":\"User ID\",\"type\":\"text\"}]");
            s2.setVisibleToRole(Role.ADMIN);
            s2.setUploadedBy(admin.getId());
            scriptRepository.save(s2);
        }
    }

    /**
     * Задача "Выдача прав доступа на счета для эталонных ролей" (бывш. "Генерация запроса
     * на полномочия по счетам"). Дерево: категория ("эталонная роль") -> банк -> скрипт.
     * Категории и банки заведены по мере поступления реальных .ps1-скриптов от бизнеса
     * (присылались отдельными итерациями — см. историю).
     */
    private void seedAccessRightsScripts(Path scriptsDir, User admin) throws Exception {
        final String parametersConfig = "[{\"name\":\"userId\",\"label\":\"User ID\",\"type\":\"text\"},"
                + "{\"name\":\"accounts\",\"label\":\"Номера счетов через запятую\",\"type\":\"text\"}]";

        List<CategorySeed> categories = new ArrayList<>();

        categories.add(new CategorySeed("ГРО (Платежи в рублях)", "gro", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "РАЙФ", "РСХБ", "СБЕР", "ЮКБ")));

        categories.add(new CategorySeed("ГУП (Платежи без импорта, этап 2)", "gup", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "РАЙФ", "РСХБ", "СБЕР", "ЮКБ")));

        categories.add(new CategorySeed("Права просмотра на все счета", "prosmotr_vlozheniy", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "КЕБ", "РОССЕЛЬХОЗ", "СБЕР", "ЮКБ")));
        // Примечание: в этой категории пока нет скрипта для РАЙФ — банк не прислали.

        categories.add(new CategorySeed("Валютный контроль (Рубли)", "vk_rub", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "РАЙФ", "РСХБ", "СБЕР", "ЮКБ")));

        categories.add(new CategorySeed("Валютный контроль (Валюта)", "vk_valuta", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "РАЙФ", "РСХБ", "СБЕР", "ЮКБ")));

        categories.add(new CategorySeed("SAP-PI", "sap_pi", banksOf(
                "АЛЬФА", "ВТБ", "ГПБ", "РАЙФ", "РСХБ", "СБЕР", "ЮКБ")));

        for (CategorySeed cat : categories) {
            for (String bank : cat.banks) {
                String name = cat.category + " — " + bank;
                if (scriptRepository.existsByName(name)) {
                    continue; // уже засеяно при предыдущем запуске (идемпотентность между итерациями)
                }
                String classpathScript = "sample-scripts/" + cat.folder + "/" + cat.folder + "_" + bank + ".ps1";
                Path scriptFile = copyClasspathScript(classpathScript, scriptsDir);

                ScriptEntity s = new ScriptEntity();
                s.setName(name);
                s.setDescription("Формирует JSON-пакет прав доступа к документам для банка " + bank
                        + " (эталонная роль: " + cat.category + ") по указанному user_id и списку счетов.");
                s.setFilePath(scriptFile.toString());
                s.setScriptType(ScriptType.POWERSHELL);
                s.setParametersConfig(parametersConfig);
                s.setCategory(cat.category);
                s.setBankName(bank);
                s.setVisibleToRole(null); // видно и админу, и бизнес-пользователю
                s.setUploadedBy(admin.getId());
                scriptRepository.save(s);
            }
        }
    }

    /**
     * Задача "Генерация ЭЦП" — отдельная от дерева "Выдача прав доступа...": не привязана
     * к user_id/счетам, а генерирует по одному JSON-файлу регистрации сертификата на каждую
     * введённую ЭЦП (комментарий, номер сертификата, дата истечения — три списка через
     * запятую в одном порядке). На фронтенде отображается отдельной плашкой (см.
     * DashboardPage.jsx: категории вне набора "эталонных ролей" получают свою карточку).
     */
    private void seedEcpGenerationScripts(Path scriptsDir, User admin) throws Exception {
        String name = "Генерация ЭЦП — RaiffeisenBankApi";
        if (scriptRepository.existsByName(name)) {
            return; // уже засеяно при предыдущем запуске
        }
        Path scriptFile = copyClasspathScript("sample-scripts/ecp/generation_ECP_RaiffeisenBankApi.py", scriptsDir);

        ScriptEntity s = new ScriptEntity();
        s.setName(name);
        s.setDescription("Формирует JSON-файл регистрации ЭЦП (сертификата) для модуля RaiffeisenBankApi. "
                + "На каждую введённую ЭЦП создаётся отдельный JSON-файл; при нескольких ЭЦП результат "
                + "скачивается одним zip-архивом.");
        s.setFilePath(scriptFile.toString());
        s.setScriptType(ScriptType.PYTHON);
        s.setParametersConfig("[{\"name\":\"comments\",\"label\":\"Комментарии (через запятую, порядок как у номеров/дат)\",\"type\":\"text\"},"
                + "{\"name\":\"numbers_ecp\",\"label\":\"Номера сертификатов ЭЦП (через запятую)\",\"type\":\"text\"},"
                + "{\"name\":\"exp_dates_ecp\",\"label\":\"Даты истечения ДД.ММ.ГГГГ (через запятую)\",\"type\":\"text\"}]");
        s.setCategory("Генерация ЭЦП");
        s.setBankName("РАЙФ");
        s.setVisibleToRole(null); // видно и админу, и бизнес-пользователю
        s.setUploadedBy(admin.getId());
        scriptRepository.save(s);
    }

    private static List<String> banksOf(String... banks) {
        return Arrays.asList(banks);
    }

    private record CategorySeed(String category, String folder, List<String> banks) {
    }

    private Path copyClasspathScript(String classpathLocation, Path targetDir) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        Path target = targetDir.resolve(Path.of(classpathLocation).getFileName().toString());
        try (var in = resource.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        target.toFile().setExecutable(true);
        return target;
    }
}

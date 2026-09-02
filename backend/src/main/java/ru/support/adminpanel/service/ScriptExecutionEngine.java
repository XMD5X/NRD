package ru.support.adminpanel.service;

import org.springframework.stereotype.Component;
import ru.support.adminpanel.entity.ScriptType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Запуск скриптов (PowerShell/Python/Bash) локальными процессами.
 * Соответствует HLD: backend выполняет скрипты на своей же машине через ProcessBuilder.
 *
 * ВАЖНО для авторов скриптов: скрипт должен принимать входные параметры как
 * позиционные аргументы командной строки (в порядке, заданном в parametersConfig),
 * а НЕ через интерактивный ввод (Read-Host и подобное) — панель запускает
 * скрипт неинтерактивно и не сможет ответить на приглашения ввода.
 */
@Component
public class ScriptExecutionEngine {

    public static class Result {
        public int exitCode;
        public String stdout;
        public String stderr;
        public boolean timedOut;
    }

    public Result run(ScriptType type, String filePath, List<String> orderedParamValues, Path workingDir) {
        // Скрипт запускается с рабочей директорией = папка результатов конкретного
        // выполнения (см. ExecutionService), поэтому путь к файлу скрипта ОБЯЗАН быть
        // абсолютным — иначе относительный путь (например, "./data/scripts/x.sh")
        // будет искаться внутри papки результатов, а не там, где он реально лежит.
        String absoluteFilePath = Path.of(filePath).toAbsolutePath().normalize().toString();

        List<String> command = new ArrayList<>();
        switch (type) {
            case BASH -> {
                command.add("bash");
                command.add(absoluteFilePath);
            }
            case PYTHON -> {
                command.add("python3");
                command.add(absoluteFilePath);
            }
            case POWERSHELL -> {
                // На Linux-хосте для PowerShell требуется установленный pwsh.
                // На целевом Windows Server (см. HLD) используется штатный powershell.exe.
                command.add("pwsh");
                command.add("-File");
                command.add(absoluteFilePath);
            }
        }
        command.addAll(orderedParamValues);

        Result result = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (workingDir != null) {
                pb.directory(workingDir.toFile());
            }
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.timedOut = true;
                result.exitCode = -1;
            } else {
                result.exitCode = process.exitValue();
            }
            result.stdout = stdout;
            result.stderr = stderr;
        } catch (IOException | InterruptedException ex) {
            result.exitCode = -1;
            result.stdout = "";
            result.stderr = "Ошибка запуска скрипта: " + ex.getMessage();
        }
        return result;
    }

    public static Map<String, String> emptyIfNull(Map<String, String> map) {
        return map == null ? Map.of() : map;
    }
}

package ru.support.adminpanel.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Зарегистрированный в системе скрипт (PowerShell/Python/Bash), добавленный администратором.
 * Файл параметров (parametersConfig) — JSON-массив описания полей формы ввода,
 * настраивается разработчиком при добавлении скрипта (см. HLD, границы MVP).
 */
@Entity
@Table(name = "scripts")
public class ScriptEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "script_type", nullable = false)
    private ScriptType scriptType;

    /** JSON-описание входных параметров формы, например:
     *  [{"name":"user_id","label":"User ID","type":"text"},{"name":"accounts","label":"Счета через запятую","type":"text"}] */
    @Column(name = "parameters_config", columnDefinition = "text")
    private String parametersConfig = "[]";

    /** Путь к отдельному скрипту "отправки" в целевую систему (если есть отдельный шаг отправки). Может быть null. */
    @Column(name = "send_script_path")
    private String sendScriptPath;

    /** Категория ("эталонная роль") для группировки в дереве задач, например "ГРО (Платежи в рублях)". Может быть null (плоский список). */
    @Column(name = "category")
    private String category;

    /** Название банка (второй уровень дерева), извлекается из имени файла скрипта при загрузке. Может быть null. */
    @Column(name = "bank_name")
    private String bankName;

    /** Кто может видеть скрипт в списке задач: ADMIN, BUSINESS_USER или null (оба). Администратор видит всё всегда. */
    @Enumerated(EnumType.STRING)
    @Column(name = "visible_to_role")
    private Role visibleToRole;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public ScriptType getScriptType() { return scriptType; }
    public void setScriptType(ScriptType scriptType) { this.scriptType = scriptType; }

    public String getParametersConfig() { return parametersConfig; }
    public void setParametersConfig(String parametersConfig) { this.parametersConfig = parametersConfig; }

    public String getSendScriptPath() { return sendScriptPath; }
    public void setSendScriptPath(String sendScriptPath) { this.sendScriptPath = sendScriptPath; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public Role getVisibleToRole() { return visibleToRole; }
    public void setVisibleToRole(Role visibleToRole) { this.visibleToRole = visibleToRole; }

    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }

    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

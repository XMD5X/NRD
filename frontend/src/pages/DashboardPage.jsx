import React, { useEffect, useMemo, useState } from 'react'
import client from '../api/client.js'
import { logger } from '../logging/logger.js'
import ExecutionPanel from './ExecutionPanel.jsx'

// Название корневого пункта меню для задачи с деревом "категория (эталонная роль) -> банк".
// Скрипты, у которых backend возвращает непустое поле category, автоматически
// попадают в это дерево — НО только категории из ACCESS_RIGHTS_CATEGORIES ниже.
// Любая другая категория (например, "Генерация ЭЦП") отображается отдельной
// плашкой (карточкой) — см. renderOtherCategoryCard.
const ACCESS_RIGHTS_ROOT = 'Выдача прав доступа на счета для эталонных ролей'

const ACCESS_RIGHTS_CATEGORIES = new Set([
  'ГРО (Платежи в рублях)',
  'ГУП (Платежи без импорта, этап 2)',
  'Права просмотра на все счета',
  'Валютный контроль (Рубли)',
  'Валютный контроль (Валюта)',
  'SAP-PI',
])

export default function DashboardPage() {
  const [scripts, setScripts] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [category, setCategory] = useState('')
  const [bankScriptId, setBankScriptId] = useState('')
  const [flatSelectedId, setFlatSelectedId] = useState('')
  const [otherSelectedByCategory, setOtherSelectedByCategory] = useState({})

  useEffect(() => {
    logger.trace('Загрузка списка доступных задач', 'dashboard')
    client.get('/scripts')
      .then((res) => {
        setScripts(res.data)
        logger.info(`Получено задач: ${res.data.length}`, 'dashboard')
      })
      .catch((err) => {
        // Без .catch() запрос, упавший по любой причине (истёкшая сессия, сеть,
        // ошибка сервера), оставлял бы страницу висеть на "Загрузка..." навсегда —
        // loading никогда не становился бы false. При 401 (протухший/невалидный
        // токен) interceptor в api/client.js сам перенаправит на /login, здесь просто
        // не даём странице зависнуть на остальных ошибках.
        logger.warning(`Ошибка загрузки списка задач: ${err.message}`, 'dashboard')
        setLoadError('Не удалось загрузить список задач. Попробуйте обновить страницу.')
      })
      .finally(() => setLoading(false))
  }, [])

  const categorized = useMemo(() => scripts.filter((s) => s.category), [scripts])
  const flat = useMemo(() => scripts.filter((s) => !s.category), [scripts])

  const accessRightsScripts = useMemo(
    () => categorized.filter((s) => ACCESS_RIGHTS_CATEGORIES.has(s.category)),
    [categorized]
  )
  const otherCategorized = useMemo(
    () => categorized.filter((s) => !ACCESS_RIGHTS_CATEGORIES.has(s.category)),
    [categorized]
  )

  const accessRightsCategoryList = useMemo(() => {
    const seen = []
    for (const s of accessRightsScripts) {
      if (!seen.includes(s.category)) seen.push(s.category)
    }
    return seen
  }, [accessRightsScripts])

  // Отдельные категории (плашки) — каждая своя карточка с деревом "категория -> банк".
  const otherCategoryGroups = useMemo(() => {
    const map = new Map()
    for (const s of otherCategorized) {
      if (!map.has(s.category)) map.set(s.category, [])
      map.get(s.category).push(s)
    }
    return Array.from(map.entries())
  }, [otherCategorized])

  const banksInCategory = accessRightsScripts.filter((s) => s.category === category)
  const selectedBankScript = banksInCategory.find((s) => s.id === bankScriptId)
  const flatSelected = flat.find((s) => s.id === flatSelectedId)

  // Если в категории (плашке) всего один вариант банка/модуля — выбираем его автоматически,
  // чтобы не показывать бесполезный select из одного пункта.
  useEffect(() => {
    setOtherSelectedByCategory((prev) => {
      let changed = false
      const next = { ...prev }
      for (const [cat, catScripts] of otherCategoryGroups) {
        if (catScripts.length === 1 && !next[cat]) {
          next[cat] = catScripts[0].id
          changed = true
        }
      }
      return changed ? next : prev
    })
  }, [otherCategoryGroups])

  function handleCategoryChange(value) {
    setCategory(value)
    setBankScriptId('')
  }

  function handleOtherSelect(cat, scriptId) {
    setOtherSelectedByCategory((prev) => ({ ...prev, [cat]: scriptId }))
  }

  return (
    <div>
      <h1>Доступные задачи</h1>
      {loading && <p>Загрузка...</p>}
      {!loading && loadError && <p className="error">{loadError}</p>}
      {!loading && !loadError && scripts.length === 0 && <p>Нет доступных задач.</p>}

      {!loading && accessRightsScripts.length > 0 && (
        <div className="card">
          <h2>{ACCESS_RIGHTS_ROOT}</h2>
          <label>
            Эталонная роль
            <select value={category} onChange={(e) => handleCategoryChange(e.target.value)}>
              <option value="">-- выбрать --</option>
              {accessRightsCategoryList.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
          {category && (
            <label>
              Банк
              <select value={bankScriptId} onChange={(e) => setBankScriptId(e.target.value)}>
                <option value="">-- выбрать --</option>
                {banksInCategory.map((s) => (
                  <option key={s.id} value={s.id}>{s.bankName || s.name}</option>
                ))}
              </select>
            </label>
          )}
          {selectedBankScript && <p className="hint">{selectedBankScript.description}</p>}
        </div>
      )}

      {!loading && otherCategoryGroups.map(([cat, catScripts]) => {
        const selectedId = otherSelectedByCategory[cat] || ''
        const selected = catScripts.find((s) => s.id === selectedId)
        const hasMultipleBanks = catScripts.length > 1
        return (
          <div className="card" key={cat}>
            <h2>{cat}</h2>
            {hasMultipleBanks && (
              <label>
                Банк / модуль
                <select value={selectedId} onChange={(e) => handleOtherSelect(cat, e.target.value)}>
                  <option value="">-- выбрать --</option>
                  {catScripts.map((s) => (
                    <option key={s.id} value={s.id}>{s.bankName || s.name}</option>
                  ))}
                </select>
              </label>
            )}
            {selected && <p className="hint">{selected.description}</p>}
            {selected && <ExecutionPanel key={selected.id} script={selected} />}
          </div>
        )
      })}

      {!loading && flat.length > 0 && (
        <div className="card">
          <label>
            Выберите задачу
            <select value={flatSelectedId} onChange={(e) => setFlatSelectedId(e.target.value)}>
              <option value="">-- выбрать --</option>
              {flat.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </label>
          {flatSelected && <p className="hint">{flatSelected.description}</p>}
        </div>
      )}

      {selectedBankScript && <ExecutionPanel key={selectedBankScript.id} script={selectedBankScript} />}
      {flatSelected && <ExecutionPanel key={flatSelected.id} script={flatSelected} />}
    </div>
  )
}

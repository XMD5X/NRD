import React, { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import useScripts from '../hooks/useScripts.js'
import { ACCESS_RIGHTS_ROOT, groupScripts } from '../taskGroups.js'
import ExecutionPanel from './ExecutionPanel.jsx'
import BulkAccessPanel from './BulkAccessPanel.jsx'

// Служебное значение select'а "Банк" для режима "сразу по всем банкам" — не
// пересекается с реальными UUID скриптов, поэтому безопасно как маркер.
const ALL_BANKS = '__ALL_BANKS__'

// Одна страница на все три вида задач (mode передаётся из маршрута в App.jsx):
//  - "access-rights" — единственная задача с деревом "эталонная роль -> банк"
//  - "category"       — задача-плашка с одной категорией (может быть 1 или
//                        несколько вариантов банка/модуля внутри)
//  - "script"         — задача без категории (плоский список), один скрипт = страница
export default function TaskDetailPage({ mode }) {
  const { categoryName, scriptId } = useParams()
  const { scripts, loading, loadError } = useScripts()
  const groups = useMemo(() => groupScripts(scripts), [scripts])

  const [role, setRole] = useState('')
  const [accessBankId, setAccessBankId] = useState('')
  const [categoryBankId, setCategoryBankId] = useState('')

  const banksInRole = useMemo(
    () => groups.accessRightsScripts.filter((s) => s.category === role),
    [groups.accessRightsScripts, role]
  )
  const isAllBanks = accessBankId === ALL_BANKS
  const accessSelected = banksInRole.find((s) => s.id === accessBankId)

  const decodedCategory = categoryName ? decodeURIComponent(categoryName) : ''
  const categoryEntry = useMemo(
    () => groups.otherCategoryGroups.find(([c]) => c === decodedCategory),
    [groups.otherCategoryGroups, decodedCategory]
  )
  const categoryScripts = categoryEntry ? categoryEntry[1] : []
  // Если в задаче всего один вариант банка/модуля — он выбран сразу, без
  // бесполезного select из одного пункта (как и раньше на дашборде).
  const effectiveCategoryBankId = categoryBankId || (categoryScripts.length === 1 ? categoryScripts[0].id : '')
  const categorySelected = categoryScripts.find((s) => s.id === effectiveCategoryBankId)

  const flatScript = groups.flat.find((s) => s.id === scriptId)

  const backLink = <Link to="/" className="task-back">← Доступные задачи</Link>

  if (loading) return <div>{backLink}<p>Загрузка...</p></div>
  if (loadError) return <div>{backLink}<p className="error">{loadError}</p></div>

  if (mode === 'access-rights') {
    return (
      <div>
        {backLink}
        <div className="card">
          <h1>{ACCESS_RIGHTS_ROOT}</h1>
          <label>
            Эталонная роль
            <select value={role} onChange={(e) => { setRole(e.target.value); setAccessBankId('') }}>
              <option value="">-- выбрать --</option>
              {groups.accessRightsCategoryList.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>
          {role && (
            <label>
              Банк
              <select value={accessBankId} onChange={(e) => setAccessBankId(e.target.value)}>
                <option value="">-- выбрать --</option>
                {banksInRole.map((s) => (
                  <option key={s.id} value={s.id}>{s.bankName || s.name}</option>
                ))}
                <option value={ALL_BANKS}>Все банки</option>
              </select>
            </label>
          )}
          {accessSelected && <p className="hint">{accessSelected.description}</p>}
        </div>
        {accessSelected && <ExecutionPanel key={accessSelected.id} script={accessSelected} />}
        {isAllBanks && <BulkAccessPanel key={`all-banks:${role}`} category={role} />}
      </div>
    )
  }

  if (mode === 'category') {
    if (!categoryEntry) {
      return <div>{backLink}<p className="error">Задача не найдена.</p></div>
    }
    const hasMultiple = categoryScripts.length > 1
    return (
      <div>
        {backLink}
        <div className="card">
          <h1>{decodedCategory}</h1>
          {hasMultiple && (
            <label>
              Банк / модуль
              <select value={effectiveCategoryBankId} onChange={(e) => setCategoryBankId(e.target.value)}>
                <option value="">-- выбрать --</option>
                {categoryScripts.map((s) => (
                  <option key={s.id} value={s.id}>{s.bankName || s.name}</option>
                ))}
              </select>
            </label>
          )}
          {categorySelected && <p className="hint">{categorySelected.description}</p>}
        </div>
        {categorySelected && <ExecutionPanel key={categorySelected.id} script={categorySelected} />}
      </div>
    )
  }

  // mode === 'script'
  if (!flatScript) {
    return <div>{backLink}<p className="error">Задача не найдена.</p></div>
  }
  return (
    <div>
      {backLink}
      <div className="card">
        <h1>{flatScript.name}</h1>
        {flatScript.description && <p className="hint">{flatScript.description}</p>}
      </div>
      <ExecutionPanel key={flatScript.id} script={flatScript} />
    </div>
  )
}

// Общая логика группировки скриптов (задач) для страницы со списком задач
// (DashboardPage) и страницы конкретной задачи (TaskDetailPage) — чтобы
// правила группировки не могли разойтись между двумя местами.

export const ACCESS_RIGHTS_ROOT = 'Выдача прав доступа на счета для эталонных ролей'

export const ACCESS_RIGHTS_CATEGORIES = new Set([
  'ГРО (Платежи в рублях)',
  'ГУП (Платежи без импорта, этап 2)',
  'Права просмотра на все счета',
  'Валютный контроль (Рубли)',
  'Валютный контроль (Валюта)',
  'SAP-PI',
])

export function groupScripts(scripts) {
  const categorized = scripts.filter((s) => s.category)
  const flat = scripts.filter((s) => !s.category)

  const accessRightsScripts = categorized.filter((s) => ACCESS_RIGHTS_CATEGORIES.has(s.category))
  const otherCategorized = categorized.filter((s) => !ACCESS_RIGHTS_CATEGORIES.has(s.category))

  const accessRightsCategoryList = []
  for (const s of accessRightsScripts) {
    if (!accessRightsCategoryList.includes(s.category)) accessRightsCategoryList.push(s.category)
  }

  const otherCategoryMap = new Map()
  for (const s of otherCategorized) {
    if (!otherCategoryMap.has(s.category)) otherCategoryMap.set(s.category, [])
    otherCategoryMap.get(s.category).push(s)
  }
  const otherCategoryGroups = Array.from(otherCategoryMap.entries())

  return { accessRightsScripts, accessRightsCategoryList, otherCategoryGroups, flat }
}

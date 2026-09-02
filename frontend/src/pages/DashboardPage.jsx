import React, { useMemo } from 'react'
import { Link } from 'react-router-dom'
import useScripts from '../hooks/useScripts.js'
import { ACCESS_RIGHTS_ROOT, groupScripts } from '../taskGroups.js'

export default function DashboardPage() {
  const { scripts, loading, loadError } = useScripts()
  const { accessRightsScripts, accessRightsCategoryList, otherCategoryGroups, flat } = useMemo(
    () => groupScripts(scripts),
    [scripts]
  )

  const tiles = useMemo(() => {
    const list = []
    if (accessRightsScripts.length > 0) {
      list.push({
        key: 'access-rights',
        to: '/tasks/access-rights',
        title: ACCESS_RIGHTS_ROOT,
        hint: `${accessRightsCategoryList.length} эталонных ролей`,
      })
    }
    for (const [cat, catScripts] of otherCategoryGroups) {
      list.push({
        key: `category:${cat}`,
        to: `/tasks/category/${encodeURIComponent(cat)}`,
        title: cat,
        hint: catScripts.length === 1 ? (catScripts[0].description || '') : `${catScripts.length} вариантов`,
      })
    }
    for (const s of flat) {
      list.push({
        key: `script:${s.id}`,
        to: `/tasks/script/${s.id}`,
        title: s.name,
        hint: s.description || '',
      })
    }
    return list
  }, [accessRightsScripts, accessRightsCategoryList, otherCategoryGroups, flat])

  return (
    <div>
      <h1>Доступные задачи</h1>
      {loading && <p>Загрузка...</p>}
      {!loading && loadError && <p className="error">{loadError}</p>}
      {!loading && !loadError && tiles.length === 0 && <p>Нет доступных задач.</p>}

      {!loading && !loadError && tiles.length > 0 && (
        <div className="task-grid">
          {tiles.map((t) => (
            <Link className="task-tile" key={t.key} to={t.to}>
              <h2>{t.title}</h2>
              {t.hint && <p className="hint">{t.hint}</p>}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

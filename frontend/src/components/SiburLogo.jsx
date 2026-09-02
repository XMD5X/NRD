import React from 'react'

/**
 * Простой фирменный знак (не копия официального логотипа СИБУРа — тот защищён и
 * его нельзя видоизменять, см. guide.sibur.ru/materials). Две пересекающиеся
 * фигуры — круглая и остроугольная — согласно языку паттернов бренда символизируют
 * партнёрство (circle+angle = диалог), в мятном и ярко-оранжевом фирменных цветах.
 */
export default function SiburLogo({ size = 32 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <circle cx="13" cy="16" r="11" fill="var(--sibur-mint)" />
      <path d="M17 5 L28 11 L28 21 L17 27 Z" fill="var(--sibur-orange)" opacity="0.92" />
    </svg>
  )
}

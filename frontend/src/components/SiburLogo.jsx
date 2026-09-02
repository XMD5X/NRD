import React from 'react'
import logo from '../assets/sibur-logo.png'

/**
 * Официальный логотип СИБУР — предоставлен пользователем как файл из
 * фирменного брендбука (цветной вариант, DNA teal #008C95, прозрачный фон).
 * Согласно брендбуку вносить изменения в логотип не допускается, поэтому
 * здесь только пропорциональное масштабирование по высоте — без искажений,
 * обрезки или смены цвета.
 */
export default function SiburLogo({ height = 26, className }) {
  return (
    <img
      src={logo}
      alt="СИБУР"
      className={className}
      style={{ display: 'block', height, width: 'auto' }}
    />
  )
}

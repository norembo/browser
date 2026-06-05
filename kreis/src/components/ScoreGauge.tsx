'use client'

import { useEffect, useRef } from 'react'

interface Props {
  score: number   // 0–100
  size?: number   // px, default 180
}

// Map score → colour stop
function scoreColor(score: number): string {
  if (score >= 76) return '#22c55e'   // green-500
  if (score >= 51) return '#eab308'   // yellow-500
  if (score >= 31) return '#f97316'   // orange-500
  return '#ef4444'                    // red-500
}

function scoreLabel(score: number): string {
  if (score >= 76) return 'Healthy'
  if (score >= 51) return 'Needs Work'
  if (score >= 31) return 'At Risk'
  return 'Critical'
}

export function ScoreGauge({ score, size = 180 }: Props) {
  const circleRef = useRef<SVGCircleElement>(null)

  const R            = 54
  const circumference = 2 * Math.PI * R          // ≈339.29
  const clampedScore  = Math.max(0, Math.min(100, score))
  const offset        = circumference * (1 - clampedScore / 100)
  const color         = scoreColor(clampedScore)
  const label         = scoreLabel(clampedScore)

  useEffect(() => {
    const el = circleRef.current
    if (!el) return

    // Animate the stroke from full offset (0%) to the target offset
    el.style.strokeDashoffset = String(circumference)
    requestAnimationFrame(() => {
      el.style.transition     = 'stroke-dashoffset 1.2s cubic-bezier(0.34, 1.56, 0.64, 1)'
      el.style.strokeDashoffset = String(offset)
    })
  }, [score, offset, circumference])

  return (
    <div className="flex flex-col items-center gap-2 animate-score-in">
      <svg
        width={size}
        height={size}
        viewBox="0 0 120 120"
        aria-label={`Conversion health score: ${clampedScore} out of 100`}
      >
        {/* Track ring */}
        <circle
          cx="60" cy="60" r={R}
          fill="none"
          stroke="#ffffff10"
          strokeWidth="10"
        />
        {/* Progress ring */}
        <circle
          ref={circleRef}
          cx="60" cy="60" r={R}
          fill="none"
          stroke={color}
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference}
          transform="rotate(-90 60 60)"
          style={{ filter: `drop-shadow(0 0 8px ${color}66)` }}
        />
        {/* Score number */}
        <text
          x="60" y="55"
          textAnchor="middle"
          dominantBaseline="middle"
          fontSize="24"
          fontWeight="700"
          fill={color}
          fontFamily="Inter var, Inter, system-ui"
        >
          {clampedScore}
        </text>
        {/* /100 */}
        <text
          x="60" y="73"
          textAnchor="middle"
          fontSize="10"
          fill="#94a3b8"
          fontFamily="Inter var, Inter, system-ui"
        >
          / 100
        </text>
      </svg>

      <div className="text-center">
        <span
          className="text-sm font-semibold tracking-wide px-3 py-1 rounded-full"
          style={{ color, backgroundColor: `${color}22` }}
        >
          {label}
        </span>
      </div>
    </div>
  )
}

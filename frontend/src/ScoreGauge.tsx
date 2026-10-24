type Props = { score: number }

export default function ScoreGauge({ score }: Props) {
  const radius = 58
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (Math.min(Math.max(score, 0), 100) / 100) * circumference
  const color = score >= 75 ? '#1c6a54' : score >= 50 ? '#a35d12' : '#9a3a2a'

  return (
    <svg className="gauge" viewBox="0 0 148 148" role="img" aria-label={`Score ${score}`}>
      <circle cx="74" cy="74" r={radius} fill="none" stroke="#e5dccf" strokeWidth="10" />
      <circle
        cx="74"
        cy="74"
        r={radius}
        fill="none"
        stroke={color}
        strokeWidth="10"
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        transform="rotate(-90 74 74)"
      />
      <text x="74" y="80" textAnchor="middle" fontSize="36" fill="#1a1612">
        {score}
      </text>
      <text x="74" y="102" textAnchor="middle" fontSize="11" fill="#6c6358">
        / 100
      </text>
    </svg>
  )
}

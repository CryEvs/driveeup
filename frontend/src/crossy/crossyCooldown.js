const KEY = 'driveeup_cross_road_cooldown_until'
const DURATION_MS = 60 * 1000

export function getCrossRoadCooldownRemaining() {
  const until = Number(localStorage.getItem(KEY) || 0)
  return Math.max(0, until - Date.now())
}

export function startCrossRoadCooldown() {
  localStorage.setItem(KEY, String(Date.now() + DURATION_MS))
}

export function formatCooldown(ms) {
  const s = Math.ceil(ms / 1000)
  const m = Math.floor(s / 60)
  const r = s % 60
  return m > 0 ? `${m} мин ${r} с` : `${r} с`
}

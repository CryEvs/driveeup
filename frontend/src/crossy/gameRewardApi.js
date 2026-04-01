const API = '/api'

export async function claimDriveCoin(lanesPassed, token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${API}/game/claim-drive-coin`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ lanesPassed }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.error || 'Не удалось начислить')
  }
  return res.json()
}

import { PLAYER_R, ROW_TYPES, TILE } from './constants.js'

const CAR_WRAP = 28

export function updateCars(row, dt) {
  if (row.type !== ROW_TYPES.ROAD || !row.cars) return
  for (const car of row.cars) {
    car.x += car.vx * dt * 60
    if (car.x > CAR_WRAP) car.x = -CAR_WRAP
    if (car.x < -CAR_WRAP) car.x = CAR_WRAP
  }
}

export function updateRail(row, dt) {
  if (row.type !== ROW_TYPES.RAIL || !row.rail) return
  const r = row.rail
  r.t = (r.t || 0) + dt
  const c = r.t % r.period
  r.warning = c < r.warn
  r.trainActive = c >= r.warn && c < r.warn + r.trainDur
  if (r.trainActive) {
    const u = (c - r.warn) / r.trainDur
    r.trainX = -22 + u * 44
  } else {
    r.trainX = -999
  }
}

export function hitsCar(row, px) {
  if (row.type !== ROW_TYPES.ROAD) return false
  for (const car of row.cars || []) {
    if (Math.abs(car.x - px) < car.halfW + PLAYER_R) return true
  }
  return false
}

export function hitsTrain(row, px) {
  if (row.type !== ROW_TYPES.RAIL || !row.rail || !row.rail.trainActive) return false
  const tx = row.rail.trainX
  const half = row.rail.trainHalfLen
  return Math.abs(tx - px) < half + PLAYER_R
}

export function worldZForRow(index) {
  return index * TILE
}

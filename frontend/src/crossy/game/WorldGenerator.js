import { ROW_TYPES } from './constants.js'

/** Должен совпадать с CAR_WRAP в ObstacleManager (кольцо дороги по X) */
const CAR_WRAP = 28
const TRACK_LEN = CAR_WRAP * 2

function randBetween(a, b) {
  return a + Math.random() * (b - a)
}

export function createGenState() {
  return {
    roadsRemaining: 0,
    lastType: ROW_TYPES.GRASS,
    /** После завершённой серии дорог для машин — следующая полоса только трава */
    requireGrassNext: false,
  }
}

function generateRoadCars() {
  const n = 3 + Math.floor(Math.random() * 4)
  const dir = Math.random() < 0.5 ? -1 : 1
  const speed = randBetween(0.06, 0.14)
  const vx = dir * speed
  const halfW = 1.12
  const halfZ = 0.58
  const cars = []
  const spacing = TRACK_LEN / n
  const phase = Math.random() * TRACK_LEN
  for (let i = 0; i < n; i++) {
    const t = (phase + i * spacing) % TRACK_LEN
    const x = t - CAR_WRAP
    cars.push({
      x,
      vx,
      halfW,
      halfZ,
      color: 0xffffff,
    })
  }
  return cars
}

function railPayload() {
  const period = 4 + Math.random() * 2
  return {
    phase: Math.random() * period,
    period,
    warn: 1.4,
    trainDur: 0.88,
    trainHalfLen: 2.4,
    vx: 0.28,
    t: 0,
  }
}

/**
 * Старт — одна трава. Подряд только дорог (машины): 1–4, затем обязательно трава.
 * Ж/д и дороги чередуются как угодно после травы (не считаются «дорогой для машин»).
 */
export function generateRow(index, gen) {
  if (index < 1) {
    gen.lastType = ROW_TYPES.GRASS
    return { type: ROW_TYPES.GRASS, index, cars: [], logs: [], rail: null }
  }

  if (gen.requireGrassNext) {
    gen.requireGrassNext = false
    gen.lastType = ROW_TYPES.GRASS
    return { type: ROW_TYPES.GRASS, index, cars: [], logs: [], rail: null }
  }

  if (gen.roadsRemaining > 0) {
    gen.roadsRemaining -= 1
    gen.lastType = ROW_TYPES.ROAD
    if (gen.roadsRemaining === 0) {
      gen.requireGrassNext = true
    }
    return {
      type: ROW_TYPES.ROAD,
      index,
      cars: generateRoadCars(),
      logs: [],
      rail: null,
    }
  }

  const prev = gen.lastType
  const canGrass = prev !== ROW_TYPES.GRASS

  let type = ROW_TYPES.ROAD
  const roll = Math.random()

  if (!canGrass) {
    if (roll < 0.48) {
      type = ROW_TYPES.ROAD
      const streak = 1 + Math.floor(Math.random() * 4)
      gen.roadsRemaining = streak - 1
      if (gen.roadsRemaining === 0) gen.requireGrassNext = true
    } else {
      type = ROW_TYPES.RAIL
    }
  } else {
    if (roll < 0.16) {
      type = ROW_TYPES.GRASS
    } else if (roll < 0.55) {
      type = ROW_TYPES.ROAD
      const streak = 1 + Math.floor(Math.random() * 4)
      gen.roadsRemaining = streak - 1
      if (gen.roadsRemaining === 0) gen.requireGrassNext = true
    } else {
      type = ROW_TYPES.RAIL
    }
  }

  gen.lastType = type

  if (type === ROW_TYPES.GRASS) {
    return { type, index, cars: [], logs: [], rail: null }
  }
  if (type === ROW_TYPES.ROAD) {
    return { type, index, cars: generateRoadCars(), logs: [], rail: null }
  }
  return {
    type: ROW_TYPES.RAIL,
    index,
    cars: [],
    logs: [],
    rail: railPayload(),
  }
}

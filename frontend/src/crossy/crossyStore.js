import { create } from 'zustand'

function previewCoin(lanesPassed) {
  return Math.round(lanesPassed * 0.5 * 10) / 10
}

export const useCrossyStore = create((set) => ({
  status: 'idle',
  lanesPassed: 0,
  driveCoinPreview: 0,

  setStatus: (status) => set({ status }),
  resetMeta: () =>
    set({
      status: 'running',
      lanesPassed: 0,
      driveCoinPreview: 0,
    }),

  /** Очки только после проигрыша: 0.5 за каждую пройденную полосу */
  gameOver: (lanesPassed) =>
    set({
      status: 'gameOver',
      lanesPassed,
      driveCoinPreview: previewCoin(lanesPassed),
    }),
}))

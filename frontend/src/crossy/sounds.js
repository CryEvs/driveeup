let ctx

function audio() {
  if (!ctx) ctx = new AudioContext()
  return ctx
}

export function playJump() {
  try {
    const c = audio()
    const o = c.createOscillator()
    const g = c.createGain()
    o.type = 'sine'
    o.frequency.value = 520
    g.gain.setValueAtTime(0.12, c.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.08)
    o.connect(g)
    g.connect(c.destination)
    o.start()
    o.stop(c.currentTime + 0.09)
  } catch {
    /* ignore */
  }
}

export function playHit() {
  try {
    const c = audio()
    const o = c.createOscillator()
    const g = c.createGain()
    o.type = 'sawtooth'
    o.frequency.value = 120
    g.gain.setValueAtTime(0.15, c.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.25)
    o.connect(g)
    g.connect(c.destination)
    o.start()
    o.stop(c.currentTime + 0.26)
  } catch {
    /* ignore */
  }
}

let musicInterval
export function startMusic() {
  stopMusic()
  musicInterval = setInterval(() => {
    try {
      const c = audio()
      const o = c.createOscillator()
      const g = c.createGain()
      o.type = 'triangle'
      o.frequency.value = 130 + Math.sin(Date.now() * 0.001) * 8
      g.gain.setValueAtTime(0.02, c.currentTime)
      g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + 0.4)
      o.connect(g)
      g.connect(c.destination)
      o.start()
      o.stop(c.currentTime + 0.42)
    } catch {
      /* ignore */
    }
  }, 480)
}

export function stopMusic() {
  if (musicInterval) {
    clearInterval(musicInterval)
    musicInterval = null
  }
}

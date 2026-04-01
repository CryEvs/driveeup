import { useEffect, useRef } from 'react'
import { GameEngine } from './game/GameEngine'
import { useCrossyStore } from './crossyStore'

export function GameCanvas({ onReady }) {
  const containerRef = useRef(null)
  const engineRef = useRef(null)
  const onReadyRef = useRef(onReady)
  onReadyRef.current = onReady

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const engine = new GameEngine(el, useCrossyStore)
    engineRef.current = engine
    let cancelled = false
    engine.start().then(() => {
      if (cancelled) {
        engine.dispose()
        return
      }
      onReadyRef.current?.()
    })
    return () => {
      cancelled = true
      engine.dispose()
      engineRef.current = null
    }
  }, [])

  return <div ref={containerRef} className="crossy-game__canvas" />
}

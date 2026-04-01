import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const LANE_WIDTH = 1.15
const SIDEWALK_LANE = 3
const CAR_LANES_LEFT = [0, 1, 2]
const CAR_LANES_RIGHT = [4, 5, 6]
const START_Z = -5.5
const FINISH_Z = 5.8
const STEP_Z = 0.75
const PLAYER_R = 0.38
const CAR_HALF_X = 0.55
const CAR_HALF_Z = 0.35

function laneCenterX(lane) {
  return (lane - SIDEWALK_LANE) * LANE_WIDTH
}

const HUMAN_GLB =
  'https://cdn.jsdelivr.net/gh/KhronosGroup/glTF-Sample-Models@master/2.0/CesiumMan/glTF-Binary/CesiumMan.glb'

/**
 * @param {{ embedded?: boolean, onGameOver: (detail: { won: boolean; score: number }) => void }} props
 */
export function CrossRoadGame({ embedded = false, onGameOver }) {
  const containerRef = useRef(null)
  const hudScoreRef = useRef(null)
  const hudHintRef = useRef(null)
  const hudEndRef = useRef(null)
  const onGameOverRef = useRef(onGameOver)
  onGameOverRef.current = onGameOver

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    let width = container.clientWidth || 800
    let height = container.clientHeight || 520
    const scene = new THREE.Scene()
    scene.background = new THREE.Color(embedded ? 0x0a120a : 0x1a2818)
    scene.fog = new THREE.Fog(embedded ? 0x0a120a : 0x1a2818, 25, 55)

    const camera = new THREE.PerspectiveCamera(48, width / height, 0.1, 120)
    camera.position.set(0, 11.5, 13.2)
    camera.lookAt(0, 0, 0.5)

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    renderer.setSize(width, height)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    renderer.shadowMap.enabled = true
    renderer.shadowMap.type = THREE.PCFSoftShadowMap
    container.appendChild(renderer.domElement)

    const hemi = new THREE.HemisphereLight(0xb8e0ff, 0x3d5a28, 0.85)
    scene.add(hemi)
    const dir = new THREE.DirectionalLight(0xffffff, 1.05)
    dir.position.set(8, 18, 10)
    dir.castShadow = true
    dir.shadow.mapSize.set(2048, 2048)
    scene.add(dir)

    const ground = new THREE.Mesh(
      new THREE.PlaneGeometry(40, 40),
      new THREE.MeshStandardMaterial({ color: 0x2a3d24, roughness: 0.9 })
    )
    ground.rotation.x = -Math.PI / 2
    ground.position.y = -0.02
    ground.receiveShadow = true
    scene.add(ground)

    const roadGroup = new THREE.Group()
    scene.add(roadGroup)

    const roadMat = new THREE.MeshStandardMaterial({ color: 0x3a3a3a, roughness: 0.75 })
    const lineMat = new THREE.MeshBasicMaterial({ color: 0xf5f5a0 })
    for (let i = 0; i < 7; i++) {
      const cx = laneCenterX(i)
      const isWalk = i === SIDEWALK_LANE
      const w = LANE_WIDTH - 0.04
      const mesh = new THREE.Mesh(new THREE.PlaneGeometry(w, 22), roadMat.clone())
      mesh.rotation.x = -Math.PI / 2
      mesh.position.set(cx, 0.01, 0)
      mesh.receiveShadow = true
      roadGroup.add(mesh)
      if (isWalk) {
        mesh.material = new THREE.MeshStandardMaterial({ color: 0x6b7a5c, roughness: 0.85 })
      } else {
        for (let z = -10; z <= 10; z += 2.2) {
          const line = new THREE.Mesh(new THREE.PlaneGeometry(0.06, 0.35), lineMat)
          line.rotation.x = -Math.PI / 2
          line.position.set(cx, 0.02, z)
          roadGroup.add(line)
        }
      }
    }

    const borderMat = new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: 0x223311 })
    const finishLine = new THREE.Mesh(new THREE.PlaneGeometry(LANE_WIDTH * 7.2, 0.25), borderMat)
    finishLine.rotation.x = -Math.PI / 2
    finishLine.position.set(0, 0.03, FINISH_Z)
    scene.add(finishLine)

    const startLine = new THREE.Mesh(new THREE.PlaneGeometry(LANE_WIDTH * 7.2, 0.2), borderMat)
    startLine.rotation.x = -Math.PI / 2
    startLine.position.set(0, 0.03, START_Z)
    scene.add(startLine)

    const loader = new GLTFLoader()
    const cars = []
    let taxiTemplate = null
    let playerRoot = null
    let gameEnded = false
    let score = 0
    let steps = 0

    const player = {
      lane: SIDEWALK_LANE,
      z: START_Z,
      mesh: null
    }

    function hudTick() {
      const el = hudScoreRef.current
      if (el) el.textContent = `Очки: ${score} · Шаги: ${steps}`
    }

    function hudEnd(won) {
      const el = hudEndRef.current
      if (el) {
        el.style.display = 'block'
        el.textContent = won ? 'Победа! Финиш!' : 'Конец игры!'
      }
      const hint = hudHintRef.current
      if (hint) hint.style.display = 'none'
    }

    function endGame(won) {
      if (gameEnded) return
      gameEnded = true
      const finalScore = won ? score + 100 + steps : Math.max(0, score + steps - 5)
      hudEnd(won)
      hudTick()
      onGameOverRef.current?.({ won, score: finalScore })
    }

    function spawnCar() {
      const pickLeft = Math.random() < 0.5
      const pool = pickLeft ? CAR_LANES_LEFT : CAR_LANES_RIGHT
      const lane = pool[Math.floor(Math.random() * pool.length)]
      const vx = pickLeft ? -(0.12 + Math.random() * 0.13) : 0.12 + Math.random() * 0.13
      const z = player.z + (Math.random() * 16 - 1)
      const startX = vx < 0 ? 11 : -11

      let mesh
      if (taxiTemplate) {
        mesh = taxiTemplate.clone(true)
        mesh.traverse((c) => {
          if (c.isMesh) {
            c.castShadow = true
            c.receiveShadow = true
          }
        })
      } else {
        mesh = new THREE.Group()
        const box = new THREE.Mesh(
          new THREE.BoxGeometry(1.05, 0.5, 0.62),
          new THREE.MeshStandardMaterial({ color: pickLeft ? 0xffcc22 : 0x44aaff, metalness: 0.2 })
        )
        box.castShadow = true
        mesh.add(box)
      }

      const s = 0.42
      mesh.scale.set(s, s, s)
      mesh.rotation.y = vx < 0 ? Math.PI / 2 : -Math.PI / 2
      mesh.position.set(startX, 0.32, z)
      scene.add(mesh)
      cars.push({ mesh, lane, vx })
    }

    loader.load(
      '/models/cartoon_taxi_low_poli.glb',
      (gltf) => {
        taxiTemplate = gltf.scene
      },
      undefined,
      () => {}
    )

    loader.load(
      HUMAN_GLB,
      (gltf) => {
        const root = gltf.scene
        root.traverse((c) => {
          if (c.isMesh) {
            c.castShadow = true
          }
        })
        const box = new THREE.Box3().setFromObject(root)
        const size = new THREE.Vector3()
        box.getSize(size)
        const scale = 0.85 / Math.max(size.x, size.y, size.z)
        root.scale.setScalar(scale)
        root.position.set(laneCenterX(player.lane), 0, player.z)
        root.rotation.y = Math.PI
        playerRoot = root
        player.mesh = root
        scene.add(root)
        const box2 = new THREE.Box3().setFromObject(root)
        root.position.y = -box2.min.y
      },
      undefined,
      () => {
        const geom = new THREE.CapsuleGeometry(0.28, 0.55, 4, 8)
        const mat = new THREE.MeshStandardMaterial({ color: 0x4a90d9 })
        const mesh = new THREE.Mesh(geom, mat)
        mesh.castShadow = true
        mesh.position.set(laneCenterX(player.lane), 0.55, player.z)
        player.mesh = mesh
        playerRoot = mesh
        scene.add(mesh)
      }
    )

    let spawnAcc = 0
    let lastTime = performance.now()

    function tryCollisions() {
      if (gameEnded) return
      const px = laneCenterX(player.lane)
      const pz = player.z
      for (const c of cars) {
        if (c.lane !== player.lane) continue
        const cx = c.mesh.position.x
        const cz = c.mesh.position.z
        const dx = Math.abs(cx - px)
        const dz = Math.abs(cz - pz)
        if (dx < CAR_HALF_X + PLAYER_R && dz < CAR_HALF_Z + PLAYER_R) {
          endGame(false)
          return
        }
      }
    }

    function updatePlayerVisual() {
      if (!player.mesh) return
      const x = laneCenterX(player.lane)
      if (playerRoot && playerRoot.isGroup) {
        player.mesh.position.x = x
        player.mesh.position.z = player.z
      } else {
        player.mesh.position.set(x, 0.55, player.z)
      }
    }

    function stepForward() {
      if (gameEnded) return
      player.z += STEP_Z
      steps += 1
      score += 2
      hudTick()
      updatePlayerVisual()
      if (player.z >= FINISH_Z) {
        endGame(true)
      } else tryCollisions()
    }

    function stepLane(delta) {
      if (gameEnded) return
      player.lane = Math.max(0, Math.min(6, player.lane + delta))
      updatePlayerVisual()
      tryCollisions()
    }

    function onKeyDown(e) {
      if (gameEnded) return
      if (e.code === 'Space' || e.code === 'ArrowUp') {
        e.preventDefault()
        stepForward()
      } else if (e.code === 'ArrowLeft' || e.code === 'KeyA') {
        e.preventDefault()
        stepLane(-1)
      } else if (e.code === 'ArrowRight' || e.code === 'KeyD') {
        e.preventDefault()
        stepLane(1)
      }
    }

    function onPointer(e) {
      if (gameEnded) return
      const rect = renderer.domElement.getBoundingClientRect()
      const clientX = e.changedTouches ? e.changedTouches[0].clientX : e.clientX
      const tx = clientX - rect.left
      const third = rect.width / 3
      if (tx < third) stepLane(-1)
      else if (tx > third * 2) stepLane(1)
      else stepForward()
      e.preventDefault()
    }

    window.addEventListener('keydown', onKeyDown)
    const canvas = renderer.domElement
    canvas.addEventListener('touchstart', onPointer, { passive: false })
    function onClick(e) {
      if (e.pointerType === 'touch') return
      onPointer(e)
    }
    canvas.addEventListener('click', onClick)

    const onResize = () => {
      width = container.clientWidth || 800
      height = container.clientHeight || 520
      camera.aspect = width / height
      camera.updateProjectionMatrix()
      renderer.setSize(width, height)
    }
    window.addEventListener('resize', onResize)

    let raf = 0
    function animate(now) {
      raf = requestAnimationFrame(animate)
      const dt = Math.min(0.05, (now - lastTime) / 1000)
      lastTime = now

      if (!gameEnded) {
        spawnAcc += dt
        if (spawnAcc > 0.5 + Math.random() * 0.35) {
          spawnAcc = 0
          if (cars.length < 40) spawnCar()
        }

        for (let i = cars.length - 1; i >= 0; i--) {
          const c = cars[i]
          c.mesh.position.x += c.vx * (dt * 60)
          if (c.mesh.position.x > 14 || c.mesh.position.x < -14) {
            scene.remove(c.mesh)
            cars.splice(i, 1)
          }
        }
        tryCollisions()

        const targetX = laneCenterX(player.lane)
        camera.position.x += (targetX * 0.35 - camera.position.x) * 0.08
        camera.lookAt(targetX * 0.2, 0, player.z * 0.15 + 0.5)
      }

      renderer.render(scene, camera)
    }
    raf = requestAnimationFrame(animate)
    hudTick()

    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('resize', onResize)
      canvas.removeEventListener('touchstart', onPointer)
      canvas.removeEventListener('click', onClick)
      if (container.contains(canvas)) container.removeChild(canvas)
      renderer.dispose()
    }
  }, [embedded])

  return (
    <div className={`crossroad-wrap${embedded ? ' crossroad-wrap--embed' : ''}`}>
      <div ref={containerRef} className="crossroad-canvas" />
      <div className="crossroad-hud">
        <div ref={hudScoreRef} className="crossroad-hud__score" />
        <div ref={hudHintRef} className="crossroad-hud__hint">
          Пробел / тап по центру — вперёд · ← → или края экрана — смена полосы
        </div>
        <div ref={hudEndRef} className="crossroad-hud__end" style={{ display: 'none' }} />
      </div>
    </div>
  )
}

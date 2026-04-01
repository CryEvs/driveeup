import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { TILE, ROW_TYPES, ROWS_AHEAD, ROWS_BEHIND } from './constants.js'
import { createGenState, generateRow } from './WorldGenerator.js'
import { createPlayerMesh } from './Player.js'
import { updateCars, updateRail, hitsCar, hitsTrain, worldZForRow } from './ObstacleManager.js'
import { playHit, playJump, startMusic, stopMusic } from '../sounds.js'

const GROUND_W = 52
const TAXI_URL = '/models/cartoon_taxi_low_poli.glb'
/** Модель GLB смотрит «задом» относительно ожидаемого направления движения */
const TAXI_YAW_FIX = Math.PI
export class GameEngine {
  constructor(container, store) {
    this.container = container
    this.store = store
    this.rows = new Map()
    this.rowGroups = new Map()
    this.carMeshes = new Map()
    this.trainMeshes = new Map()
    this.taxiTemplate = null
    this.genState = createGenState()

    this.playerRow = 0
    this.playerX = 0
    this.running = false
    this.raf = 0
    this.lastT = performance.now()

    this.scene = new THREE.Scene()
    this.scene.background = new THREE.Color(0x7ec8e8)
    this.scene.fog = new THREE.Fog(0xa8daf5, 70, 240)

    const w = container.clientWidth || 800
    const h = container.clientHeight || 600
    this.camera = new THREE.PerspectiveCamera(36, w / h, 0.1, 320)
    this.camera.position.set(0, 7.2, -10.8)

    this.renderer = new THREE.WebGLRenderer({ antialias: true })
    this.renderer.setSize(w, h)
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.shadowMap.enabled = true
    this.renderer.shadowMap.type = THREE.PCFSoftShadowMap
    container.appendChild(this.renderer.domElement)

    const amb = new THREE.AmbientLight(0xffffff, 0.58)
    this.scene.add(amb)
    const sun = new THREE.DirectionalLight(0xfff5e6, 1.15)
    sun.position.set(16, 28, 12)
    sun.castShadow = true
    sun.shadow.mapSize.set(2048, 2048)
    sun.shadow.camera.near = 0.5
    sun.shadow.camera.far = 120
    sun.shadow.camera.left = -40
    sun.shadow.camera.right = 40
    sun.shadow.camera.top = 40
    sun.shadow.camera.bottom = -40
    this.scene.add(sun)
    const fill = new THREE.DirectionalLight(0xb8dcff, 0.35)
    fill.position.set(-12, 14, -8)
    this.scene.add(fill)

    this.player = createPlayerMesh()
    this.scene.add(this.player)

    this.resize = () => {
      const cw = container.clientWidth || 800
      const ch = container.clientHeight || 600
      this.camera.aspect = cw / ch
      this.camera.updateProjectionMatrix()
      this.renderer.setSize(cw, ch)
    }
    window.addEventListener('resize', this.resize)

    this._keydown = (e) => {
      if (e.code === 'Space' || e.code === 'ArrowUp') {
        e.preventDefault()
        this.jump()
      }
    }
    window.addEventListener('keydown', this._keydown)
    this._pointer = (e) => {
      e.preventDefault()
      this.jump()
    }
    this.renderer.domElement.addEventListener('pointerdown', this._pointer)
  }

  loadTaxi() {
    return new Promise((resolve) => {
      const loader = new GLTFLoader()
      loader.load(
        TAXI_URL,
        (gltf) => {
          const root = gltf.scene
          const box = new THREE.Box3().setFromObject(root)
          const size = new THREE.Vector3()
          box.getSize(size)
          const s = 1.28 / Math.max(size.x, size.y, size.z)
          root.scale.setScalar(s)
          root.updateMatrixWorld(true)
          const boxGround = new THREE.Box3().setFromObject(root)
          root.position.y = -boxGround.min.y
          root.traverse((c) => {
            if (c.isMesh) {
              c.castShadow = true
              c.receiveShadow = true
            }
          })
          this.taxiTemplate = root
          resolve()
        },
        undefined,
        () => resolve()
      )
    })
  }

  async start() {
    if (!this.taxiTemplate) await this.loadTaxi()
    this.resetWorld()
    this.running = true
    this.store.getState().resetMeta()
    this.store.getState().setStatus('running')
    startMusic()
    this.lastT = performance.now()
    this.raf = requestAnimationFrame(() => this.loop())
  }

  resetWorld() {
    for (const g of this.rowGroups.values()) {
      this.scene.remove(g)
      g.traverse((o) => {
        if (o.geometry) o.geometry.dispose()
        if (o.material) {
          if (Array.isArray(o.material)) o.material.forEach((m) => m.dispose())
          else o.material.dispose()
        }
      })
    }
    this.rows.clear()
    this.rowGroups.clear()
    this.carMeshes.clear()
    this.trainMeshes.clear()
    this.genState = createGenState()

    this.playerRow = 0
    this.playerX = 0
    for (let i = 0; i <= ROWS_AHEAD; i++) {
      const row = generateRow(i, this.genState)
      this.rows.set(i, row)
      this.buildRowVisual(row)
    }
    this.syncPlayerMesh()
  }

  makeCarMesh(car) {
    if (this.taxiTemplate) {
      const mesh = this.taxiTemplate.clone(true)
      mesh.rotation.y =
        (car.vx < 0 ? Math.PI / 2 : -Math.PI / 2) + TAXI_YAW_FIX
      mesh.position.set(car.x, 0, 0)
      return mesh
    }
    const mesh = new THREE.Mesh(
      new THREE.BoxGeometry(car.halfW * 2, 0.55, car.halfZ * 2),
      new THREE.MeshStandardMaterial({ color: 0xffcc33, roughness: 0.35, metalness: 0.25 })
    )
    mesh.position.set(car.x, 0.4, 0)
    mesh.castShadow = true
    return mesh
  }

  buildRowVisual(row) {
    const z = worldZForRow(row.index)
    const group = new THREE.Group()
    group.position.z = z
    this.scene.add(group)
    this.rowGroups.set(row.index, group)

    const groundMat = new THREE.MeshStandardMaterial({
      color:
        row.type === ROW_TYPES.GRASS
          ? 0x4caf50
          : row.type === ROW_TYPES.ROAD
            ? 0x3a3a3e
            : 0x353540,
      roughness: 0.88,
    })
    const ground = new THREE.Mesh(new THREE.PlaneGeometry(GROUND_W, TILE * 0.99), groundMat)
    ground.rotation.x = -Math.PI / 2
    ground.position.y = 0.01
    ground.receiveShadow = true
    group.add(ground)

    if (row.type === ROW_TYPES.GRASS) {
      for (let i = 0; i < 14; i++) {
        const tuft = new THREE.Mesh(
          new THREE.BoxGeometry(0.22 + Math.random() * 0.15, 0.35 + Math.random() * 0.2, 0.22),
          new THREE.MeshStandardMaterial({ color: 0x3d9a40, roughness: 0.9 })
        )
        tuft.position.set(
          (Math.random() - 0.5) * (GROUND_W * 0.85),
          0.18,
          (Math.random() - 0.5) * TILE * 0.7
        )
        tuft.castShadow = true
        group.add(tuft)
      }
    }

    if (row.type === ROW_TYPES.ROAD) {
      const edgeMat = new THREE.MeshStandardMaterial({ color: 0xf5f5a8, roughness: 0.6 })
      const edgeL = new THREE.Mesh(new THREE.BoxGeometry(GROUND_W, 0.06, 0.12), edgeMat)
      edgeL.position.set(0, 0.04, -TILE * 0.42)
      group.add(edgeL)
      const edgeR = new THREE.Mesh(new THREE.BoxGeometry(GROUND_W, 0.06, 0.12), edgeMat)
      edgeR.position.set(0, 0.04, TILE * 0.42)
      group.add(edgeR)
      const dashMat = new THREE.MeshBasicMaterial({ color: 0xffee88 })
      for (let lx = -24; lx <= 24; lx += 3.2) {
        const dash = new THREE.Mesh(new THREE.PlaneGeometry(1.4, 0.12), dashMat)
        dash.rotation.x = -Math.PI / 2
        dash.position.set(lx, 0.03, 0)
        group.add(dash)
      }
      const meshes = []
      row.cars.forEach((car) => {
        const mesh = this.makeCarMesh(car)
        group.add(mesh)
        meshes.push(mesh)
      })
      this.carMeshes.set(row.index, meshes)
    }

    if (row.type === ROW_TYPES.RAIL) {
      const railMat = new THREE.MeshStandardMaterial({ color: 0x777788, metalness: 0.35 })
      const railL = new THREE.Mesh(new THREE.BoxGeometry(GROUND_W, 0.1, 0.16), railMat)
      railL.position.set(0, 0.12, -0.28)
      railL.castShadow = true
      group.add(railL)
      const railR = new THREE.Mesh(new THREE.BoxGeometry(GROUND_W, 0.1, 0.16), railMat)
      railR.position.set(0, 0.12, 0.28)
      group.add(railR)
      const sleeper = new THREE.Mesh(
        new THREE.BoxGeometry(0.55, 0.05, 0.35),
        new THREE.MeshStandardMaterial({ color: 0x5c4a3a, roughness: 0.95 })
      )
      for (let sx = -24; sx <= 24; sx += 2) {
        const sl = sleeper.clone()
        sl.position.set(sx, 0.08, 0)
        group.add(sl)
      }

      const lamp = new THREE.Mesh(
        new THREE.CylinderGeometry(0.12, 0.16, 0.55, 8),
        new THREE.MeshStandardMaterial({ color: 0xcc3333, emissive: 0x220000 })
      )
      lamp.position.set(-22, 0.32, -1.4)
      group.add(lamp)
      row.rail.lampMesh = lamp

      const train = new THREE.Mesh(
        new THREE.BoxGeometry(row.rail.trainHalfLen * 2, 0.6, 1.0),
        new THREE.MeshStandardMaterial({ color: 0xcc2222, roughness: 0.32 })
      )
      train.position.set(-30, 0.42, 0)
      train.visible = false
      train.castShadow = true
      group.add(train)
      this.trainMeshes.set(row.index, train)
    }
  }

  syncPlayerMesh() {
    const z = worldZForRow(this.playerRow)
    this.player.position.set(this.playerX, 0, z)
    const px = this.playerX
    /** Камера строго сзади по X, чуть ближе к герою по Z, взгляд вперёд по полосе */
    const camBehind = 10.8
    const lookAheadZ = z + 6
    this.camera.position.lerp(new THREE.Vector3(px, 7.0, z - camBehind), 0.14)
    this.camera.lookAt(px, 0.85, lookAheadZ)
  }

  jump() {
    if (!this.running) return
    const next = this.playerRow + 1
    if (!this.rows.has(next)) {
      const r = generateRow(next, this.genState)
      this.rows.set(next, r)
      this.buildRowVisual(r)
    }

    const row = this.rows.get(next)
    playJump()

    if (row.type === ROW_TYPES.ROAD) {
      if (hitsCar(row, this.playerX)) {
        this.die()
        return
      }
    } else if (row.type === ROW_TYPES.RAIL) {
      if (hitsTrain(row, this.playerX)) {
        this.die()
        return
      }
    }

    this.playerRow = next
    this.cleanupRows()
    this.ensureAhead()
    this.syncPlayerMesh()
  }

  cleanupRows() {
    const min = this.playerRow - ROWS_BEHIND
    for (const key of [...this.rows.keys()]) {
      if (key < min) {
        const g = this.rowGroups.get(key)
        if (g) {
          this.scene.remove(g)
          g.traverse((o) => {
            if (o.geometry) o.geometry.dispose()
            if (o.material) {
              if (Array.isArray(o.material)) o.material.forEach((m) => m.dispose())
              else o.material.dispose()
            }
          })
        }
        this.rows.delete(key)
        this.rowGroups.delete(key)
        this.carMeshes.delete(key)
        this.trainMeshes.delete(key)
      }
    }
  }

  ensureAhead() {
    const need = this.playerRow + ROWS_AHEAD
    for (let i = this.playerRow; i <= need; i++) {
      if (!this.rows.has(i)) {
        const r = generateRow(i, this.genState)
        this.rows.set(i, r)
        this.buildRowVisual(r)
      }
    }
  }

  die() {
    if (!this.running) return
    this.running = false
    stopMusic()
    playHit()
    cancelAnimationFrame(this.raf)
    this.store.getState().gameOver(this.playerRow)
  }

  loop() {
    if (!this.running) return
    const now = performance.now()
    const dt = Math.min(0.033, (now - this.lastT) / 1000)
    this.lastT = now

    for (const [idx, row] of this.rows) {
      updateCars(row, dt)
      updateRail(row, dt)
      const cars = this.carMeshes.get(idx)
      if (cars && row.cars) {
        row.cars.forEach((car, i) => {
          if (cars[i]) {
            cars[i].position.x = car.x
            if (this.taxiTemplate && cars[i].rotation) {
              cars[i].rotation.y =
                (car.vx < 0 ? Math.PI / 2 : -Math.PI / 2) + TAXI_YAW_FIX
            }
          }
        })
      }
      const train = this.trainMeshes.get(idx)
      if (train && row.type === ROW_TYPES.RAIL && row.rail) {
        train.visible = row.rail.trainActive
        if (row.rail.trainActive) train.position.x = row.rail.trainX
        if (row.rail.lampMesh) {
          row.rail.lampMesh.material.emissive.setHex(row.rail.warning ? 0xff0000 : 0x220000)
        }
      }
    }

    const cur = this.rows.get(this.playerRow)
    if (cur) {
      if (cur.type === ROW_TYPES.ROAD && hitsCar(cur, this.playerX)) {
        this.die()
        return
      }
      if (cur.type === ROW_TYPES.RAIL && hitsTrain(cur, this.playerX)) {
        this.die()
        return
      }
    }

    this.syncPlayerMesh()
    this.renderer.render(this.scene, this.camera)
    this.raf = requestAnimationFrame(() => this.loop())
  }

  dispose() {
    this.running = false
    stopMusic()
    cancelAnimationFrame(this.raf)
    window.removeEventListener('resize', this.resize)
    window.removeEventListener('keydown', this._keydown)
    this.renderer.domElement.removeEventListener('pointerdown', this._pointer)
    if (this.renderer.domElement.parentNode) {
      this.renderer.domElement.parentNode.removeChild(this.renderer.domElement)
    }
    this.renderer.dispose()
  }
}

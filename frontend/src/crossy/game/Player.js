import * as THREE from 'three'

/** Low-poly «кубический» персонаж */
export function createPlayerMesh() {
  const g = new THREE.Group()
  const matBody = new THREE.MeshStandardMaterial({ color: 0x4a9eff, roughness: 0.5 })
  const matHead = new THREE.MeshStandardMaterial({ color: 0xffcc88, roughness: 0.5 })
  const matLeg = new THREE.MeshStandardMaterial({ color: 0x334466, roughness: 0.6 })

  const body = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.45, 0.45), matBody)
  body.position.y = 0.55
  body.castShadow = true
  g.add(body)

  const head = new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.35, 0.4), matHead)
  head.position.y = 0.98
  head.castShadow = true
  g.add(head)

  const legL = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.25, 0.2), matLeg)
  legL.position.set(-0.14, 0.2, 0)
  legL.castShadow = true
  g.add(legL)

  const legR = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.25, 0.2), matLeg)
  legR.position.set(0.14, 0.2, 0)
  legR.castShadow = true
  g.add(legR)

  g.position.y = 0
  return g
}

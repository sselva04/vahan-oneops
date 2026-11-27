import type { RmShape } from './types'

/**
 * Preview of the theoretical weight while typing. The server calculates the same way and is the one that counts.
 * Sizes in mm, density in g/cm3, result in kg per piece (or null while sizes are missing).
 */
export function previewPerPieceKg(
  shape: RmShape, density: number, a: number, b: number, c: number, manual: number,
): number | null {
  const ok = (...n: number[]) => n.every(x => Number.isFinite(x) && x > 0)
  const circle = (d: number) => (Math.PI / 4) * d * d
  let volume: number
  switch (shape) {
    case 'BLOCK': if (!ok(a, b, c)) return null; volume = a * b * c; break
    case 'ROUND': if (!ok(a, b)) return null; volume = circle(a) * b; break
    case 'TUBE': if (!ok(a, b, c) || b >= a) return null; volume = (circle(a) - circle(b)) * c; break
    case 'MANUAL': return ok(manual) ? manual : null
  }
  return (volume * density) / 1_000_000
}

export const SHAPE_LABEL: Record<RmShape, string> = {
  BLOCK: 'Block (L × W × T)',
  ROUND: 'Round (Ø × length)',
  TUBE: 'Tube (outer Ø, inner Ø, length)',
  MANUAL: 'Type the weight',
}

export function sizeText(shape: RmShape, a: number | null, b: number | null, c: number | null, manual: number | null): string {
  switch (shape) {
    case 'BLOCK': return `${a} × ${b} × ${c} mm`
    case 'ROUND': return `Ø${a} × ${b} mm`
    case 'TUBE': return `Ø${a} / Ø${b} × ${c} mm`
    case 'MANUAL': return `${manual} kg per piece (typed)`
  }
}

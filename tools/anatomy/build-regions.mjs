#!/usr/bin/env node
// Gera app/src/main/assets/anatomy/body_regions_v1.json a partir do vulovix/body-muscles.
// One-off: a saida e commitada e este script NAO faz parte do build do Gradle.
// Rode com: node tools/anatomy/build-regions.mjs

import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

const SOURCE_REPO = 'https://github.com/vulovix/body-muscles'
const SOURCE_COMMIT = '15c8085ee97cb94c51f92c75224416b15f955b5c'
const RAW = `https://raw.githubusercontent.com/vulovix/body-muscles/${SOURCE_COMMIT}/src/data`
const OUT = resolve(import.meta.dirname, '../../app/src/main/assets/anatomy/body_regions_v1.json')

// Um bloco por regiao. Parsing por bloco em vez de uma regex monolitica: varias
// entradas upstream tem comentarios "//" entre os campos.
const BLOCK = /\{\s*id:\s*"[^"]+"[\s\S]*?\n  \}/g
const FIELD = (name) => new RegExp(`(?:^|\\n)\\s*${name}:\\s*(?:ViewSide\\.)?"?([^"\\n,]+)"?`)

async function parse(view) {
  const response = await fetch(`${RAW}/muscles.${view}.ts`)
  if (!response.ok) throw new Error(`${view}: HTTP ${response.status}`)

  const source = await response.text()
  const blocks = source.match(BLOCK) ?? []
  const declared = (source.match(/\n\s*id:\s*"/g) ?? []).length

  if (blocks.length !== declared) {
    throw new Error(`${view}: ${blocks.length} blocos para ${declared} ids — o formato upstream mudou`)
  }

  return blocks.map((block) => {
    const id = block.match(FIELD('id'))?.[1]
    const name = block.match(/\n\s*name:\s*"([^"]+)"/)?.[1]
    const side = block.match(/\n\s*view:\s*ViewSide\.(FRONT|BACK)/)?.[1]
    const d = block.match(/\n\s*path:\s*"([^"]+)"/)?.[1]

    if (!id || !name || !side || !d) {
      throw new Error(`${view}: bloco incompleto (id=${id} name=${!!name} view=${side} path=${!!d})`)
    }

    if (side.toLowerCase() !== view) throw new Error(`${id}: view ${side} no arquivo ${view}`)

    return { id, name, view, d }
  })
}

const regions = [...(await parse('front')), ...(await parse('back'))]
const ids = new Set()
for (const region of regions) {
  if (ids.has(region.id)) throw new Error(`id duplicado: ${region.id}`)
  if (!/^[mM]/.test(region.d)) throw new Error(`${region.id}: path nao comeca com moveto`)
  ids.add(region.id)
}

const payload = {
  schemaVersion: 1,
  source: SOURCE_REPO,
  sourceCommit: SOURCE_COMMIT,
  license: 'Apache-2.0',
  modifications:
    'muscles.front.ts e muscles.back.ts reserializados para JSON. ' +
    'Nenhuma coordenada, id ou nome foi alterado.',
  regions,
}

mkdirSync(dirname(OUT), { recursive: true })
writeFileSync(OUT, `${JSON.stringify(payload, null, 2)}\n`)

const counts = regions.reduce((acc, r) => ({ ...acc, [r.view]: (acc[r.view] ?? 0) + 1 }), {})
console.log(`front=${counts.front} back=${counts.back} total=${regions.length}`)
console.log(`gravado em ${OUT}`)

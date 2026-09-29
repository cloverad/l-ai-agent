/**
 * 解析助手回复：带标记的 media_gallery、残缺 JSON 条目、Markdown 图片、表格、纯文本。
 * 模型常截断 JSON，前端需从碎片中恢复图片卡片，并清掉裸 JSON。
 */

export type MediaItem = {
  thumb?: string
  url: string
  credit?: string
}

export type MessagePart =
  | { type: 'text'; text: string }
  | { type: 'image'; url: string; alt: string }
  | { type: 'gallery'; items: MediaItem[]; note?: string }
  | { type: 'table'; headers: string[]; rows: string[][] }

const MD_IMAGE = /!\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/g
const BARE_IMAGE_LINE =
  /^(https?:\/\/\S+\.(?:jpg|jpeg|png|webp|gif)(?:\?\S*)?)$/i
const PEXELS_LINE = /^(https?:\/\/(?:images\.)?pexels\.com\/\S+)$/i

const MARKER_BLOCK =
  /<<<MEDIA_GALLERY>>>\s*([\s\S]*?)\s*<<<END_MEDIA_GALLERY>>>/g

/** 单条媒体对象（字段顺序可乱，允许残缺外层） */
const MEDIA_ITEM_OBJ =
  /\{\s*(?:"[^"]+"\s*:\s*"[^"]*"\s*,\s*)*"url"\s*:\s*"(https?:\/\/[^"]+)"\s*(?:,\s*"[^"]+"\s*:\s*"[^"]*")*\s*\}/g

const PEXELS_URL =
  /https?:\/\/(?:images\.)?pexels\.com\/photos\/[^\s"'<>\\]+/gi

export function parseMessageParts(content: string): MessagePart[] {
  if (!content) return []

  let working = content
  const galleries: MessagePart[] = []

  // 1) 官方标记块
  working = working.replace(MARKER_BLOCK, (_all, json: string) => {
    const g = tryParseGallery(json.trim()) || tryRecoverItems(json)
    if (g) galleries.push(g)
    return '\n'
  })

  // 2) 完整 media_gallery JSON
  working = stripAndCollectFullGallery(working, galleries)

  // 3) 残缺条目：,{"thumb":...,"url":...,"credit":...}]
  const orphan = tryRecoverItems(working)
  if (orphan && orphan.items.length) {
    galleries.push(orphan)
    working = stripMediaDebris(working)
  }

  // 4) 仍残留的 pexels URL
  if (!galleries.length) {
    const urls = uniqueUrls(working.match(PEXELS_URL) || [])
    if (urls.length) {
      galleries.push({
        type: 'gallery',
        items: urls.map((url) => ({ url, thumb: url, credit: 'Pexels' })),
      })
      working = working.replace(PEXELS_URL, ' ')
    }
  } else {
    working = working.replace(PEXELS_URL, ' ')
  }

  working = cleanupJunkText(working)
  const textParts = parseTextSegment(working)

  const parts: MessagePart[] = []
  // 标题类文字在前，图库紧随其后更符合【图片获取】
  const heading = textParts.filter((p) => p.type === 'text')
  const rest = textParts.filter((p) => p.type !== 'text')
  parts.push(...heading.filter((p) => p.type === 'text' && looksLikeHeading(p.text)))
  parts.push(...mergeGalleries(galleries))
  parts.push(...heading.filter((p) => p.type === 'text' && !looksLikeHeading(p.text)))
  parts.push(...rest)

  return parts.length ? parts : [{ type: 'text', text: content }]
}

function looksLikeHeading(text: string) {
  return /^【[^】]+】\s*$/m.test(text.trim()) || text.trim().startsWith('【')
}

function mergeGalleries(galleries: MessagePart[]): MessagePart[] {
  const items: MediaItem[] = []
  let note: string | undefined
  for (const g of galleries) {
    if (g.type !== 'gallery') continue
    for (const it of g.items) {
      if (!items.some((x) => x.url === it.url)) items.push(it)
    }
    if (g.note) note = g.note
  }
  return items.length ? [{ type: 'gallery', items, note }] : []
}

function stripAndCollectFullGallery(text: string, out: MessagePart[]): string {
  let result = text
  const idx = result.indexOf('"media_gallery"')
  if (idx < 0) return result
  const braceStart = result.lastIndexOf('{', idx)
  if (braceStart < 0) return result
  let depth = 0
  for (let i = braceStart; i < result.length; i++) {
    if (result[i] === '{') depth++
    if (result[i] === '}') depth--
    if (depth === 0) {
      const raw = result.slice(braceStart, i + 1)
      const g = tryParseGallery(raw)
      if (g) {
        out.push(g)
        result = result.slice(0, braceStart) + '\n' + result.slice(i + 1)
      }
      break
    }
  }
  return result
}

function tryParseGallery(raw: string): Extract<MessagePart, { type: 'gallery' }> | null {
  try {
    const obj = JSON.parse(raw) as {
      type?: string
      items?: MediaItem[]
      note?: string
    }
    if (obj.type !== 'media_gallery' || !Array.isArray(obj.items)) return null
    const items = normalizeItems(obj.items)
    if (!items.length) return null
    return { type: 'gallery', items, note: obj.note }
  } catch {
    return null
  }
}

function tryRecoverItems(raw: string): Extract<MessagePart, { type: 'gallery' }> | null {
  const items: MediaItem[] = []
  const re = new RegExp(MEDIA_ITEM_OBJ.source, 'g')
  let m: RegExpExecArray | null
  while ((m = re.exec(raw)) !== null) {
    try {
      const obj = JSON.parse(m[0]) as MediaItem
      if (obj.url?.startsWith('http')) {
        items.push({
          url: obj.url,
          thumb: obj.thumb || obj.url,
          credit: obj.credit || '',
        })
      }
    } catch {
      const url = m[1]
      if (url) items.push({ url, thumb: url, credit: '' })
    }
  }
  // 再捞 credit/thumb 乱序但含 url 的对象
  if (!items.length) {
    const loose =
      /\{\s*"thumb"\s*:\s*"(https?:\/\/[^"]+)"\s*,\s*"url"\s*:\s*"(https?:\/\/[^"]+)"\s*,\s*"credit"\s*:\s*"([^"]*)"\s*\}/g
    let lm: RegExpExecArray | null
    while ((lm = loose.exec(raw)) !== null) {
      items.push({ thumb: lm[1], url: lm[2], credit: lm[3] })
    }
  }
  const normalized = normalizeItems(items)
  return normalized.length ? { type: 'gallery', items: normalized } : null
}

function normalizeItems(items: MediaItem[]): MediaItem[] {
  const out: MediaItem[] = []
  for (const it of items) {
    if (!it?.url?.startsWith('http')) continue
    if (out.some((x) => x.url === it.url)) continue
    out.push({
      url: it.url,
      thumb: it.thumb || it.url,
      credit: it.credit || '',
    })
  }
  return out
}

function stripMediaDebris(text: string): string {
  return text
    .replace(MEDIA_ITEM_OBJ, ' ')
    .replace(/\{\s*"thumb"\s*:\s*"https?:\/\/[^"]+"\s*,\s*"url"\s*:\s*"https?:\/\/[^"]+"\s*,\s*"credit"\s*:\s*"[^"]*"\s*\}/g, ' ')
    .replace(/\[\s*,?/g, ' ')
    .replace(/,?\s*\]/g, ' ')
    .replace(/\{\s*"type"\s*:\s*"media_gallery"[\s\S]*$/g, ' ')
}

function cleanupJunkText(text: string): string {
  return text
    .replace(/<<<MEDIA_GALLERY>>>/g, ' ')
    .replace(/<<<END_MEDIA_GALLERY>>>/g, ' ')
    .replace(/"type"\s*:\s*"media_gallery"/g, ' ')
    .replace(/"items"\s*:\s*/g, ' ')
    .replace(/[{}\[\]]/g, ' ')
    .replace(/^[,\s]+/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function uniqueUrls(urls: string[]): string[] {
  const out: string[] = []
  for (const u of urls) {
    const clean = u.replace(/[),.;]+$/, '')
    if (!out.includes(clean)) out.push(clean)
  }
  return out
}

function parseTextSegment(segment: string): MessagePart[] {
  if (!segment.trim()) return []
  const parts: MessagePart[] = []
  let last = 0
  const re = new RegExp(MD_IMAGE.source, 'g')
  let match: RegExpExecArray | null

  while ((match = re.exec(segment)) !== null) {
    if (match.index > last) {
      parts.push(...splitTextAndTables(segment.slice(last, match.index)))
    }
    parts.push({ type: 'image', url: match[2], alt: match[1] || '图片' })
    last = match.index + match[0].length
  }
  if (last < segment.length) {
    parts.push(...splitTextAndTables(segment.slice(last)))
  }
  return parts
}

function splitTextAndTables(block: string): MessagePart[] {
  const lines = block.split('\n')
  const parts: MessagePart[] = []
  let buf: string[] = []
  let i = 0

  const flushText = () => {
    const text = buf.join('\n').trim()
    if (text) parts.push({ type: 'text', text })
    buf = []
  }

  while (i < lines.length) {
    const line = lines[i]
    const trimmed = line.trim()

    if (!trimmed) {
      buf.push(line)
      i++
      continue
    }

    if (BARE_IMAGE_LINE.test(trimmed) || PEXELS_LINE.test(trimmed)) {
      flushText()
      parts.push({ type: 'image', url: trimmed, alt: '图片' })
      i++
      continue
    }

    if (isTableHeaderRow(trimmed) && i + 1 < lines.length && isTableSepRow(lines[i + 1].trim())) {
      flushText()
      const headers = splitTableRow(trimmed)
      i += 2
      const rows: string[][] = []
      while (i < lines.length && isTableDataRow(lines[i].trim())) {
        rows.push(splitTableRow(lines[i].trim()))
        i++
      }
      parts.push({ type: 'table', headers, rows })
      continue
    }

    buf.push(line)
    i++
  }
  flushText()
  return parts
}

function isTableHeaderRow(line: string) {
  return line.startsWith('|') && line.endsWith('|') && line.includes('|')
}

function isTableSepRow(line: string) {
  return /^\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?$/.test(line)
}

function isTableDataRow(line: string) {
  return line.startsWith('|') && line.endsWith('|') && !isTableSepRow(line)
}

function splitTableRow(line: string): string[] {
  return line
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((c) => c.trim())
}

export function chatStorageKey(chatId: string) {
  return `travel-chat-cache:${chatId}`
}

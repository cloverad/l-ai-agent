/**
 * 解析 TravelManus 流式协议：思考区 / 正式答复 / PDF 下载标记。
 */

export type ManusSection = {
  thinkLines: string[]
  answer: string
  pdfs: string[]
  /** 思考是否已结束（收到 THINK_END） */
  thinkDone: boolean
}

const THINK_START = '<<<THINK_START>>>'
const THINK_END = '<<<THINK_END>>>'
const ANSWER_START = '<<<ANSWER_START>>>'
const ANSWER_END = '<<<ANSWER_END>>>'
const PDF_RE = /<<<PDF:([^>\n]+)>>>/g

export function parseManusContent(content: string): ManusSection {
  if (!content) {
    return { thinkLines: [], answer: '', pdfs: [], thinkDone: false }
  }

  const pdfs: string[] = []
  let working = content
  working = working.replace(PDF_RE, (_m, name: string) => {
    const n = name.trim()
    if (n && !pdfs.includes(n)) pdfs.push(n)
    return ''
  })

  let thinkBlock = ''
  let answerBlock = ''
  let thinkDone = working.includes(THINK_END)

  const ts = working.indexOf(THINK_START)
  const te = working.indexOf(THINK_END)
  if (ts >= 0) {
    const from = ts + THINK_START.length
    thinkBlock = te > ts ? working.slice(from, te) : working.slice(from)
  } else if (!working.includes(ANSWER_START)) {
    // 兼容旧流：整段当思考
    thinkBlock = working
  }

  const as = working.indexOf(ANSWER_START)
  const ae = working.indexOf(ANSWER_END)
  if (as >= 0) {
    const from = as + ANSWER_START.length
    answerBlock = ae > as ? working.slice(from, ae) : working.slice(from)
    thinkDone = true
  }

  const thinkLines = thinkBlock
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l && !l.startsWith('<<<'))

  return {
    thinkLines,
    answer: answerBlock.trim(),
    pdfs,
    thinkDone,
  }
}

export function pdfDownloadUrl(fileName: string) {
  return `/api/files/download?name=${encodeURIComponent(fileName)}`
}

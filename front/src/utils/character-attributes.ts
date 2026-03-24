export interface CharacterAttributeForm {
  age: string
  gender: string
  identity: string
  camp: string
  goal: string
  background: string
  appearance: string
  traits: string[]
  talents: string[]
  skills: string[]
  weaknesses: string[]
  equipment: string[]
  tags: string[]
  relations: string[]
  notes: string
  extraAttributes: Record<string, unknown>
}

const recognizedKeyMap: Record<string, keyof CharacterAttributeForm | 'ignore'> = {
  age: 'age',
  '年龄': 'age',
  gender: 'gender',
  '性别': 'gender',
  identity: 'identity',
  '身份': 'identity',
  '职业': 'identity',
  '角色定位': 'identity',
  camp: 'camp',
  '阵营': 'camp',
  goal: 'goal',
  '目标': 'goal',
  background: 'background',
  '背景': 'background',
  '出身': 'background',
  appearance: 'appearance',
  '外貌': 'appearance',
  traits: 'traits',
  '特性': 'traits',
  '性格': 'traits',
  '性格特性': 'traits',
  talents: 'talents',
  '天赋': 'talents',
  skills: 'skills',
  '技能': 'skills',
  '特长': 'skills',
  '能力': 'skills',
  weaknesses: 'weaknesses',
  '弱点': 'weaknesses',
  '缺点': 'weaknesses',
  equipment: 'equipment',
  '装备': 'equipment',
  tags: 'tags',
  '标签': 'tags',
  relations: 'relations',
  '关系': 'relations',
  '人际关系': 'relations',
  notes: 'notes',
  '备注': 'notes',
  '秘密': 'notes',
  '补充': 'notes',
  createTime: 'ignore',
  updateTime: 'ignore',
}

export function createEmptyCharacterAttributeForm(): CharacterAttributeForm {
  return {
    age: '',
    gender: '',
    identity: '',
    camp: '',
    goal: '',
    background: '',
    appearance: '',
    traits: [],
    talents: [],
    skills: [],
    weaknesses: [],
    equipment: [],
    tags: [],
    relations: [],
    notes: '',
    extraAttributes: {},
  }
}

export function parseCharacterAttributes(raw?: string | null): CharacterAttributeForm {
  const parsed = createEmptyCharacterAttributeForm()
  if (!raw?.trim()) {
    return parsed
  }

  try {
    const source = JSON.parse(raw) as Record<string, unknown>
    for (const [key, value] of Object.entries(source)) {
      const mappedKey = recognizedKeyMap[key]
      if (!mappedKey) {
        parsed.extraAttributes[key] = value
        continue
      }
      if (mappedKey === 'ignore') {
        continue
      }

      switch (mappedKey) {
        case 'traits':
        case 'talents':
        case 'skills':
        case 'weaknesses':
        case 'equipment':
        case 'tags':
        case 'relations':
          parsed[mappedKey] = normalizeList(value)
          break
        case 'age':
        case 'gender':
        case 'identity':
        case 'camp':
        case 'goal':
        case 'background':
        case 'appearance':
        case 'notes':
          parsed[mappedKey] = normalizeText(value)
          break
        default:
          break
      }
    }
  } catch {
    parsed.notes = raw.trim()
  }

  return parsed
}

export function buildCharacterAttributesJson(form: CharacterAttributeForm): string {
  const result: Record<string, unknown> = { ...form.extraAttributes }

  appendText(result, '年龄', form.age)
  appendText(result, '性别', form.gender)
  appendText(result, '身份', form.identity)
  appendText(result, '阵营', form.camp)
  appendText(result, '目标', form.goal)
  appendText(result, '背景', form.background)
  appendText(result, '外貌', form.appearance)
  appendList(result, '特性', form.traits)
  appendList(result, '天赋', form.talents)
  appendList(result, '技能', form.skills)
  appendList(result, '弱点', form.weaknesses)
  appendList(result, '装备', form.equipment)
  appendList(result, '标签', form.tags)
  appendList(result, '关系', form.relations)
  appendText(result, '备注', form.notes)

  return JSON.stringify(result, null, 2)
}

export function cloneCharacterAttributeForm(source: CharacterAttributeForm): CharacterAttributeForm {
  return {
    age: source.age,
    gender: source.gender,
    identity: source.identity,
    camp: source.camp,
    goal: source.goal,
    background: source.background,
    appearance: source.appearance,
    traits: [...source.traits],
    talents: [...source.talents],
    skills: [...source.skills],
    weaknesses: [...source.weaknesses],
    equipment: [...source.equipment],
    tags: [...source.tags],
    relations: [...source.relations],
    notes: source.notes,
    extraAttributes: { ...source.extraAttributes },
  }
}

function appendText(target: Record<string, unknown>, key: string, value: string) {
  if (value.trim()) {
    target[key] = value.trim()
  } else {
    delete target[key]
  }
}

function appendList(target: Record<string, unknown>, key: string, values: string[]) {
  const normalized = normalizeList(values)
  if (normalized.length) {
    target[key] = normalized
  } else {
    delete target[key]
  }
}

function normalizeText(value: unknown) {
  if (typeof value === 'string') {
    return value.trim()
  }
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function normalizeList(value: unknown) {
  if (Array.isArray(value)) {
    return uniqueStrings(value.map((item) => normalizeText(item)).filter(Boolean))
  }

  if (typeof value === 'string') {
    return uniqueStrings(
      value
        .split(/[\n,，、;；/|]/g)
        .map((item) => item.trim())
        .filter(Boolean),
    )
  }

  return []
}

function uniqueStrings(values: string[]) {
  return [...new Set(values)]
}

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import type { Character, CharacterInventoryItem, Item } from '@/types'
import { useInventoryStore } from '@/stores/inventory'
import { useItemStore } from '@/stores/item'

const props = defineProps<{
  modelValue: boolean
  projectId: number | null
  character: Character | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  changed: []
}>()

const itemStore = useItemStore()
const inventoryStore = useInventoryStore()

const tab = ref('inventory')
const inventorySaving = ref(false)
const itemDialog = ref(false)
const generationLoading = ref(false)
const editingItemId = ref<number | null>(null)

const addForm = reactive({
  itemId: null as number | null,
  quantity: 1,
  durability: 100,
  notes: '',
})

const generationForm = reactive({
  category: '',
  count: 3,
  prompt: '',
  constraints: '',
})

const itemForm = reactive({
  name: '',
  description: '',
  category: 'prop',
  rarity: 'common',
  stackable: false,
  maxStack: 20,
  usable: false,
  equippable: false,
  slotType: 'misc',
  itemValue: 0,
  weight: 0,
  attributesJson: '{}',
  effectJson: '{}',
  tags: '',
})

const categoryOptions = [
  { title: '道具', value: 'prop' },
  { title: '药品 / 消耗品', value: 'consumable' },
  { title: '装备', value: 'equipment' },
  { title: '材料', value: 'material' },
  { title: '任务物品', value: 'quest' },
]

const rarityOptions = [
  { title: '普通', value: 'common' },
  { title: '优秀', value: 'uncommon' },
  { title: '稀有', value: 'rare' },
  { title: '史诗', value: 'epic' },
  { title: '传说', value: 'legendary' },
  { title: '神器', value: 'artifact' },
]

const slotOptions = [
  { title: '武器', value: 'weapon' },
  { title: '头部', value: 'head' },
  { title: '身体', value: 'body' },
  { title: '饰品', value: 'accessory' },
  { title: '副手', value: 'offhand' },
  { title: '消耗位', value: 'consumable' },
  { title: '其他', value: 'misc' },
]

const inventory = computed(() =>
  props.character ? inventoryStore.getInventory(props.character.id) : [],
)

const inventoryLoading = computed(() =>
  props.character ? inventoryStore.isLoading(props.character.id) : false,
)

const availableItems = computed(() =>
  itemStore.items.map((item) => ({
    title: `${item.name} · ${formatCategory(item.category)}`,
    value: item.id,
    subtitle: item.rarity ? formatRarity(item.rarity) : '普通',
  })),
)

watch(
  () => [props.modelValue, props.projectId, props.character?.id] as const,
  async ([open, projectId, characterId]) => {
    if (!open || !projectId || !characterId) {
      return
    }
    await Promise.allSettled([
      itemStore.fetchByProject(projectId),
      inventoryStore.fetchByCharacter(projectId, characterId),
    ])
  },
  { immediate: true },
)

function close() {
  emit('update:modelValue', false)
}

function resetItemForm() {
  Object.assign(itemForm, {
    name: '',
    description: '',
    category: 'prop',
    rarity: 'common',
    stackable: false,
    maxStack: 20,
    usable: false,
    equippable: false,
    slotType: 'misc',
    itemValue: 0,
    weight: 0,
    attributesJson: '{}',
    effectJson: '{}',
    tags: '',
  })
}

function openCreateItem() {
  editingItemId.value = null
  resetItemForm()
  itemDialog.value = true
}

function openEditItem(item: Item) {
  editingItemId.value = item.id
  Object.assign(itemForm, {
    name: item.name,
    description: item.description || '',
    category: item.category,
    rarity: item.rarity,
    stackable: item.stackable,
    maxStack: item.maxStack || 20,
    usable: item.usable,
    equippable: item.equippable,
    slotType: item.slotType || 'misc',
    itemValue: item.itemValue || 0,
    weight: item.weight || 0,
    attributesJson: item.attributesJson || '{}',
    effectJson: item.effectJson || '{}',
    tags: item.tags || '',
  })
  itemDialog.value = true
}

async function submitItem() {
  if (!props.projectId) {
    return
  }

  inventorySaving.value = true
  try {
    const payload = {
      ...itemForm,
      name: itemForm.name.trim(),
      description: itemForm.description.trim(),
      attributesJson: itemForm.attributesJson.trim() || '{}',
      effectJson: itemForm.effectJson.trim() || '{}',
      tags: itemForm.tags.trim(),
    }
    if (editingItemId.value) {
      await itemStore.update(props.projectId, editingItemId.value, payload)
    } else {
      await itemStore.create(props.projectId, payload)
    }
    itemDialog.value = false
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function deleteItem(item: Item) {
  if (!props.projectId || !window.confirm(`确认删除物品“${item.name}”吗？相关背包引用也会被移除。`)) {
    return
  }

  inventorySaving.value = true
  try {
    await itemStore.remove(props.projectId, item.id)
    if (props.character) {
      await inventoryStore.fetchByCharacter(props.projectId, props.character.id)
    }
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function addSelectedItem() {
  if (!props.projectId || !props.character?.id || !addForm.itemId) {
    return
  }

  inventorySaving.value = true
  try {
    await inventoryStore.add(props.projectId, props.character.id, {
      itemId: addForm.itemId,
      quantity: addForm.quantity,
      durability: addForm.durability,
      notes: addForm.notes.trim() || undefined,
    })
    addForm.itemId = null
    addForm.quantity = 1
    addForm.durability = 100
    addForm.notes = ''
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function quickAddItem(item: Item) {
  if (!props.projectId || !props.character?.id) {
    return
  }
  inventorySaving.value = true
  try {
    await inventoryStore.add(props.projectId, props.character.id, {
      itemId: item.id,
      quantity: 1,
      durability: 100,
    })
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function adjustQuantity(entry: CharacterInventoryItem, delta: number) {
  if (!props.projectId || !props.character?.id) {
    return
  }

  const nextQuantity = entry.quantity + delta
  if (nextQuantity <= 0) {
    await removeInventoryItem(entry)
    return
  }

  inventorySaving.value = true
  try {
    await inventoryStore.update(props.projectId, props.character.id, entry.id, {
      quantity: nextQuantity,
    })
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function toggleEquipped(entry: CharacterInventoryItem) {
  if (!props.projectId || !props.character?.id) {
    return
  }

  inventorySaving.value = true
  try {
    await inventoryStore.update(props.projectId, props.character.id, entry.id, {
      equipped: !entry.equipped,
    })
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function removeInventoryItem(entry: CharacterInventoryItem) {
  if (!props.projectId || !props.character?.id || !window.confirm(`确认移出“${displayInventoryName(entry)}”吗？`)) {
    return
  }

  inventorySaving.value = true
  try {
    await inventoryStore.remove(props.projectId, props.character.id, entry.id)
    emit('changed')
  } finally {
    inventorySaving.value = false
  }
}

async function generateToLibrary() {
  if (!props.projectId) {
    return
  }

  generationLoading.value = true
  try {
    await itemStore.generate(props.projectId, {
      category: generationForm.category || undefined,
      count: generationForm.count,
      prompt: generationForm.prompt.trim() || undefined,
      constraints: generationForm.constraints.trim() || undefined,
    })
    tab.value = 'library'
    emit('changed')
  } finally {
    generationLoading.value = false
  }
}

async function generateToInventory() {
  if (!props.projectId || !props.character?.id) {
    return
  }

  generationLoading.value = true
  try {
    await inventoryStore.generate(props.projectId, props.character.id, {
      category: generationForm.category || undefined,
      count: generationForm.count,
      prompt: generationForm.prompt.trim() || undefined,
      constraints: generationForm.constraints.trim() || undefined,
    })
    tab.value = 'inventory'
    emit('changed')
  } finally {
    generationLoading.value = false
  }
}

function formatCategory(value?: string) {
  return categoryOptions.find((item) => item.value === value)?.title || '道具'
}

function formatRarity(value?: string) {
  return rarityOptions.find((item) => item.value === value)?.title || '普通'
}

function formatSlot(value?: string) {
  return slotOptions.find((item) => item.value === value)?.title || '其他'
}

function displayInventoryName(entry: CharacterInventoryItem) {
  return entry.customName || entry.item?.name || '未命名物品'
}
</script>

<template>
  <v-dialog :model-value="modelValue" max-width="1180" scrollable @update:model-value="emit('update:modelValue', $event)">
    <v-card>
      <v-card-title class="d-flex align-center justify-space-between ga-3">
        <div>
          <div class="text-h5">角色背包</div>
          <div class="text-body-2 text-medium-emphasis mt-1">
            {{ character?.name || '未选择角色' }} · 当前物品 {{ character?.inventoryItemCount || 0 }} · 已装备 {{ character?.equippedItemCount || 0 }}
          </div>
        </div>
        <v-btn icon="mdi-close" variant="text" @click="close" />
      </v-card-title>

      <v-tabs v-model="tab" color="primary">
        <v-tab value="inventory">角色背包</v-tab>
        <v-tab value="library">项目物品库</v-tab>
        <v-tab value="generate">物品生成</v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <v-window-item value="inventory">
          <v-card-text class="pa-6">
            <v-row class="mb-4">
              <v-col cols="12" md="6">
                <v-select
                  v-model="addForm.itemId"
                  :items="availableItems"
                  item-title="title"
                  item-value="value"
                  label="从项目物品库加入背包"
                  clearable
                  no-data-text="当前项目还没有物品，可先到“项目物品库”页签创建"
                />
              </v-col>
              <v-col cols="6" md="2">
                <v-text-field v-model.number="addForm.quantity" type="number" label="数量" min="1" />
              </v-col>
              <v-col cols="6" md="2">
                <v-text-field v-model.number="addForm.durability" type="number" label="耐久" min="0" />
              </v-col>
              <v-col cols="12" md="2" class="d-flex align-end">
                <v-btn block color="primary" :loading="inventorySaving" :disabled="!addForm.itemId" @click="addSelectedItem">
                  加入背包
                </v-btn>
              </v-col>
              <v-col cols="12">
                <v-text-field v-model="addForm.notes" label="加入备注" />
              </v-col>
            </v-row>

            <v-progress-linear v-if="inventoryLoading" indeterminate color="primary" class="mb-4" />

            <div v-if="!inventory.length && !inventoryLoading" class="text-body-2 text-medium-emphasis">
              当前角色还没有背包物品。可以从项目物品库添加，也可以去“物品生成”页签直接生成并加入背包。
            </div>

            <v-row v-else>
              <v-col v-for="entry in inventory" :key="entry.id" cols="12" md="6">
                <v-card class="soft-panel h-100">
                  <v-card-text class="d-flex flex-column h-100">
                    <div class="d-flex justify-space-between align-start ga-3">
                      <div>
                        <div class="text-h6">{{ displayInventoryName(entry) }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          {{ formatCategory(entry.item.category) }} · {{ formatRarity(entry.item.rarity) }} · {{ formatSlot(entry.item.slotType) }}
                        </div>
                      </div>
                      <div class="d-flex flex-column align-end ga-2">
                        <v-chip size="small" color="secondary" variant="tonal">数量 {{ entry.quantity }}</v-chip>
                        <v-chip v-if="entry.equipped" size="small" color="primary" variant="tonal">已装备</v-chip>
                      </div>
                    </div>

                    <div class="text-body-2 text-medium-emphasis mt-3">
                      {{ entry.item.description || '暂无物品描述。' }}
                    </div>

                    <div class="d-flex flex-wrap ga-2 mt-4">
                      <v-chip size="small" variant="outlined">耐久 {{ entry.durability ?? 100 }}</v-chip>
                      <v-chip size="small" variant="outlined">价值 {{ entry.item.itemValue ?? 0 }}</v-chip>
                      <v-chip size="small" variant="outlined">重量 {{ entry.item.weight ?? 0 }}</v-chip>
                      <v-chip v-if="entry.item.tags" size="small" variant="outlined">{{ entry.item.tags }}</v-chip>
                    </div>

                    <div class="d-flex ga-2 mt-auto pt-4 flex-wrap">
                      <v-btn
                        size="small"
                        variant="outlined"
                        :disabled="!entry.item.stackable"
                        @click="adjustQuantity(entry, -1)"
                      >
                        数量 -1
                      </v-btn>
                      <v-btn
                        size="small"
                        variant="outlined"
                        :disabled="!entry.item.stackable"
                        @click="adjustQuantity(entry, 1)"
                      >
                        数量 +1
                      </v-btn>
                      <v-btn
                        size="small"
                        color="primary"
                        variant="tonal"
                        :disabled="!entry.item.equippable"
                        @click="toggleEquipped(entry)"
                      >
                        {{ entry.equipped ? '卸下' : '装备' }}
                      </v-btn>
                      <v-btn size="small" color="error" variant="text" @click="removeInventoryItem(entry)">
                        移出背包
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-window-item>

        <v-window-item value="library">
          <v-card-text class="pa-6">
            <div class="d-flex justify-space-between align-center flex-wrap ga-3 mb-4">
              <div class="text-body-2 text-medium-emphasis">
                项目物品库是当前项目可复用的物品定义，角色背包会引用这里的物品。
              </div>
              <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreateItem">新建物品</v-btn>
            </div>

            <div v-if="!itemStore.items.length" class="text-body-2 text-medium-emphasis">
              当前项目还没有物品。可以手动创建，也可以去“物品生成”页签让 AI 先生成一批。
            </div>

            <v-row v-else>
              <v-col v-for="item in itemStore.items" :key="item.id" cols="12" md="6">
                <v-card class="soft-panel h-100">
                  <v-card-text class="d-flex flex-column h-100">
                    <div class="d-flex justify-space-between align-start ga-3">
                      <div>
                        <div class="text-h6">{{ item.name }}</div>
                        <div class="text-caption text-medium-emphasis mt-1">
                          {{ formatCategory(item.category) }} · {{ formatRarity(item.rarity) }}
                        </div>
                      </div>
                      <v-chip size="small" variant="tonal" color="secondary">
                        {{ item.sourceType === 'ai' ? 'AI 生成' : '手动创建' }}
                      </v-chip>
                    </div>

                    <div class="text-body-2 text-medium-emphasis mt-3">
                      {{ item.description || '暂无物品描述。' }}
                    </div>

                    <div class="d-flex flex-wrap ga-2 mt-4">
                      <v-chip size="small" variant="outlined">堆叠 {{ item.stackable ? item.maxStack || 20 : 1 }}</v-chip>
                      <v-chip size="small" variant="outlined">价值 {{ item.itemValue ?? 0 }}</v-chip>
                      <v-chip size="small" variant="outlined">重量 {{ item.weight ?? 0 }}</v-chip>
                      <v-chip size="small" variant="outlined">{{ formatSlot(item.slotType) }}</v-chip>
                    </div>

                    <div class="d-flex ga-2 mt-auto pt-4 flex-wrap">
                      <v-btn size="small" color="primary" variant="tonal" @click="quickAddItem(item)">加入当前角色背包</v-btn>
                      <v-btn size="small" variant="outlined" @click="openEditItem(item)">编辑</v-btn>
                      <v-btn size="small" color="error" variant="text" @click="deleteItem(item)">删除</v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-window-item>

        <v-window-item value="generate">
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="4">
                <v-select
                  v-model="generationForm.category"
                  :items="[{ title: '混合生成', value: '' }, ...categoryOptions]"
                  item-title="title"
                  item-value="value"
                  label="生成分类"
                />
              </v-col>
              <v-col cols="12" md="4">
                <v-text-field v-model.number="generationForm.count" type="number" min="1" max="10" label="生成数量" />
              </v-col>
              <v-col cols="12" md="4" class="d-flex align-end">
                <v-chip color="secondary" variant="tonal">
                  角色：{{ character?.name || '未选择' }}
                </v-chip>
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="generationForm.prompt"
                  label="物品生成需求"
                  rows="4"
                  auto-grow
                  hint="例如：生成适合废土探索小队使用的治疗药品、一次性侦查道具和近战武器。"
                  persistent-hint
                />
              </v-col>
              <v-col cols="12">
                <v-textarea
                  v-model="generationForm.constraints"
                  label="额外约束"
                  rows="3"
                  auto-grow
                  hint="例如：不要现代枪械；优先突出炼金和蒸汽风格；描述要适合剧情使用。"
                  persistent-hint
                />
              </v-col>
            </v-row>

            <div class="d-flex ga-3 flex-wrap mt-4">
              <v-btn color="primary" :loading="generationLoading" @click="generateToLibrary">
                生成到项目物品库
              </v-btn>
              <v-btn variant="outlined" :loading="generationLoading" @click="generateToInventory">
                生成并加入当前角色背包
              </v-btn>
            </div>
          </v-card-text>
        </v-window-item>
      </v-window>
    </v-card>
  </v-dialog>

  <v-dialog v-model="itemDialog" max-width="900" scrollable>
    <v-card>
      <v-card-title>{{ editingItemId ? '编辑物品' : '新建物品' }}</v-card-title>
      <v-card-text class="pt-4">
        <v-row>
          <v-col cols="12" md="6">
            <v-text-field v-model="itemForm.name" label="物品名称" />
          </v-col>
          <v-col cols="12" md="6">
            <v-select v-model="itemForm.category" :items="categoryOptions" item-title="title" item-value="value" label="物品分类" />
          </v-col>
          <v-col cols="12" md="6">
            <v-select v-model="itemForm.rarity" :items="rarityOptions" item-title="title" item-value="value" label="稀有度" />
          </v-col>
          <v-col cols="12" md="6">
            <v-select v-model="itemForm.slotType" :items="slotOptions" item-title="title" item-value="value" label="装备部位" />
          </v-col>
          <v-col cols="12">
            <v-textarea v-model="itemForm.description" label="物品描述" rows="3" auto-grow />
          </v-col>
          <v-col cols="12" md="4">
            <v-switch v-model="itemForm.stackable" color="primary" label="可堆叠" />
          </v-col>
          <v-col cols="12" md="4">
            <v-switch v-model="itemForm.usable" color="primary" label="可使用" />
          </v-col>
          <v-col cols="12" md="4">
            <v-switch v-model="itemForm.equippable" color="primary" label="可装备" />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field v-model.number="itemForm.maxStack" type="number" min="1" label="最大堆叠数" />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field v-model.number="itemForm.itemValue" type="number" min="0" label="价值" />
          </v-col>
          <v-col cols="12" md="4">
            <v-text-field v-model.number="itemForm.weight" type="number" min="0" label="重量" />
          </v-col>
          <v-col cols="12">
            <v-text-field v-model="itemForm.tags" label="标签" hint="多个标签用逗号分隔" persistent-hint />
          </v-col>
          <v-col cols="12" md="6">
            <v-textarea v-model="itemForm.attributesJson" label="属性 JSON" rows="6" auto-grow />
          </v-col>
          <v-col cols="12" md="6">
            <v-textarea v-model="itemForm.effectJson" label="效果 JSON" rows="6" auto-grow />
          </v-col>
        </v-row>
      </v-card-text>
      <v-card-actions class="justify-end">
        <v-btn variant="text" @click="itemDialog = false">取消</v-btn>
        <v-btn color="primary" :loading="inventorySaving" @click="submitItem">保存</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

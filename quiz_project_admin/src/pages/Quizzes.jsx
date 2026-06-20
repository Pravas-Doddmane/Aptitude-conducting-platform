import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { getQuizzes, createQuiz, updateQuiz, deleteQuiz, publishQuiz, unpublishQuiz } from '../api/quizService'
import { getCategories } from '../api/categoryService'
import { getCertificateTemplates } from '../api/certificateTemplateService'
import { Table } from '../components/ui/Table'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { useToast } from '../hooks/useToast'
import { Plus, Pencil, Trash2, Play, Pause, FileQuestion, Search } from 'lucide-react'
import { EmptyState } from '../components/ui/EmptyState'

const schema = z.object({
  categoryId: z.string().min(1, 'Category is required'),
  title: z.string().min(1, 'Title is required'),
  description: z.string().optional(),
  durationHours: z.coerce.number().int().min(0).max(23),
  durationMinutes: z.coerce.number().int().min(0).max(59),
  durationSecondsPart: z.coerce.number().int().min(0).max(59),
  maxAttemptsPerUser: z.coerce.number().int().min(0).max(100),
  availableFrom: z.string().optional(),
  availableTo: z.string().optional(),
  practiceMode: z.boolean().optional().default(false),
  competitionMode: z.boolean().optional().default(false),
  requireFullScreen: z.boolean().optional().default(false),
  preventTabSwitch: z.boolean().optional().default(false),
  requireCamera: z.boolean().optional().default(false),
  requireMicrophone: z.boolean().optional().default(false),
  requireLocation: z.boolean().optional().default(false),
  certificateEnabled: z.boolean().optional().default(false),
  certificateAutoGenerate: z.boolean().optional().default(true),
  certificateDelayHours: z.coerce.number().int().min(0).max(168).optional().default(0),
  certificateTemplateId: z.string().optional(),
  passingScore: z.coerce.number().min(0).max(100),
  questionIds: z.array(z.string()).optional().default([]),
  categoryIds: z.array(z.string()).optional().default([]),
}).refine(
  (data) => data.durationHours * 3600 + data.durationMinutes * 60 + data.durationSecondsPart >= 10,
  {
    message: 'Duration must be at least 10 seconds',
    path: ['durationSecondsPart'],
  }
).refine(
  (data) => data.practiceMode || !data.certificateEnabled || !!data.certificateTemplateId,
  {
    message: 'Select a certificate template',
    path: ['certificateTemplateId'],
  }
).refine(
  (data) => !(data.practiceMode && data.competitionMode),
  {
    message: 'Choose Practice or Competition, not both',
    path: ['competitionMode'],
  }
)

const toDurationParts = (totalSeconds = 0) => {
  const safeSeconds = Math.max(0, Number(totalSeconds) || 0)
  const durationHours = Math.floor(safeSeconds / 3600)
  const durationMinutes = Math.floor((safeSeconds % 3600) / 60)
  const durationSecondsPart = safeSeconds % 60
  return { durationHours, durationMinutes, durationSecondsPart }
}

const toTotalSeconds = (hours, minutes, seconds) => (Number(hours) * 3600) + (Number(minutes) * 60) + Number(seconds)

const formatDuration = (totalSeconds = 0) => {
  const { durationHours, durationMinutes, durationSecondsPart } = toDurationParts(totalSeconds)
  return `${String(durationHours).padStart(2, '0')}:${String(durationMinutes).padStart(2, '0')}:${String(durationSecondsPart).padStart(2, '0')}`
}

const pad = (value) => String(value).padStart(2, '0')

const toDateTimeLocalValue = (value) => {
  if (!value) return ''
  const date = new Date(value)
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const toIsoFromLocal = (value) => {
  if (!value) return null
  return new Date(value).toISOString()
}

export const Quizzes = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingQuiz, setEditingQuiz] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: quizzes, isLoading } = useQuery({
    queryKey: ['quizzes'],
    queryFn: () => getQuizzes(),
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: () => getCategories(),
  })
  const { data: certificateTemplates } = useQuery({
    queryKey: ['certificateTemplates'],
    queryFn: getCertificateTemplates,
  })
  const minAvailableDateTime = useMemo(() => toDateTimeLocalValue(new Date()), [])

  const categoryOptions = useMemo(
    () => (Array.isArray(categories) ? categories.map((category) => ({ value: category.id, label: category.name })) : []),
    [categories]
  )

  const categoryNameById = useMemo(
    () => new Map((Array.isArray(categories) ? categories : []).map((category) => [category.id, category.name])),
    [categories]
  )
  const certificateTemplateOptions = useMemo(
    () => (Array.isArray(certificateTemplates) ? certificateTemplates
      .filter((template) => template.active)
      .map((template) => ({ value: template.id, label: template.name })) : []),
    [certificateTemplates]
  )

  const filteredQuizzes = useMemo(() => {
    const query = search.trim().toLowerCase()
    let result = quizzes || []
    
    // Filter by search query
    if (query) {
      result = result.filter((quiz) => {
        const categoryNames = quiz.categoryNames?.length
          ? quiz.categoryNames.join(' ')
          : quiz.categoryName || categoryNameById.get(quiz.categoryId) || ''
        const fields = [quiz.title, categoryNames, quiz.description, quiz.status]
        return fields.some((field) => field?.toLowerCase().includes(query))
      })
    }
    
    // Sort by creation date (newest first)
    return result.sort((a, b) => {
      const dateA = new Date(a.createdAt || 0)
      const dateB = new Date(b.createdAt || 0)
      return dateB - dateA // Descending order (newest first)
    })
  }, [quizzes, search, categoryNameById])

  const createMutation = useMutation({
    mutationFn: createQuiz,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
      toast.success('Quiz created')
      setIsModalOpen(false)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateQuiz(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
      toast.success('Quiz updated')
      setIsModalOpen(false)
      setEditingQuiz(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteQuiz,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
      toast.success('Quiz deleted')
      setDeleteId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const publishMutation = useMutation({
    mutationFn: publishQuiz,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
      toast.success('Quiz published')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const unpublishMutation = useMutation({
    mutationFn: unpublishQuiz,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
      toast.success('Quiz unpublished')
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const { register, handleSubmit, formState: { errors }, reset, watch, setValue } = useForm({
    resolver: zodResolver(schema),
  })
  const categoryRegistration = register('categoryId')
  const primaryCategoryId = watch('categoryId')
  const selectedCategoryIds = watch('categoryIds') || []
  const practiceMode = watch('practiceMode')
  const competitionMode = watch('competitionMode')
  const certificateEnabled = watch('certificateEnabled')

  const handlePrimaryCategoryChange = (event) => {
    const categoryId = event.target.value
    setValue('categoryId', categoryId, { shouldDirty: true, shouldValidate: true })
    if (categoryId && !selectedCategoryIds.includes(categoryId)) {
      setValue('categoryIds', [categoryId, ...selectedCategoryIds], { shouldDirty: true, shouldValidate: true })
    }
  }

  const toggleCategoryId = (categoryId) => {
    if (categoryId === primaryCategoryId) return
    const nextCategoryIds = selectedCategoryIds.includes(categoryId)
      ? selectedCategoryIds.filter((id) => id !== categoryId)
      : [...selectedCategoryIds, categoryId]
    setValue('categoryIds', nextCategoryIds, { shouldDirty: true, shouldValidate: true })
  }

  const clearCompetitionRequirements = () => {
    setValue('requireFullScreen', false, { shouldDirty: true, shouldValidate: true })
    setValue('preventTabSwitch', false, { shouldDirty: true, shouldValidate: true })
    setValue('requireCamera', false, { shouldDirty: true, shouldValidate: true })
    setValue('requireMicrophone', false, { shouldDirty: true, shouldValidate: true })
    setValue('requireLocation', false, { shouldDirty: true, shouldValidate: true })
  }

  const clearCertificateSettings = () => {
    setValue('certificateEnabled', false, { shouldDirty: true, shouldValidate: true })
    setValue('certificateAutoGenerate', false, { shouldDirty: true, shouldValidate: true })
    setValue('certificateDelayHours', 0, { shouldDirty: true, shouldValidate: true })
    setValue('certificateTemplateId', '', { shouldDirty: true, shouldValidate: true })
  }

  const handlePracticeModeChange = (event) => {
    const checked = event.target.checked
    setValue('practiceMode', checked, { shouldDirty: true, shouldValidate: true })
    if (checked) {
      setValue('competitionMode', false, { shouldDirty: true, shouldValidate: true })
      clearCompetitionRequirements()
      clearCertificateSettings()
    }
  }

  const handleCompetitionModeChange = (event) => {
    const checked = event.target.checked
    setValue('competitionMode', checked, { shouldDirty: true, shouldValidate: true })
    if (checked) {
      setValue('practiceMode', false, { shouldDirty: true, shouldValidate: true })
    } else {
      clearCompetitionRequirements()
    }
  }

  const openCreate = () => {
    setEditingQuiz(null)
    reset({
      categoryId: '',
      title: '',
      description: '',
      durationHours: 0,
      durationMinutes: 1,
      durationSecondsPart: 0,
      maxAttemptsPerUser: 0,
      availableFrom: '',
      availableTo: '',
      practiceMode: false,
      competitionMode: false,
      requireFullScreen: false,
      preventTabSwitch: false,
      requireCamera: false,
      requireMicrophone: false,
      requireLocation: false,
      certificateEnabled: false,
      certificateAutoGenerate: true,
      certificateDelayHours: 0,
      certificateTemplateId: '',
      passingScore: 50,
      questionIds: [],
      categoryIds: [],
    })
    setIsModalOpen(true)
  }

  const openEdit = (quiz) => {
    setEditingQuiz(quiz)
    const { durationHours, durationMinutes, durationSecondsPart } = toDurationParts(quiz.durationSeconds || 60)
    reset({
      categoryId: quiz.categoryId || '',
      title: quiz.title,
      description: quiz.description || '',
      durationHours,
      durationMinutes,
      durationSecondsPart,
      maxAttemptsPerUser: quiz.maxAttemptsPerUser ?? 0,
      availableFrom: toDateTimeLocalValue(quiz.availableFrom),
      availableTo: toDateTimeLocalValue(quiz.availableTo),
      practiceMode: !!quiz.practiceMode,
      competitionMode: !!quiz.competitionMode,
      requireFullScreen: !!quiz.requireFullScreen,
      preventTabSwitch: !!quiz.preventTabSwitch,
      requireCamera: !!quiz.requireCamera,
      requireMicrophone: !!quiz.requireMicrophone,
      requireLocation: !!quiz.requireLocation,
      certificateEnabled: !quiz.practiceMode && !!quiz.certificateEnabled,
      certificateAutoGenerate: !quiz.practiceMode && (quiz.certificateAutoGenerate ?? true),
      certificateDelayHours: quiz.practiceMode ? 0 : quiz.certificateDelayHours ?? 0,
      certificateTemplateId: quiz.practiceMode ? '' : quiz.certificateTemplateId || '',
      passingScore: quiz.passingScore || 50,
      questionIds: quiz.questionIds || [],
      categoryIds: quiz.categoryIds?.length ? quiz.categoryIds : [quiz.categoryId].filter(Boolean),
    })
    setIsModalOpen(true)
  }

  const onSubmit = (formData) => {
    const durationSeconds = toTotalSeconds(formData.durationHours, formData.durationMinutes, formData.durationSecondsPart)
    const categoryIds = Array.from(new Set([formData.categoryId, ...(formData.categoryIds || [])].filter(Boolean)))
    const payload = {
      ...formData,
      durationSeconds,
      categoryIds,
      availableFrom: toIsoFromLocal(formData.availableFrom),
      availableTo: toIsoFromLocal(formData.availableTo),
      practiceMode: !!formData.practiceMode && !formData.competitionMode,
      competitionMode: !!formData.competitionMode && !formData.practiceMode,
      requireFullScreen: !!formData.competitionMode && !formData.practiceMode && !!formData.requireFullScreen,
      preventTabSwitch: !!formData.competitionMode && !formData.practiceMode && !!formData.preventTabSwitch,
      requireCamera: !!formData.competitionMode && !formData.practiceMode && !!formData.requireCamera,
      requireMicrophone: !!formData.competitionMode && !formData.practiceMode && !!formData.requireMicrophone,
      requireLocation: !!formData.competitionMode && !formData.practiceMode && !!formData.requireLocation,
      certificateEnabled: !formData.practiceMode && !!formData.certificateEnabled,
      certificateAutoGenerate: !formData.practiceMode && !!formData.certificateEnabled && !!formData.certificateAutoGenerate,
      certificateDelayHours: !formData.practiceMode && formData.certificateEnabled ? Number(formData.certificateDelayHours || 0) : 0,
      certificateTemplateId: !formData.practiceMode && formData.certificateEnabled ? formData.certificateTemplateId || null : null,
    }
    delete payload.durationHours
    delete payload.durationMinutes
    delete payload.durationSecondsPart

    if (editingQuiz) {
      updateMutation.mutate({ id: editingQuiz.id, data: payload })
    } else {
      createMutation.mutate(payload)
    }
  }

  const columns = [
    { header: 'Title', accessor: 'title' },
    { header: 'Categories', render: (row) => row.categoryNames?.length ? row.categoryNames.join(', ') : row.categoryName || categoryNameById.get(row.categoryId) || '—' },
    { header: 'Questions', render: (row) => row.questionCount ?? 0 },
    { header: 'Easy', render: (row) => row.easyQuestionCount ?? 0 },
    { header: 'Medium', render: (row) => row.mediumQuestionCount ?? 0 },
    { header: 'Hard', render: (row) => row.hardQuestionCount ?? 0 },
    { header: 'Duration', render: (row) => formatDuration(row.durationSeconds) },
    { header: 'Attempts/User', render: (row) => (row.maxAttemptsPerUser && row.maxAttemptsPerUser > 0 ? row.maxAttemptsPerUser : 'Unlimited') },
    {
      header: 'Practice',
      render: (row) => row.practiceMode
        ? <Badge variant="info">Practice</Badge>
        : <Badge variant="neutral">No</Badge>,
    },
    {
      header: 'Mode',
      render: (row) => row.competitionMode
        ? <Badge variant="danger">Competition</Badge>
        : <Badge variant="neutral">Normal</Badge>,
    },
    {
      header: 'Certificate',
      render: (row) => {
        if (!row.certificateEnabled) return <Badge variant="neutral">No</Badge>
        const delay = row.certificateDelayHours && row.certificateDelayHours > 0 ? `${row.certificateDelayHours}h delay` : 'Instant'
        return (
          <div className="flex flex-col gap-1">
            <Badge variant="info">{row.certificateTemplateName || 'Enabled'}</Badge>
            <span className="text-xs text-gray-500">{row.certificateAutoGenerate ? 'Auto' : 'Manual'} · {delay}</span>
          </div>
        )
      },
    },
    {
      header: 'Availability',
      render: (row) => {
        const from = row.availableFrom ? toDateTimeLocalValue(row.availableFrom).replace('T', ' ') : 'Anytime'
        const to = row.availableTo ? toDateTimeLocalValue(row.availableTo).replace('T', ' ') : 'Open ended'
        return `${from} → ${to}`
      },
    },
    {
      header: 'Status',
      render: (row) => {
        if (row.status === 'PUBLISHED') return <Badge variant="success">Published</Badge>
        if (row.status === 'UNPUBLISHED') return <Badge variant="neutral">Unpublished</Badge>
        return <Badge variant="warning">Draft</Badge>
      },
    },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" onClick={() => openEdit(row)}><Pencil className="w-4 h-4" /></Button>
          {row.status === 'PUBLISHED' ? (
            <Button variant="ghost" size="sm" onClick={() => unpublishMutation.mutate(row.id)}><Pause className="w-4 h-4 text-amber-600" /></Button>
          ) : (
            <Button variant="ghost" size="sm" onClick={() => publishMutation.mutate(row.id)}><Play className="w-4 h-4 text-emerald-600" /></Button>
          )}
          <Button variant="ghost" size="sm" disabled={row.status === 'PUBLISHED'} onClick={() => { if (row.status !== 'PUBLISHED') { setDeleteId(row.id) }
          }}
          title={row.status === 'PUBLISHED' ? 'Published quizzes cannot be deleted' : 'Delete quiz'}>
            <Trash2
            className={`w-4 h-4 ${
              row.status === 'PUBLISHED' ? 'text-gray-400 cursor-not-allowed' : 'text-red-500' }`} />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quizzes</h1>
          <p className="text-sm text-gray-500 mt-1">Manage quizzes and their status</p>
        </div>
        <Button onClick={openCreate}><Plus className="w-4 h-4 mr-2" /> Create Quiz</Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search by title or category..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {!isLoading && filteredQuizzes.length === 0 ? (
        <EmptyState
          title="No quizzes found"
          description={search ? 'No quizzes match your search.' : 'Create your first quiz.'}
          icon={FileQuestion}
        />
      ) : (
        <Table columns={columns} data={filteredQuizzes} loading={isLoading} />
      )}

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingQuiz ? 'Edit Quiz' : 'Create Quiz'}>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Select
            label="Primary Category"
            options={categoryOptions}
            error={errors.categoryId?.message}
            {...categoryRegistration}
            value={primaryCategoryId || ''}
            onChange={(event) => {
              categoryRegistration.onChange(event)
              handlePrimaryCategoryChange(event)
            }}
          />
          <div className="rounded-xl border border-gray-200 bg-gray-50 p-4">
            <p className="text-sm font-semibold text-gray-800">Additional Categories</p>
            <p className="text-xs text-gray-500 mt-1">Select extra categories. Quiz questions will come from all selected categories.</p>
            <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-2">
              {categoryOptions.map((category) => {
                const checked = selectedCategoryIds.includes(category.value) || primaryCategoryId === category.value
                const isPrimary = primaryCategoryId === category.value
                return (
                  <label
                    key={category.value}
                    className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm ${checked ? 'border-indigo-200 bg-white text-indigo-700' : 'border-gray-200 bg-white text-gray-700'}`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      disabled={isPrimary}
                      onChange={() => toggleCategoryId(category.value)}
                      className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500 disabled:opacity-60"
                    />
                    <span>{category.label}{isPrimary ? ' (Primary)' : ''}</span>
                  </label>
                )
              })}
            </div>
          </div>
          <Input label="Title" error={errors.title?.message} {...register('title')} />
          <Input label="Description" error={errors.description?.message} {...register('description')} />
          <div className="grid grid-cols-3 gap-3">
            <Input label="Hours" type="number" min="0" max="23" error={errors.durationHours?.message} {...register('durationHours')} />
            <Input label="Minutes" type="number" min="0" max="59" error={errors.durationMinutes?.message} {...register('durationMinutes')} />
            <Input label="Seconds" type="number" min="0" max="59" error={errors.durationSecondsPart?.message} {...register('durationSecondsPart')} />
          </div>
          <Input label="Attempts per User" type="number" min="0" max="100" placeholder="0 = Unlimited" error={errors.maxAttemptsPerUser?.message} {...register('maxAttemptsPerUser')} />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <Input
              label="Available From"
              type="datetime-local"
              min={minAvailableDateTime}
              error={errors.availableFrom?.message}
              {...register('availableFrom')}
            />
            <Input
              label="Available To"
              type="datetime-local"
              min={minAvailableDateTime}
              error={errors.availableTo?.message}
              {...register('availableTo')}
            />
          </div>
          <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4 space-y-3">
            <div>
              <p className="text-sm font-semibold text-gray-900">Quiz Type</p>
              <p className="text-xs text-gray-500 mt-1">Choose only one mode. Leave both off for a normal quiz.</p>
            </div>
            <div className={`grid grid-cols-1 gap-3 ${!practiceMode && !competitionMode ? 'md:grid-cols-2' : ''}`}>
              {!competitionMode && (
                <label className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm transition ${practiceMode ? 'border-indigo-300 bg-white shadow-sm' : 'border-gray-200 bg-white hover:border-indigo-200'}`}>
                  <input
                    type="checkbox"
                    checked={!!practiceMode}
                    onChange={handlePracticeModeChange}
                    className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span>
                    <span className="block font-semibold text-gray-800">Practice Quiz</span>
                    <span className="text-gray-500">For learning and practice. Competition settings are hidden.</span>
                  </span>
                </label>
              )}
              {!practiceMode && (
                <label className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm transition ${competitionMode ? 'border-red-200 bg-white shadow-sm' : 'border-gray-200 bg-white hover:border-red-100'}`}>
                  <input
                    type="checkbox"
                    checked={!!competitionMode}
                    onChange={handleCompetitionModeChange}
                    className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span>
                    <span className="block font-semibold text-gray-800">Competition Quiz</span>
                    <span className="text-gray-500">Enable strict quiz requirements for user-side proctoring.</span>
                  </span>
                </label>
              )}
            </div>
            {errors.competitionMode?.message && <p className="text-sm text-red-600">{errors.competitionMode.message}</p>}

            {competitionMode && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 border-t border-gray-200 pt-3">
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('requireFullScreen')} />
                  Full screen required
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('requireCamera')} />
                  Camera required
                </label>
                {/* Location required - Hidden for future update */}
                <div style={{ display: 'none' }}>
                  <label className="flex items-center gap-2 text-sm text-gray-700">
                    <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('requireLocation')} />
                    Location required
                  </label>

                  <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('preventTabSwitch')} />
                  Prevent tab switching
                </label>

                <label className="flex items-center gap-2 text-sm text-gray-700">
                  <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('requireMicrophone')} />
                  Audio required
                </label>

                </div>
              </div>
            )}
          </div>
          {practiceMode ? (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
              <p className="text-sm font-semibold text-amber-900">Certificates disabled for Practice Quiz</p>
              <p className="text-xs text-amber-700 mt-1">Practice quizzes are for learning, so certificate options are not available.</p>
            </div>
          ) : (
            <div className={`rounded-2xl border p-4 transition ${certificateEnabled ? 'border-indigo-200 bg-indigo-50/60 shadow-sm' : 'border-gray-200 bg-gray-50'}`}>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm font-semibold text-gray-900">Certificates</p>
                  <p className="text-xs text-gray-500 mt-1">Allow passed users to receive a downloadable certificate.</p>
                </div>
                <label className="inline-flex cursor-pointer items-center gap-2 rounded-full bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm ring-1 ring-gray-200">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    {...register('certificateEnabled')}
                  />
                  Enabled
                </label>
              </div>

              {certificateEnabled && (
                <div className="mt-4 space-y-4 border-t border-indigo-100 pt-4">
                  <Select
                    label="Certificate Template"
                    options={certificateTemplateOptions}
                    error={errors.certificateTemplateId?.message}
                    {...register('certificateTemplateId')}
                  />
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <label className="flex items-start gap-3 rounded-xl border border-white bg-white p-3 text-sm shadow-sm">
                      <input
                        type="checkbox"
                        className="mt-1 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        {...register('certificateAutoGenerate')}
                      />
                      <span>
                        <span className="block font-semibold text-gray-800">Auto generate for passed users</span>
                        <span className="text-xs text-gray-500">User side can show/download certificate automatically after eligibility.</span>
                      </span>
                    </label>
                    <div className="rounded-xl border border-white bg-white p-3 shadow-sm">
                      <Input
                        label="Generate After Completion (hours)"
                        type="number"
                        min="0"
                        max="168"
                        error={errors.certificateDelayHours?.message}
                        {...register('certificateDelayHours')}
                      />
                      <div className="mt-2 flex flex-wrap gap-2">
                        {[0, 6, 12, 24].map((hours) => (
                          <button
                            key={hours}
                            type="button"
                            onClick={() => setValue('certificateDelayHours', hours, { shouldDirty: true, shouldValidate: true })}
                            className="rounded-full border border-indigo-100 bg-indigo-50 px-3 py-1 text-xs font-medium text-indigo-700 hover:bg-indigo-100"
                          >
                            {hours === 0 ? 'Immediately' : `${hours}h`}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                  <p className="rounded-lg bg-white px-3 py-2 text-xs text-indigo-700 ring-1 ring-indigo-100">
                    Certificates are available only for users whose score is greater than or equal to the passing score.
                  </p>
                </div>
              )}
            </div>
          )}
          <Input label="Passing Score (%)" type="number" error={errors.passingScore?.message} {...register('passingScore')} />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit" loading={createMutation.isLoading || updateMutation.isLoading}>
              {editingQuiz ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteMutation.mutate(deleteId)}
        title="Delete Quiz"
        message="This quiz and all related data will be permanently removed."
        loading={deleteMutation.isLoading}
      />
    </div>
  )
}

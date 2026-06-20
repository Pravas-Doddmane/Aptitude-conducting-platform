import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { getQuestions, createQuestion, updateQuestion, deleteQuestion } from '../api/questionService'
import { getCategories } from '../api/categoryService'
import { generateQuestionsWithAi, generateQuestionsWithAiFromFile } from '../api/aiService'
import { Table } from '../components/ui/Table'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { useToast } from '../hooks/useToast'
import { Plus, Pencil, Trash2, MessageSquare, Search, Sparkles } from 'lucide-react'
import { EmptyState } from '../components/ui/EmptyState'
import { DIFFICULTY_LEVELS } from '../utils/constants'
import { uploadQuestionImage } from '../api/uploadService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const optionSchema = z.object({
  optionText: z.string().min(1, 'Option text required'),
  correct: z.boolean(),
  optionOrder: z.number(),
})

const schema = z.object({
  categoryId: z.string().min(1, 'Category is required'),
  stem: z.string().min(1, 'Question stem is required'),
  explanation: z.string().optional(),
  imageUrl: z.string().optional().nullable(),
  difficultyLevel: z.enum(['EASY', 'MEDIUM', 'HARD']),
  options: z.array(optionSchema).min(2).max(6),
})

const aiSchema = z.object({
  categoryId: z.string().min(1, 'Category is required'),
  topic: z.string().min(1, 'Topic is required'),
  difficultyLevel: z.enum(['EASY', 'MEDIUM', 'HARD']),
  questionCount: z.coerce.number().int().min(1).max(20),
  optionsPerQuestion: z.coerce.number().int().min(2).max(6),
})

const getErrorMessage = (error, fallback = 'Something went wrong') => {
  const validationMessages = error.response?.data?.messages
  if (validationMessages) {
    return Object.values(validationMessages).join(', ')
  }
  return error.response?.data?.message || error.message || fallback
}

export const Questions = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isAiModalOpen, setIsAiModalOpen] = useState(false)
  const [editingQuestion, setEditingQuestion] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [uploadingImage, setUploadingImage] = useState(false)
  const [imageMode, setImageMode] = useState('url')
  const [aiSource, setAiSource] = useState('prompt')
  const [aiFile, setAiFile] = useState(null)
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: questions, isLoading } = useQuery({
    queryKey: ['questions'],
    queryFn: () => getQuestions(),
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: () => getCategories(),
  })

  const categoryOptions = useMemo(
    () => (Array.isArray(categories) ? categories.map((category) => ({ value: category.id, label: category.name })) : []),
    [categories]
  )

  const categoryNameById = useMemo(
    () => new Map((Array.isArray(categories) ? categories : []).map((category) => [category.id, category.name])),
    [categories]
  )

  const filteredQuestions = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return questions || []
    return (questions || []).filter((question) => {
      const categoryName = question.categoryName || categoryNameById.get(question.categoryId) || ''
      const fields = [question.stem, categoryName, question.explanation, question.status, question.difficultyLevel]
      return fields.some((field) => field?.toLowerCase().includes(query))
    })
  }, [questions, search, categoryNameById])

  const createMutation = useMutation({
    mutationFn: createQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['questions'] })
      toast.success('Question created')
      setIsModalOpen(false)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateQuestion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['questions'] })
      toast.success('Question updated')
      setIsModalOpen(false)
      setEditingQuestion(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteQuestion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['questions'] })
      toast.success('Question deleted')
      setDeleteId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const createBlankOption = (optionOrder, correct = false) => ({
    optionText: '',
    correct,
    optionOrder,
  })

  const aiGenerateMutation = useMutation({
    mutationFn: ({ payload, file }) => file
      ? generateQuestionsWithAiFromFile({ payload, file })
      : generateQuestionsWithAi(payload),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['questions'] })
      toast.success(`AI created ${response.createdCount} question${response.createdCount === 1 ? '' : 's'}`)
      if (response.skippedDuplicateCount > 0) {
        toast.error(`${response.skippedDuplicateCount} duplicate question${response.skippedDuplicateCount === 1 ? '' : 's'} skipped`)
      }
      setIsAiModalOpen(false)
      setAiSource('prompt')
      setAiFile(null)
      resetAi()
    },
    onError: (err) => toast.error(getErrorMessage(err, 'AI generation failed')),
  })

  const defaultOptions = [
    createBlankOption(1, true),
    createBlankOption(2, false),
  ]

  const { register, handleSubmit, control, formState: { errors }, reset, watch, setValue } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { categoryId: '', stem: '', explanation: '', imageUrl: '', difficultyLevel: 'EASY', options: defaultOptions },
  })

  const {
    register: registerAi,
    handleSubmit: handleAiSubmit,
    formState: { errors: aiErrors },
    reset: resetAi,
  } = useForm({
    resolver: zodResolver(aiSchema),
    defaultValues: {
      categoryId: '',
      topic: '',
      difficultyLevel: 'EASY',
      questionCount: 5,
      optionsPerQuestion: 4,
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'options' })

  const openCreate = () => {
    setEditingQuestion(null)
    setUploadingImage(false)
    setImageMode('url')
    reset({ categoryId: '', stem: '', explanation: '', imageUrl: '', difficultyLevel: 'EASY', options: defaultOptions })
    setIsModalOpen(true)
  }

  const openAiGenerate = () => {
    setAiSource('prompt')
    setAiFile(null)
    resetAi({
      categoryId: '',
      topic: '',
      difficultyLevel: 'EASY',
      questionCount: 5,
      optionsPerQuestion: 4,
    })
    setIsAiModalOpen(true)
  }

  const openEdit = (q) => {
    setEditingQuestion(q)
    setUploadingImage(false)
    setImageMode(q.imageUrl?.includes('/uploads/') ? 'upload' : 'url')
    const options = q.options?.map((opt, idx) => ({
      optionText: opt.optionText,
      correct: opt.correct,
      optionOrder: opt.optionOrder || idx + 1,
    })) || defaultOptions
    reset({
      categoryId: q.categoryId || '',
      stem: q.stem,
      explanation: q.explanation || '',
      imageUrl: q.imageUrl || '',
      difficultyLevel: q.difficultyLevel || 'EASY',
      options,
    })
    setIsModalOpen(true)
  }

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    try {
      setUploadingImage(true)
      setImageMode('upload')
      const response = await uploadQuestionImage(file)
      const imageUrl = response.imageUrl?.startsWith('http')
        ? response.imageUrl
        : `${API_BASE_URL}${response.imageUrl}`
      setValue('imageUrl', imageUrl, { shouldDirty: true, shouldValidate: true })
      toast.success('Image uploaded')
    } catch (error) {
      toast.error(error.response?.data?.message || 'Image upload failed')
      event.target.value = ''
    } finally {
      setUploadingImage(false)
    }
  }

  const onSubmit = (formData) => {
    const normalizedOptions = formData.options.map((option, index) => ({
      ...option,
      optionOrder: index + 1,
    }))
    // Ensure exactly one correct
    const correctCount = normalizedOptions.filter(o => o.correct).length
    if (correctCount !== 1) {
      toast.error('Exactly one option must be marked correct.')
      return
    }
    if (editingQuestion) {
      updateMutation.mutate({ id: editingQuestion.id, data: { ...formData, options: normalizedOptions } })
    } else {
      createMutation.mutate({ ...formData, options: normalizedOptions })
    }
  }

  const onAiSubmit = (formData) => {
    if (aiSource === 'file' && !aiFile) {
      toast.error('Upload a PDF, photo, or text file for AI generation.')
      return
    }
    aiGenerateMutation.mutate({ payload: formData, file: aiSource === 'file' ? aiFile : null })
  }

  const onAiInvalid = () => {
    toast.error('Select a category and fill all AI generation fields.')
  }

  const handleImageModeChange = (nextMode) => {
    setImageMode(nextMode)
    setValue('imageUrl', '', { shouldDirty: true, shouldValidate: true })
  }

  const difficultyBadge = (level) => {
    switch (level) {
      case 'EASY': return <Badge variant="success">Easy</Badge>
      case 'MEDIUM': return <Badge variant="warning">Medium</Badge>
      case 'HARD': return <Badge variant="danger">Hard</Badge>
      default: return <Badge>{level}</Badge>
    }
  }

  const columns = [
    { header: 'Stem', render: (row) => <span className="line-clamp-2">{row.stem}</span> },
    { header: 'Category', render: (row) => row.categoryName || categoryNameById.get(row.categoryId) || '—' },
    {
      header: 'Difficulty',
      render: (row) => difficultyBadge(row.difficultyLevel),
    },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" onClick={() => openEdit(row)}><Pencil className="w-4 h-4" /></Button>
          <Button variant="ghost" size="sm" onClick={() => setDeleteId(row.id)}><Trash2 className="w-4 h-4 text-red-500" /></Button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Questions</h1>
          <p className="text-sm text-gray-500 mt-1">Manage quiz questions and options</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={openAiGenerate} disabled={categoryOptions.length === 0}>
            <Sparkles className="w-4 h-4 mr-2" /> Generate with AI
          </Button>
          <Button onClick={openCreate}><Plus className="w-4 h-4 mr-2" /> Add Question</Button>
        </div>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search by stem or category..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {!isLoading && filteredQuestions.length === 0 ? (
        <EmptyState
          title="No questions found"
          description={search ? 'No questions match your search.' : 'Create your first question.'}
          icon={MessageSquare}
        />
      ) : (
        <Table columns={columns} data={filteredQuestions} loading={isLoading} />
      )}

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} size="lg" title={editingQuestion ? 'Edit Question' : 'Create Question'}>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Select label="Category" options={categoryOptions} error={errors.categoryId?.message} {...register('categoryId')} />
          <Input label="Question Stem" error={errors.stem?.message} {...register('stem')} />
          <Input label="Explanation" error={errors.explanation?.message} {...register('explanation')} />
<div className="space-y-3">
  <label className="block text-sm font-medium text-gray-700">
    Upload Image from Device
  </label>

  <input
    type="file"
    accept="image/*"
    onChange={handleFileUpload}
    className="block w-full text-sm text-gray-600 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-4 file:py-2 file:text-indigo-700 hover:file:bg-indigo-100"
  />

  {uploadingImage && (
    <p className="text-xs text-gray-500">Uploading image...</p>
  )}

  {watch('imageUrl') && (
    <div className="rounded-xl border border-gray-200 p-3">
      <p className="text-xs font-medium text-gray-500 mb-2">Preview</p>
      <img
        src={watch('imageUrl')}
        alt="Question preview"
        className="max-h-40 rounded-lg object-contain bg-gray-50"
      />
    </div>
  )}

  {/*
    Future Image URL Support

    const [imageMode, setImageMode] = useState('upload');

    <p className="block text-sm font-medium text-gray-700">
      Image Source (choose one)
    </p>

    <div className="flex flex-wrap gap-4 text-sm">
      <label className="flex items-center gap-2">
        <input
          type="radio"
          checked={imageMode === 'upload'}
          onChange={() => handleImageModeChange('upload')}
        />
        Upload from device
      </label>

      <label className="flex items-center gap-2">
        <input
          type="radio"
          checked={imageMode === 'url'}
          onChange={() => handleImageModeChange('url')}
        />
        Use image URL
      </label>
    </div>

    {imageMode === 'url' && (
      <Input
        label="Image URL (optional)"
        error={errors.imageUrl?.message}
        {...register('imageUrl')}
      />
    )}
  */}
</div>
          <Select label="Difficulty" options={DIFFICULTY_LEVELS.map(d => ({ value: d, label: d }))} error={errors.difficultyLevel?.message} {...register('difficultyLevel')} />

          <div>
            <div className="flex items-center justify-between gap-3 mb-2">
              <p className="text-sm font-medium text-gray-700">Options (2 to 6, one correct)</p>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={() => {
                  if (fields.length >= 6) return
                  append(createBlankOption(fields.length + 1, false))
                }}
              >
                Add Option
              </Button>
            </div>
            {fields.map((field, index) => (
              <div key={field.id} className="flex items-center gap-3 mb-2">
                <span className="text-xs text-gray-500 w-6">{index + 1}.</span>
                <input
                  {...register(`options.${index}.optionText`)}
                  placeholder={`Option ${index + 1}`}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <label className="flex items-center gap-1 text-sm">
                  <input
                    type="radio"
                    checked={!!watch(`options.${index}.correct`)}
                    onChange={() => {
                      fields.forEach((field, i) => {
                        setValue(`options.${i}.correct`, i === index, {
                          shouldDirty: true,
                          shouldValidate: true,
                        })
                      })
                    }}
                    className="text-indigo-600 focus:ring-indigo-500"
                  />
                  Correct
                </label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  disabled={fields.length <= 2}
                  onClick={() => {
                    const removedWasCorrect = watch(`options.${index}.correct`)
                    remove(index)
                    if (removedWasCorrect) {
                      setValue('options.0.correct', true, {
                        shouldDirty: true,
                        shouldValidate: true,
                      })
                    }
                  }}
                >
                  Remove
                </Button>
              </div>
            ))}
            {errors.options && <p className="text-xs text-red-500">Each question must have 2 to 6 options, and exactly one must be correct.</p>}
          </div>

          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit" loading={createMutation.isLoading || updateMutation.isLoading}>
              {editingQuestion ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal open={isAiModalOpen} onClose={() => setIsAiModalOpen(false)} title="Generate Questions with AI">
        <form onSubmit={handleAiSubmit(onAiSubmit, onAiInvalid)} className="space-y-4">
          <Select label="Category" options={categoryOptions} error={aiErrors.categoryId?.message} {...registerAi('categoryId')} />
          <Input label="Topic" error={aiErrors.topic?.message} {...registerAi('topic')} />
          <div className="space-y-3 rounded-xl border border-gray-200 bg-gray-50 p-3">
            <p className="text-sm font-medium text-gray-700">AI Source</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-sm">
              <label className={`flex cursor-pointer items-start gap-2 rounded-lg border p-3 ${aiSource === 'prompt' ? 'border-indigo-500 bg-white' : 'border-gray-200 bg-white'}`}>
                <input
                  type="radio"
                  checked={aiSource === 'prompt'}
                  onChange={() => {
                    setAiSource('prompt')
                    setAiFile(null)
                  }}
                  className="mt-1"
                />
                <span>
                  <span className="block font-medium text-gray-800">Use topic only</span>
                  <span className="text-xs text-gray-500">AI creates new questions from the selected category and topic.</span>
                </span>
              </label>
              <label className={`flex cursor-pointer items-start gap-2 rounded-lg border p-3 ${aiSource === 'file' ? 'border-indigo-500 bg-white' : 'border-gray-200 bg-white'}`}>
                <input
                  type="radio"
                  checked={aiSource === 'file'}
                  onChange={() => setAiSource('file')}
                  className="mt-1"
                />
                <span>
                  <span className="block font-medium text-gray-800">Photo</span>
                  <span className="text-xs text-gray-500">AI reads the file and creates category-matching questions.</span>
                </span>
              </label>
            </div>
            {aiSource === 'file' && (
              <div className="space-y-2">
                <input
                  type="file"
                  accept="application/pdf,image/*,text/plain"
                  onChange={(event) => setAiFile(event.target.files?.[0] || null)}
                  className="block w-full text-sm text-gray-600 file:mr-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-4 file:py-2 file:text-indigo-700 hover:file:bg-indigo-100"
                />
                <p className="text-xs text-gray-500">
                  If the file does not match the selected category, AI will stop and ask you to choose the correct category or upload the correct file.
                </p>
                {aiFile && <p className="text-xs font-medium text-gray-700">Selected: {aiFile.name}</p>}
              </div>
            )}
          </div>
          <Select
            label="Difficulty"
            options={DIFFICULTY_LEVELS.map(difficulty => ({ value: difficulty, label: difficulty }))}
            error={aiErrors.difficultyLevel?.message}
            {...registerAi('difficultyLevel')}
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label="Number of Questions"
              type="number"
              min="1"
              max="20"
              error={aiErrors.questionCount?.message}
              {...registerAi('questionCount')}
            />
            <Input
              label="Options per Question"
              type="number"
              min="2"
              max="6"
              error={aiErrors.optionsPerQuestion?.message}
              {...registerAi('optionsPerQuestion')}
            />
          </div>
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Create categories manually first. AI will only add questions to the selected category and skip repeated question stems.
          </div>
          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setIsAiModalOpen(false)} disabled={aiGenerateMutation.isPending}>Cancel</Button>
            <Button type="submit" loading={aiGenerateMutation.isPending}>
              <Sparkles className="w-4 h-4 mr-2" /> Generate
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteMutation.mutate(deleteId)}
        title="Delete Question"
        message="Are you sure? This question will be removed permanently."
        loading={deleteMutation.isLoading}
      />
    </div>
  )
}

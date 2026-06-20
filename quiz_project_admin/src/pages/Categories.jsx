import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../api/categoryService'
import { Table } from '../components/ui/Table'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Input } from '../components/ui/Input'
import { useToast } from '../hooks/useToast'
import { Plus, Pencil, Trash2, FolderTree, Search } from 'lucide-react'
import { EmptyState } from '../components/ui/EmptyState'

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
})

export const Categories = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: categories, isLoading } = useQuery({
    queryKey: ['categories'],
    queryFn: () => getCategories(),
  })

  const createMutation = useMutation({
    mutationFn: createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast.success('Category created')
      setIsModalOpen(false)
      reset()
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast.success('Category updated')
      setIsModalOpen(false)
      setEditingCategory(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      toast.success('Category deleted')
      setDeleteId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const { register, handleSubmit, formState: { errors }, reset } = useForm({
    resolver: zodResolver(schema),
  })

  const openCreate = () => {
    setEditingCategory(null)
    reset({ name: '', description: '' })
    setIsModalOpen(true)
  }

  const openEdit = (cat) => {
    setEditingCategory(cat)
    reset({
      name: cat.name,
      description: cat.description || '',
    })
    setIsModalOpen(true)
  }

  const onSubmit = (formData) => {
    if (editingCategory) {
      updateMutation.mutate({ id: editingCategory.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const filteredCategories = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return categories || []
    return (categories || []).filter((category) => {
      const fields = [
        category.name,
        category.description,
        String(category.questionCount ?? ''),
        String(category.easyQuestionCount ?? ''),
        String(category.mediumQuestionCount ?? ''),
        String(category.hardQuestionCount ?? ''),
      ]
      return fields.some((field) => field?.toLowerCase().includes(query))
    })
  }, [categories, search])

  const columns = [
    { header: 'Name', accessor: 'name' },
    { header: 'Description', accessor: 'description', render: (row) => row.description || '—' },
    { header: 'Total', render: (row) => row.questionCount ?? 0 },
    { header: 'Easy', render: (row) => row.easyQuestionCount ?? 0 },
    { header: 'Medium', render: (row) => row.mediumQuestionCount ?? 0 },
    { header: 'Hard', render: (row) => row.hardQuestionCount ?? 0 },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-2">
          <Button variant="ghost" size="sm" onClick={() => openEdit(row)}>
            <Pencil className="w-4 h-4" />
          </Button>
          <Button variant="ghost" size="sm" onClick={() => setDeleteId(row.id)}>
            <Trash2 className="w-4 h-4 text-red-500" />
          </Button>
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
          <p className="text-sm text-gray-500 mt-1">Manage quiz categories</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="w-4 h-4 mr-2" /> Add Category
        </Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search by name or description..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {!isLoading && filteredCategories.length === 0 ? (
        <EmptyState
          title="No categories found"
          description={search ? 'No categories match your search.' : 'Create your first category to get started.'}
          icon={FolderTree}
        />
      ) : (
        <Table columns={columns} data={filteredCategories} loading={isLoading} />
      )}

      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingCategory ? 'Edit Category' : 'Create Category'}
      >
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input label="Name" error={errors.name?.message} {...register('name')} />
          <Input label="Description" error={errors.description?.message} {...register('description')} />
          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit" loading={createMutation.isLoading || updateMutation.isLoading}>
              {editingCategory ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteMutation.mutate(deleteId)}
        title="Delete Category"
        message="Are you sure you want to delete this category? This action cannot be undone."
        loading={deleteMutation.isLoading}
      />
    </div>
  )
}

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Award, Eye, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { getCertificateTemplates, createCertificateTemplate, updateCertificateTemplate, deleteCertificateTemplate } from '../api/certificateTemplateService'
import { Button } from '../components/ui/Button'
import { Modal } from '../components/ui/Modal'
import { Table } from '../components/ui/Table'
import { Badge } from '../components/ui/Badge'
import { Input } from '../components/ui/Input'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { EmptyState } from '../components/ui/EmptyState'
import { useToast } from '../hooks/useToast'

const defaultHtml = `<div class="certificate">
  <img class="certificate-logo" src="{{logoUrl}}" alt="Certificate logo" />
  <div class="subtitle">Certificate of Achievement</div>
  <h1>{{fullName}}</h1>
  <p>has successfully passed</p>
  <h2>{{quizTitle}}</h2>
  <p class="meta">Category: {{categoryName}}</p>
  <p class="score">Score: {{score}} / {{maxScore}} ({{scorePercent}}%)</p>
  <p class="date">Issued on {{issuedAt}}</p>
</div>`

const defaultCss = `.certificate {
  padding: 56px;
  text-align: center;
  border: 12px solid #4f46e5;
  position: relative;
  color: #111827;
  background: linear-gradient(135deg, #ffffff, #eef2ff);
}
.certificate-logo { position: absolute; top: 22px; right: 26px; width: 96px; max-height: 72px; object-fit: contain; }
.certificate-logo[src=""] { display: none; }
.subtitle { font-size: 24px; letter-spacing: 3px; text-transform: uppercase; color: #4f46e5; }
h1 { font-size: 52px; margin: 46px 0 20px; font-family: Georgia, serif; }
h2 { font-size: 34px; margin: 18px 0; }
p { font-size: 20px; margin: 10px 0; }
.score { font-weight: bold; color: #047857; }
.date { margin-top: 46px; font-size: 18px; color: #6b7280; }`

const sampleValues = {
  firstName: 'Full',
  lastName: 'Name',
  fullName: 'Username',
  email: 'student@example.com',
  logoUrl: '',
  quizTitle: 'Quiz',
  categoryName: 'Quiz',
  score: '8',
  maxScore: '10',
  scorePercent: '80.00',
  issuedAt: '15 Jun 2026',
}

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().optional(),
  logoUrl: z.string().url('Enter a valid image URL').optional().or(z.literal('')),
  htmlTemplate: z.string().min(1, 'HTML template is required'),
  cssTemplate: z.string().optional(),
  active: z.boolean().optional().default(true),
})

const renderSample = (value = '') => {
  let rendered = value
  Object.entries(sampleValues).forEach(([key, sample]) => {
    rendered = rendered.replaceAll(`{{${key}}}`, sample)
  })
  return rendered
}

export const CertificateTemplates = () => {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isReviewOpen, setIsReviewOpen] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: templates, isLoading } = useQuery({
    queryKey: ['certificateTemplates'],
    queryFn: getCertificateTemplates,
  })

  const filteredTemplates = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return templates || []
    return (templates || []).filter((template) =>
      [template.name, template.description, template.logoUrl].some((field) => field?.toLowerCase().includes(query))
    )
  }, [templates, search])

  const { register, handleSubmit, formState: { errors }, reset, watch } = useForm({
    resolver: zodResolver(schema),
  })

  const createMutation = useMutation({
    mutationFn: createCertificateTemplate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['certificateTemplates'] })
      toast.success('Certificate template created')
      setIsModalOpen(false)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => updateCertificateTemplate(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['certificateTemplates'] })
      toast.success('Certificate template updated')
      setIsModalOpen(false)
      setEditingTemplate(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCertificateTemplate,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['certificateTemplates'] })
      toast.success('Certificate template disabled')
      setDeleteId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const openCreate = () => {
    setEditingTemplate(null)
    reset({ name: '', description: '', logoUrl: '', htmlTemplate: defaultHtml, cssTemplate: defaultCss, active: true })
    setIsModalOpen(true)
  }

  const openEdit = (template) => {
    setEditingTemplate(template)
    reset({
      name: template.name,
      description: template.description || '',
      logoUrl: template.logoUrl || '',
      htmlTemplate: template.htmlTemplate || defaultHtml,
      cssTemplate: template.cssTemplate || '',
      active: template.active,
    })
    setIsModalOpen(true)
  }

  const onSubmit = (formData) => {
    if (editingTemplate) {
      updateMutation.mutate({ id: editingTemplate.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const columns = [
    { header: 'Name', accessor: 'name' },
    { header: 'Description', render: (row) => row.description || '—' },
    { header: 'Logo', render: (row) => row.logoUrl ? <Badge variant="info">Added</Badge> : <Badge variant="neutral">None</Badge> },
    { header: 'Status', render: (row) => row.active ? <Badge variant="success">Active</Badge> : <Badge variant="neutral">Inactive</Badge> },
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

  const previewHtml = renderSample(watch('htmlTemplate') || '')
  const previewCss = renderSample(watch('cssTemplate') || '')
  const previewDocument = `<!doctype html><html><head><style>html,body{height:100%;margin:0;overflow:hidden;}body{padding:4px;background:#f8fafc;font-family:Arial,sans-serif;box-sizing:border-box;}*{box-sizing:border-box;} ${previewCss} .preview-page{width:100%;height:100%;overflow:hidden;background:white;border-radius:14px;box-shadow:0 8px 18px rgba(15,23,42,.10);display:flex;align-items:center;justify-content:center;padding:8px;}.preview-page>.certificate{width:100%!important;height:100%!important;min-height:0!important;max-height:100%!important;margin:0!important;padding:12px 18px!important;overflow:hidden!important;display:flex!important;flex-direction:column!important;align-items:center!important;justify-content:space-evenly!important;border-width:8px!important;}.preview-page>.certificate .subtitle{font-size:clamp(24px,4vw,44px)!important;margin:0!important;line-height:1.05!important;}.preview-page>.certificate h1{font-size:clamp(56px,8vw,96px)!important;margin:0!important;line-height:1!important;}.preview-page>.certificate h2{font-size:clamp(34px,5vw,62px)!important;margin:0!important;line-height:1.05!important;}.preview-page>.certificate p{font-size:clamp(20px,3vw,34px)!important;margin:0!important;line-height:1.1!important;}.preview-page>.certificate .date{margin:0!important;}</style></head><body><div class="preview-page">${previewHtml}</div></body></html>`

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Certificate Templates</h1>
          <p className="text-sm text-gray-500 mt-1">Create HTML/CSS templates for passed quiz certificates</p>
        </div>
        <Button onClick={openCreate}><Plus className="w-4 h-4 mr-2" /> Add Template</Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search templates..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {!isLoading && filteredTemplates.length === 0 ? (
        <EmptyState title="No certificate templates found" description="Create your first certificate template." icon={Award} />
      ) : (
        <Table columns={columns} data={filteredTemplates} loading={isLoading} />
      )}

      <Modal open={isModalOpen} onClose={() => setIsModalOpen(false)} size="xl" title={editingTemplate ? 'Edit Certificate Template' : 'Create Certificate Template'}>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <Input label="Template Name" error={errors.name?.message} {...register('name')} />
          <Input label="Description" error={errors.description?.message} {...register('description')} />
          {/* Logo upload feature - Hidden for future update */}
          <div style={{ display: 'none' }}>
            <Input
              label="Logo Image URL (optional)"
              placeholder="https://example.com/company-logo.png"
              error={errors.logoUrl?.message}
              {...register('logoUrl')}
            />
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">HTML Template</label>
                <textarea {...register('htmlTemplate')} rows={12} className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                {errors.htmlTemplate && <p className="text-xs text-red-500 mt-1">{errors.htmlTemplate.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">CSS Template</label>
                <textarea {...register('cssTemplate')} rows={10} className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500" {...register('active')} />
                Active
              </label>
            </div>
            <div>
              <div className="mb-2 flex items-center justify-between gap-3">
                <p className="text-sm font-medium text-gray-700">Preview</p>
                <Button type="button" variant="secondary" size="sm" onClick={() => setIsReviewOpen(true)}>
                  <Eye className="w-4 h-4 mr-2" /> Review
                </Button>
              </div>
              <div className="rounded-2xl border border-gray-200 bg-white p-2 overflow-hidden shadow-sm">
                <iframe
                  title="Certificate template preview"
                  srcDoc={previewDocument}
                  className="h-[300px] w-full rounded-xl bg-slate-50"
                />
              </div>
              <div className="mt-3 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs text-blue-800">
                Supported variables: {'{{firstName}}'}, {'{{lastName}}'}, {'{{fullName}}'}, {'{{email}}'}, {'{{logoUrl}}'}, {'{{quizTitle}}'}, {'{{categoryName}}'}, {'{{score}}'}, {'{{maxScore}}'}, {'{{scorePercent}}'}, {'{{issuedAt}}'}.
              </div>
            </div>
          </div>
          <div className="flex justify-end gap-3">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button type="submit" loading={createMutation.isLoading || updateMutation.isLoading}>{editingTemplate ? 'Update' : 'Create'}</Button>
          </div>
        </form>
      </Modal>

      <Modal open={isReviewOpen} onClose={() => setIsReviewOpen(false)} size="xl" title="Certificate Review">
        <div className="space-y-3">
          <div className="rounded-2xl border border-gray-200 bg-white p-3 shadow-sm">
            <iframe
              title="Full certificate review"
              srcDoc={previewDocument}
              className="h-[560px] w-full rounded-xl bg-slate-50"
            />
          </div>
          <div className="flex justify-end">
            <Button type="button" variant="secondary" onClick={() => setIsReviewOpen(false)}>Close</Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteMutation.mutate(deleteId)}
        title="Disable Template"
        message="This template will be marked inactive and hidden from new quiz certificate selections."
        loading={deleteMutation.isLoading}
      />
    </div>
  )
}

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState } from '../components/ui/EmptyState'
import { Skeleton } from '../components/ui/Skeleton'
import { Table } from '../components/ui/Table'
import { useToast } from '../hooks/useToast'
import { getFeedbacks, updateFeedbackStatus } from '../api/feedbackService'
import { MessageSquare, CheckCircle2, Sparkles, ShieldCheck, Search } from 'lucide-react'

const statusOptions = [
  { value: 'ALL', label: 'All' },
  { value: 'NEW', label: 'New' },
  { value: 'REVIEWED', label: 'Reviewed' },
  { value: 'RESOLVED', label: 'Resolved' },
]

const StatCard = ({ icon: Icon, label, value, color, loading }) => {
  if (loading) return <Skeleton className="h-24 rounded-xl" />
  return (
    <Card className="p-5 flex items-center gap-4">
      <div className={`p-3 rounded-xl bg-gradient-to-br ${color}`}>
        <Icon className="w-6 h-6 text-white" />
      </div>
      <div>
        <p className="text-sm font-medium text-gray-500">{label}</p>
        <p className="text-2xl font-semibold text-gray-900">{value}</p>
      </div>
    </Card>
  )
}

const formatDate = (value) => {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}

export const Feedback = () => {
  const [status, setStatus] = useState('ALL')
  const [search, setSearch] = useState('')
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: feedbacks, isLoading } = useQuery({
    queryKey: ['feedbacks', status],
    queryFn: () => getFeedbacks(status === 'ALL' ? undefined : { status }),
  })

  const reviewMutation = useMutation({
    mutationFn: ({ id, nextStatus }) => updateFeedbackStatus(id, { status: nextStatus }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feedbacks'] })
      toast.success('Feedback updated')
    },
    onError: (error) => toast.error(error.response?.data?.message || 'Error'),
  })

  const stats = useMemo(() => {
    const list = Array.isArray(feedbacks) ? feedbacks : []
    return {
      total: list.length,
      newCount: list.filter((item) => item.status === 'NEW').length,
      reviewedCount: list.filter((item) => item.status === 'REVIEWED').length,
      resolvedCount: list.filter((item) => item.status === 'RESOLVED').length,
    }
  }, [feedbacks])

  const filteredFeedbacks = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return feedbacks || []
    return (feedbacks || []).filter((feedback) => {
      const fields = [
        feedback.userName,
        feedback.email,
        feedback.quizTitle,
        feedback.questionStem,
        feedback.message,
        feedback.adminNote,
        feedback.status,
      ]
      return fields.some((field) => field?.toLowerCase().includes(query))
    })
  }, [feedbacks, search])

  const columns = [
    { header: 'User', render: (row) => row.userName || row.email },
    { header: 'Quiz', render: (row) => row.quizTitle || '—' },
    { header: 'Question', render: (row) => row.questionStem || '—' },
    { header: 'Rating', render: (row) => <Badge variant="info">{row.rating}/5</Badge> },
    {
      header: 'Status',
      render: (row) => (
        <Badge
          variant={
            row.status === 'NEW' ? 'warning' : row.status === 'REVIEWED' ? 'info' : 'success'
          }
        >
          {row.status}
        </Badge>
      ),
    },
    { header: 'Message', render: (row) => <span className="line-clamp-2">{row.message}</span> },
    { header: 'Submitted', render: (row) => formatDate(row.createdAt) },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-2">
          {row.status === 'NEW' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => reviewMutation.mutate({ id: row.id, nextStatus: 'REVIEWED' })}
            >
              Review
            </Button>
          )}
          {row.status !== 'RESOLVED' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => reviewMutation.mutate({ id: row.id, nextStatus: 'RESOLVED' })}
            >
              Resolve
            </Button>
          )}
          {row.status === 'RESOLVED' && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => reviewMutation.mutate({ id: row.id, nextStatus: 'REVIEWED' })}
            >
              Reopen
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-col xl:flex-row xl:items-end xl:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Feedback</h1>
          <p className="text-sm text-gray-500 mt-1">Review feedback about quizzes and questions</p>
        </div>
        <div className="w-full xl:max-w-2xl rounded-2xl border border-gray-200 bg-white/90 shadow-sm p-3">
          <div className="grid grid-cols-1 gap-3 items-end">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search by user, quiz, question, message..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-xl text-sm bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-gray-500">
            <span className="font-medium text-gray-600">Quick filters:</span>
            {statusOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setStatus(option.value)}
                className={`px-3 py-1 rounded-full border transition-colors ${
                  status === option.value
                    ? 'border-indigo-500 bg-indigo-50 text-indigo-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard icon={MessageSquare} label="Total" value={stats.total} color="from-indigo-500 to-blue-600" loading={isLoading} />
        <StatCard icon={Sparkles} label="New" value={stats.newCount} color="from-amber-500 to-orange-600" loading={isLoading} />
        <StatCard icon={CheckCircle2} label="Reviewed" value={stats.reviewedCount} color="from-cyan-500 to-teal-600" loading={isLoading} />
        <StatCard icon={ShieldCheck} label="Resolved" value={stats.resolvedCount} color="from-emerald-500 to-green-600" loading={isLoading} />
      </div>

      {!isLoading && filteredFeedbacks.length === 0 ? (
        <EmptyState
          title="No feedback found"
          description={search ? 'No feedback matches your search.' : 'Feedback from users will appear here.'}
          icon={MessageSquare}
        />
      ) : (
        <Table columns={columns} data={filteredFeedbacks} loading={isLoading} />
      )}
    </div>
  )
}

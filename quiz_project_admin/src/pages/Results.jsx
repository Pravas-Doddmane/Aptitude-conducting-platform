import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Award, Download, FileSpreadsheet, Search } from 'lucide-react'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState } from '../components/ui/EmptyState'
import { Table } from '../components/ui/Table'
import { useToast } from '../hooks/useToast'
import { getQuizParticipantResults, getResultSummaries } from '../api/resultService'

const filters = [
  { value: 'ALL', label: 'All' },
  { value: 'PASSED', label: 'Passed' },
  { value: 'FAILED', label: 'Failed' },
]

const formatPercent = (value) => `${Number(value || 0).toFixed(1)}%`

const formatSeconds = (value) => {
  const totalSeconds = Number(value || 0)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`
}

const escapeCsv = (value) => {
  const text = value === null || value === undefined ? '' : String(value)
  return `"${text.replaceAll('"', '""')}"`
}

const downloadCsv = (filename, rows) => {
  const csvContent = rows.map((row) => row.map(escapeCsv).join(',')).join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const toExportRows = (participants) => [
  ['First name', 'Last name', 'Email', 'Average speed', 'Total time taken', 'Total score', 'Result'],
  ...participants.map((participant) => [
    participant.firstName || '',
    participant.lastName || '',
    participant.email || '',
    `${Number(participant.averageSpeedSecondsPerQuestion || 0).toFixed(2)} sec/question`,
    formatSeconds(participant.totalTimeTakenSeconds),
    `${participant.score || 0}/${participant.maxScore || 0}`,
    participant.passed ? 'Passed' : 'Failed',
  ]),
]

export const Results = () => {
  const [search, setSearch] = useState('')
  const [selectedQuiz, setSelectedQuiz] = useState(null)
  const [activeFilter, setActiveFilter] = useState('ALL')
  const toast = useToast()

  const { data: results, isLoading } = useQuery({
    queryKey: ['result-summaries'],
    queryFn: () => getResultSummaries(),
  })

  const { data: participants, isFetching: participantsLoading } = useQuery({
    queryKey: ['result-participants', selectedQuiz?.quizId, activeFilter],
    queryFn: () => getQuizParticipantResults(selectedQuiz.quizId, activeFilter),
    enabled: Boolean(selectedQuiz?.quizId),
  })

  const filteredResults = useMemo(() => {
    const query = search.trim().toLowerCase()
    if (!query) return results || []
    return (results || []).filter((result) =>
      [result.quizTitle, result.categoryName]
        .filter(Boolean)
        .some((field) => field.toLowerCase().includes(query))
    )
  }, [results, search])

  const selectedParticipants = participants || []

  const handleSelectQuiz = (quiz) => {
    setSelectedQuiz(quiz)
    setActiveFilter('ALL')
  }

  const handleDownload = async (quiz, filter) => {
    const participantResults = selectedQuiz?.quizId === quiz.quizId && activeFilter === filter
      ? selectedParticipants
      : await getQuizParticipantResults(quiz.quizId, filter)

    if (!participantResults.length) {
      toast.error('No result data available to download')
      return
    }

    const safeTitle = quiz.quizTitle.replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, '').toLowerCase()
    downloadCsv(`${safeTitle || 'quiz'}-${filter.toLowerCase()}-results.csv`, toExportRows(participantResults))
    toast.success('Result file downloaded')
  }

  const columns = [
    { header: 'Quiz Title', accessor: 'quizTitle' },
    { header: 'Category', accessor: 'categoryName' },
    { header: 'Questions', render: (row) => row.questionCount ?? 0 },
    { header: 'Passing', render: (row) => `${row.passingScore ?? 0}%` },
    { header: 'Participants', render: (row) => row.totalParticipants ?? 0 },
    { header: 'Passed', render: (row) => <Badge variant="success">{row.passedParticipants ?? 0}</Badge> },
    { header: 'Failed', render: (row) => <Badge variant="danger">{row.failedParticipants ?? 0}</Badge> },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" onClick={() => handleSelectQuiz(row)}>
            View
          </Button>
          <Button variant="ghost" size="sm" onClick={() => handleDownload(row, 'ALL')}>
            <Download className="w-4 h-4 mr-1" />
            All
          </Button>
          <Button variant="ghost" size="sm" onClick={() => handleDownload(row, 'PASSED')}>
            Passed
          </Button>
          <Button variant="ghost" size="sm" onClick={() => handleDownload(row, 'FAILED')}>
            Failed
          </Button>
        </div>
      ),
    },
  ]

  const participantColumns = [
    { header: 'First Name', render: (row) => row.firstName || '-' },
    { header: 'Last Name', render: (row) => row.lastName || '-' },
    { header: 'Email', accessor: 'email' },
    { header: 'Average Speed', render: (row) => `${Number(row.averageSpeedSecondsPerQuestion || 0).toFixed(2)} sec/question` },
    { header: 'Total Time', render: (row) => formatSeconds(row.totalTimeTakenSeconds) },
    { header: 'Score', render: (row) => `${row.score || 0}/${row.maxScore || 0}` },
    { header: 'Percent', render: (row) => formatPercent(row.scorePercent) },
    { header: 'Result', render: (row) => <Badge variant={row.passed ? 'success' : 'danger'}>{row.passed ? 'Passed' : 'Failed'}</Badge> },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Results</h1>
          <p className="text-sm text-gray-500 mt-1">Review quiz outcomes and export participant result sheets</p>
        </div>
        <div className="relative w-full lg:w-80">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search by quiz or category..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {!isLoading && filteredResults.length === 0 ? (
        <EmptyState
          title="No results found"
          description={search ? 'No quiz results match your search.' : 'Completed quiz attempts will appear here.'}
          icon={FileSpreadsheet}
        />
      ) : (
        <Table columns={columns} data={filteredResults} loading={isLoading} />
      )}

      {selectedQuiz && (
        <Card className="p-5 space-y-4">
          <div className="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <Award className="w-5 h-5 text-indigo-600" />
                <h2 className="text-lg font-semibold text-gray-900">{selectedQuiz.quizTitle}</h2>
              </div>
              <p className="text-sm text-gray-500 mt-1">
                {selectedQuiz.categoryName} · {selectedQuiz.questionCount ?? 0} questions · Passing {selectedQuiz.passingScore ?? 0}%
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {filters.map((filter) => (
                <Button
                  key={filter.value}
                  type="button"
                  variant={activeFilter === filter.value ? 'primary' : 'secondary'}
                  size="sm"
                  onClick={() => setActiveFilter(filter.value)}
                >
                  {filter.label}
                </Button>
              ))}
              <Button type="button" variant="ghost" size="sm" onClick={() => handleDownload(selectedQuiz, activeFilter)}>
                <Download className="w-4 h-4 mr-1" />
                Download
              </Button>
            </div>
          </div>

          <Table
            columns={participantColumns}
            data={selectedParticipants}
            loading={participantsLoading}
            emptyMessage="No participant results for this filter."
          />
        </Card>
      )}
    </div>
  )
}

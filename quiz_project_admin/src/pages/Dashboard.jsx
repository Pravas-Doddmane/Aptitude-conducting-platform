import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  ClipboardList,
  Percent,
  Search,
  Trophy,
  Users,
  BookOpen,
  MessageSquare,
} from 'lucide-react'
import { Card } from '../components/ui/Card'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Skeleton } from '../components/ui/Skeleton'
import { Table } from '../components/ui/Table'
import { getDashboardAnalytics } from '../api/dashboardService'

const formatPercent = (value) => `${Number(value || 0).toFixed(1)}%`

const formatDateTime = (value) => {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}

const StatCard = ({ icon: Icon, label, value, subtext, loading, color }) => {
  if (loading) return <Skeleton className="h-28 rounded-xl" />
  return (
    <Card className="p-5 flex items-center gap-4">
      <div className={`p-3 rounded-xl bg-gradient-to-br ${color}`}>
        <Icon className="w-6 h-6 text-white" />
      </div>
      <div>
        <p className="text-sm font-medium text-gray-500">{label}</p>
        <p className="text-2xl font-semibold text-gray-900">{value}</p>
        {subtext && <p className="text-xs text-gray-500 mt-1">{subtext}</p>}
      </div>
    </Card>
  )
}

const BarRow = ({ label, value, max, accent = 'bg-indigo-500' }) => {
  const width = max > 0 ? Math.max((value / max) * 100, value > 0 ? 6 : 0) : 0
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-gray-800">{label}</span>
        <span className="text-gray-500">{value}</span>
      </div>
      <div className="h-2 rounded-full bg-gray-100 overflow-hidden">
        <div className={`h-full rounded-full ${accent}`} style={{ width: `${width}%` }} />
      </div>
    </div>
  )
}

const DifficultyStack = ({ easy = 0, medium = 0, hard = 0 }) => {
  const total = easy + medium + hard
  const easyWidth = total > 0 ? (easy / total) * 100 : 0
  const mediumWidth = total > 0 ? (medium / total) * 100 : 0
  const hardWidth = total > 0 ? (hard / total) * 100 : 0

  return (
    <div className="space-y-2">
      <div className="flex h-2 overflow-hidden rounded-full bg-gray-100">
        <div className="bg-emerald-500" style={{ width: `${easyWidth}%` }} />
        <div className="bg-amber-500" style={{ width: `${mediumWidth}%` }} />
        <div className="bg-rose-500" style={{ width: `${hardWidth}%` }} />
      </div>
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>E {easy}</span>
        <span>M {medium}</span>
        <span>H {hard}</span>
      </div>
    </div>
  )
}

const CompactSection = ({
  title,
  description,
  searchValue,
  onSearchChange,
  onSearchSubmit,
  searchPlaceholder,
  isExpanded,
  onToggleExpanded,
  loading,
  emptyMessage,
  totalCount,
  visibleCount,
  children,
}) => {
  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
          <p className="text-sm text-gray-500">{description}</p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault()
              onSearchSubmit?.()
            }}
          >
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <Input
                value={searchValue}
                onChange={(event) => onSearchChange(event.target.value)}
                placeholder={searchPlaceholder}
                className="pl-9 w-full sm:w-64"
              />
            </div>
            <Button type="submit" variant="secondary" size="sm" className="whitespace-nowrap">
              Search
            </Button>
          </form>

          {totalCount > 2 && (
            <Button type="button" variant="ghost" size="sm" onClick={onToggleExpanded} className="whitespace-nowrap">
              {isExpanded ? (
                <>
                  <ChevronUp className="w-4 h-4 mr-1" />
                  Show less
                </>
              ) : (
                <>
                  <ChevronDown className="w-4 h-4 mr-1" />
                  Show more
                </>
              )}
            </Button>
          )}
        </div>
      </div>

      {totalCount > 0 && (
        <p className="text-xs text-gray-500">
          Showing {visibleCount} of {totalCount}
        </p>
      )}

      {loading ? (
        <Skeleton className="h-44 rounded-xl" />
      ) : totalCount === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200/60 p-8 text-center">
          <p className="text-gray-500">{emptyMessage}</p>
        </div>
      ) : (
        children
      )}
    </div>
  )
}

export const Dashboard = () => {
  const [leaderboardSearch, setLeaderboardSearch] = useState('')
  const [leaderboardQuery, setLeaderboardQuery] = useState('')
  const [categoriesSearch, setCategoriesSearch] = useState('')
  const [categoriesQuery, setCategoriesQuery] = useState('')
  const [quizzesSearch, setQuizzesSearch] = useState('')
  const [quizzesQuery, setQuizzesQuery] = useState('')
  const [attemptsSearch, setAttemptsSearch] = useState('')
  const [attemptsQuery, setAttemptsQuery] = useState('')
  const [leaderboardExpanded, setLeaderboardExpanded] = useState(false)
  const [categoriesExpanded, setCategoriesExpanded] = useState(false)
  const [quizzesExpanded, setQuizzesExpanded] = useState(false)
  const [attemptsExpanded, setAttemptsExpanded] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-analytics'],
    queryFn: getDashboardAnalytics,
  })

  const totals = data?.totals || {}
  const leaderboard = data?.leaderboard || []
  const categories = data?.categories || []
  const quizzes = data?.quizzes || []
  const recentAttempts = data?.recentAttempts || []

  const filteredLeaderboard = useMemo(() => {
    const query = leaderboardQuery.trim().toLowerCase()
    if (!query) return leaderboard
    return leaderboard.filter((row) =>
      [row.name, row.email]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query))
    )
  }, [leaderboard, leaderboardQuery])

  const filteredCategories = useMemo(() => {
    const query = categoriesQuery.trim().toLowerCase()
    if (!query) return categories
    return categories.filter((row) =>
      [row.categoryName, row.description]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query))
    )
  }, [categories, categoriesQuery])

  const filteredQuizzes = useMemo(() => {
    const query = quizzesQuery.trim().toLowerCase()
    if (!query) return quizzes
    return quizzes.filter((row) =>
      [row.title, row.categoryName, row.status]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query))
    )
  }, [quizzes, quizzesQuery])

  const filteredAttempts = useMemo(() => {
    const query = attemptsQuery.trim().toLowerCase()
    if (!query) return recentAttempts
    return recentAttempts.filter((row) =>
      [row.userName, row.email, row.quizTitle, row.status]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query))
    )
  }, [recentAttempts, attemptsQuery])

  const attemptsRate = totals.attempts > 0 ? (totals.completedAttempts / totals.attempts) * 100 : 0
  const topCategory = filteredCategories[0] || categories[0]
  const topUser = filteredLeaderboard[0] || leaderboard[0]
  const topQuiz = filteredQuizzes[0] || quizzes[0]
  const maxCategoryAttempts = Math.max(...filteredCategories.map((category) => category.attemptCount || 0), 1)
  const visibleLeaderboard = leaderboardExpanded ? filteredLeaderboard : filteredLeaderboard.slice(0, 2)
  const visibleCategories = categoriesExpanded ? filteredCategories : filteredCategories.slice(0, 2)
  const visibleQuizzes = quizzesExpanded ? filteredQuizzes : filteredQuizzes.slice(0, 2)
  const visibleAttempts = attemptsExpanded ? filteredAttempts : filteredAttempts.slice(0, 2)

  const leaderboardColumns = [
    { header: 'Rank', render: (_, index) => <span>{index + 1}</span> },
    { header: 'User', render: (row) => row.name || row.email },
    { header: 'Avg Score', render: (row) => formatPercent(row.averageScorePercent) },
    { header: 'Attempts', accessor: 'completedAttempts' },
    { header: 'Best', accessor: 'bestScore' },
  ]

  const topQuizColumns = [
    { header: 'Quiz', accessor: 'title' },
    { header: 'Category', accessor: 'categoryName' },
    { header: 'Attempts', accessor: 'attemptCount' },
    { header: 'Avg Score', render: (row) => formatPercent(row.averageScorePercent) },
    {
      header: 'Status',
      render: (row) => (
        <Badge variant={row.status === 'PUBLISHED' ? 'success' : 'warning'}>{row.status}</Badge>
      ),
    },
  ]

  const recentColumns = [
    { header: 'User', render: (row) => row.userName || row.email },
    { header: 'Quiz', accessor: 'quizTitle' },
    { header: 'Score', render: (row) => `${row.score}/${row.maxScore} (${formatPercent(row.scorePercent)})` },
    { header: 'Submitted', render: (row) => formatDateTime(row.submittedAt) },
    { header: 'Status', render: (row) => <Badge variant="info">{row.status}</Badge> },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">Analytics, leaderboard, and category performance</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-6 gap-4">
        <StatCard
          icon={BookOpen}
          label="Categories"
          value={isLoading ? '—' : totals.categories ?? 0}
          subtext={`${totals.activeCategories ?? 0} active`}
          loading={isLoading}
          color="from-indigo-500 to-blue-600"
        />
        <StatCard
          icon={Trophy}
          label="Quizzes"
          value={isLoading ? '—' : totals.quizzes ?? 0}
          subtext={`${totals.publishedQuizzes ?? 0} published`}
          loading={isLoading}
          color="from-cyan-500 to-teal-600"
        />
        <StatCard
          icon={ClipboardList}
          label="Questions"
          value={isLoading ? '—' : totals.questions ?? 0}
          subtext={`${totals.activeQuestions ?? 0} active`}
          loading={isLoading}
          color="from-amber-500 to-orange-600"
        />
        <StatCard
          icon={Users}
          label="Users"
          value={isLoading ? '—' : totals.users ?? 0}
          subtext={`${totals.completedAttempts ?? 0} completed attempts`}
          loading={isLoading}
          color="from-emerald-500 to-green-600"
        />
        <StatCard
          icon={MessageSquare}
          label="Feedback"
          value={isLoading ? '—' : totals.feedbacks ?? 0}
          subtext="All submitted feedback"
          loading={isLoading}
          color="from-sky-500 to-cyan-600"
        />
        <StatCard
          icon={CheckCircle2}
          label="Completion Rate"
          value={isLoading ? '—' : formatPercent(attemptsRate)}
          subtext={`${totals.attempts ?? 0} total attempts`}
          loading={isLoading}
          color="from-pink-500 to-rose-600"
        />
        <StatCard
          icon={Percent}
          label="Avg Score"
          value={isLoading ? '—' : formatPercent(totals.averageScorePercent)}
          subtext="Across completed attempts"
          loading={isLoading}
          color="from-violet-500 to-purple-600"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <Card className="p-6 xl:col-span-2">
          <CompactSection
            title="Leaderboard"
            description="Users ranked by completed attempt score percentage"
            searchValue={leaderboardSearch}
            onSearchChange={setLeaderboardSearch}
            onSearchSubmit={() => setLeaderboardQuery(leaderboardSearch)}
            searchPlaceholder="Search name or email..."
            isExpanded={leaderboardExpanded}
            onToggleExpanded={() => setLeaderboardExpanded((value) => !value)}
            loading={isLoading}
            emptyMessage="No completed attempts yet."
            totalCount={filteredLeaderboard.length}
            visibleCount={visibleLeaderboard.length}
          >
            <Table
              columns={leaderboardColumns}
              data={visibleLeaderboard}
              loading={false}
              emptyMessage="No completed attempts yet."
            />
          </CompactSection>
        </Card>

        <Card className="p-6">
          <div className="mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Top Highlights</h2>
            <p className="text-sm text-gray-500">Quick snapshot of what's being used</p>
          </div>
          <div className="space-y-4">
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-xs text-gray-500">Most used category</p>
              <p className="text-base font-semibold text-gray-900">{topCategory?.categoryName || '—'}</p>
              <p className="text-sm text-gray-500">{topCategory?.attemptCount ?? 0} attempts</p>
            </div>
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-xs text-gray-500">Leading user</p>
              <p className="text-base font-semibold text-gray-900">{topUser?.name || '—'}</p>
              <p className="text-sm text-gray-500">{topUser ? formatPercent(topUser.averageScorePercent) : 'No scores yet'}</p>
            </div>
            <div className="rounded-xl border border-gray-200 p-4">
              <p className="text-xs text-gray-500">Most active quiz</p>
              <p className="text-base font-semibold text-gray-900">{topQuiz?.title || '—'}</p>
              <p className="text-sm text-gray-500">{topQuiz?.attemptCount ?? 0} attempts</p>
            </div>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <Card className="p-6">
          <CompactSection
            title="Category Usage"
            searchValue={categoriesSearch}
            onSearchChange={setCategoriesSearch}
            onSearchSubmit={() => setCategoriesQuery(categoriesSearch)}
            searchPlaceholder="Search name or description..."
            isExpanded={categoriesExpanded}
            onToggleExpanded={() => setCategoriesExpanded((value) => !value)}
            loading={isLoading}
            emptyMessage="No category data yet."
            totalCount={filteredCategories.length}
            visibleCount={visibleCategories.length}
          >
            <div className="space-y-4">
              {visibleCategories.map((category) => (
                <div key={category.categoryId} className="space-y-2 rounded-xl border border-gray-100 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="font-medium text-gray-900">{category.categoryName}</p>
                      <p className="text-xs text-gray-500">
                        {category.attemptCount} attempts · {category.questionCount} questions
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-700">
                      {Math.round(((category.attemptCount || 0) / maxCategoryAttempts) * 100)}%
                    </p>
                  </div>
                  <BarRow label="Usage" value={category.attemptCount || 0} max={maxCategoryAttempts} accent="bg-indigo-500" />
                  <DifficultyStack easy={category.easyCount} medium={category.mediumCount} hard={category.hardCount} />
                </div>
              ))}
            </div>
          </CompactSection>
        </Card>

        <Card className="p-6">
          <CompactSection
            title="Quiz Performance"
            description="Which quizzes are being taken most"
            searchValue={quizzesSearch}
            onSearchChange={setQuizzesSearch}
            onSearchSubmit={() => setQuizzesQuery(quizzesSearch)}
            searchPlaceholder="Search title, category, status..."
            isExpanded={quizzesExpanded}
            onToggleExpanded={() => setQuizzesExpanded((value) => !value)}
            loading={isLoading}
            emptyMessage="No quiz data yet."
            totalCount={filteredQuizzes.length}
            visibleCount={visibleQuizzes.length}
          >
            <Table
              columns={topQuizColumns}
              data={visibleQuizzes}
              loading={false}
              emptyMessage="No quiz data yet."
            />
          </CompactSection>
        </Card>
      </div>

      <Card className="p-6">
        <CompactSection
          title="Recent Attempts"
          description="Latest submitted quiz results"
          searchValue={attemptsSearch}
          onSearchChange={setAttemptsSearch}
          onSearchSubmit={() => setAttemptsQuery(attemptsSearch)}
          searchPlaceholder="Search user, quiz, status..."
          isExpanded={attemptsExpanded}
          onToggleExpanded={() => setAttemptsExpanded((value) => !value)}
          loading={isLoading}
          emptyMessage="No quiz attempts yet."
          totalCount={filteredAttempts.length}
          visibleCount={visibleAttempts.length}
        >
          <Table
            columns={recentColumns}
            data={visibleAttempts}
            loading={false}
            emptyMessage="No quiz attempts yet."
          />
        </CompactSection>
      </Card>
    </div>
  )
}

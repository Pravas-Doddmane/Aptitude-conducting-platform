import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getUsers, blockUser, unblockUser } from '../api/userService'
import { Table } from '../components/ui/Table'
import { Button } from '../components/ui/Button'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { Badge } from '../components/ui/Badge'
import { useToast } from '../hooks/useToast'
import { Ban, CircleOff, Search } from 'lucide-react'
import { EmptyState } from '../components/ui/EmptyState'

export const Users = () => {
  const [search, setSearch] = useState('')
  const [blockId, setBlockId] = useState(null)
  const [unblockId, setUnblockId] = useState(null)
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data: users, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: () => getUsers(),
  })

  const blockMutation = useMutation({
    mutationFn: blockUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success('User blocked')
      setBlockId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const unblockMutation = useMutation({
    mutationFn: unblockUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      toast.success('User unblocked')
      setUnblockId(null)
    },
    onError: (err) => toast.error(err.response?.data?.message || 'Error'),
  })

  const filteredUsers = users?.filter(user =>
    user.email?.toLowerCase().includes(search.toLowerCase())
  ) || []

  const isAdministrator = (role) => {
    const normalized = role?.trim().toLowerCase()
    return normalized === 'administrator' || normalized === 'admin'
  }

  const columns = [
    { header: 'Email', accessor: 'email' },
    { header: 'First Name', accessor: 'firstName', render: (row) => row.firstName || '—' },
    { header: 'Last Name', accessor: 'lastName', render: (row) => row.lastName || '—' },
    {
      header: 'Status',
      render: (row) => {
        if (row.active === false) return <Badge variant="danger">Blocked</Badge>
        return <Badge variant="success">Active</Badge>
      },
    },
    {
      header: 'Role',
      render: (row) => (row.role ? <Badge variant="info">{row.role}</Badge> : '—'),
    },
    {
      header: 'Actions',
      render: (row) => (
        <div className="flex gap-1">
          {isAdministrator(row.role) ? (
            <span className="text-xs text-gray-400">Protected</span>
          ) : row.active !== false ? (
            <Button variant="ghost" size="sm" onClick={() => setBlockId(row.id)}>
              <Ban className="w-4 h-4 text-amber-600" />
            </Button>
          ) : (
            <Button variant="ghost" size="sm" onClick={() => setUnblockId(row.id)}>
              <CircleOff className="w-4 h-4 text-emerald-600" />
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Users</h1>
          <p className="text-sm text-gray-500 mt-1">Manage platform users</p>
        </div>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search by email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {!isLoading && filteredUsers.length === 0 ? (
        <EmptyState title="No users found" description={search ? 'No users match your search.' : 'No users registered yet.'} />
      ) : (
        <Table columns={columns} data={filteredUsers} loading={isLoading} />
      )}

      <ConfirmDialog
        open={!!blockId}
        onClose={() => setBlockId(null)}
        onConfirm={() => blockMutation.mutate(blockId)}
        title="Block User"
        message="Are you sure you want to block this user? They will lose access to the platform."
        confirmLabel="Block"
        loading={blockMutation.isLoading}
      />

      <ConfirmDialog
        open={!!unblockId}
        onClose={() => setUnblockId(null)}
        onConfirm={() => unblockMutation.mutate(unblockId)}
        title="Unblock User"
        message="This will restore the user's access to the platform."
        confirmLabel="Unblock"
        loading={unblockMutation.isLoading}
      />
    </div>
  )
}

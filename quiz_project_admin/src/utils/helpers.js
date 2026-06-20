export const formatDate = (dateString) => {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export const truncate = (str, len = 50) => {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '…' : str
}
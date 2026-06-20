import toast from 'react-hot-toast'

export const useToast = () => {
  return {
    success: (msg) => toast.success(msg),
    error: (msg) => toast.error(msg),
    loading: (msg) => toast.loading(msg),
  }
}
import { createContext, useState, useCallback, useEffect } from 'react'
import { loginAdmin, logoutUser } from '../api/authService'
import { accessToken } from '../api/axios'

export const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Check if token exists on mount
  useEffect(() => {
    if (accessToken) {
      // Could validate token or fetch profile, but we'll just assume valid
      setUser({ email: 'admin@gmail.com', role: 'ADMIN' }) // simplified
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (email, password) => {
    const data = await loginAdmin(email, password)
    setUser({ email, role: 'ADMIN' })
    return data
  }, [])

  const logout = useCallback(async () => {
    await logoutUser()
    setUser(null)
  }, [])

  const isAuthenticated = !!user

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
export const Card = ({ children, className = '', ...props }) => {
  return (
    <div className={`bg-white rounded-xl shadow-sm border border-gray-200/60 backdrop-blur-sm ${className}`} {...props}>
      {children}
    </div>
  )
}
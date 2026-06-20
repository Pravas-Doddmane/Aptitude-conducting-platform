export default function Card({ children, className = '', ...props }) {
  return (
    <div className={`bg-white dark:bg-gray-800 rounded-2xl shadow-lg hover:shadow-xl transition-shadow duration-300 ${className}`} {...props}>
      {children}
    </div>
  );
}
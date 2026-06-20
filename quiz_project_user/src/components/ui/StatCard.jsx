export default function StatCard({ label, value, icon: Icon, color = 'brand' }) {
  const bgColors = {
    brand: 'bg-gradient-to-br from-brand-500 to-brand-600',
    green: 'bg-gradient-to-br from-green-500 to-emerald-600',
    yellow: 'bg-gradient-to-br from-yellow-500 to-amber-600',
    purple: 'bg-gradient-to-br from-purple-500 to-indigo-600',
  };
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl shadow p-5 flex items-center gap-4">
      <div className={`p-3 rounded-xl text-white ${bgColors[color]}`}>
        {Icon && <Icon className="w-6 h-6" />}
      </div>
      <div>
        <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
        <p className="text-2xl font-bold">{value}</p>
      </div>
    </div>
  );
}
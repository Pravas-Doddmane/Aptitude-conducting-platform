import { Monitor, Camera, Mic, MapPin, ShieldAlert } from 'lucide-react';

const items = [
  { key: 'requireCamera', label: 'Camera Access', icon: Camera },
  //{ key: 'requireMicrophone', label: 'Microphone Access', icon: Mic },
  //{ key: 'requireLocation', label: 'Location Access', icon: MapPin },
  { key: 'requireFullScreen', label: 'Full Screen', icon: Monitor },
  //{ key: 'preventTabSwitch', label: 'No Tab Switching', icon: ShieldAlert },
];

export default function RequirementsChecklist({ quiz, permissions, requestPermissions }) {
  return (
    <div className="space-y-3">
      <h4 className="font-semibold">Competition Requirements</h4>
      {items.map(({ key, label, icon: Icon }) => {
        if (!quiz[key]) return null;
        const granted = permissions[key];
        return (
          <div key={key} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
            <div className="flex items-center gap-2">
              <Icon className="w-5 h-5 text-gray-500" />
              <span>{label}</span>
            </div>
            {granted ? (
              <span className="text-green-600 font-medium">Granted</span>
            ) : (
              <button onClick={() => requestPermissions(key)} className="text-brand-600 text-sm font-medium hover:underline">
                Request
              </button>
            )}
          </div>
        );
      })}
    </div>
  );
}
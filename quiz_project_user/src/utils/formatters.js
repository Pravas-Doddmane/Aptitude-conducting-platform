export const formatDuration = (totalSeconds) => {
  if (!totalSeconds && totalSeconds !== 0) return 'N/A';
  const hrs = Math.floor(totalSeconds / 3600);
  const mins = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;
  if (hrs > 0) return `${hrs}h ${mins}m ${secs}s`;
  if (mins > 0) return `${mins}m ${secs}s`;
  return `${secs}s`;
};

export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

export const truncateText = (text, max = 100) => {
  if (!text) return '';
  return text.length > max ? text.substring(0, max) + '...' : text;
};
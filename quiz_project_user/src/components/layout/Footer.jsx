export default function Footer() {
  return (
    <footer className="border-t dark:border-gray-800 py-6 text-center text-sm text-gray-500">
      © {new Date().getFullYear()} QuizMaster Pro. All rights reserved.
    </footer>
  );
}
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { forgotPassword } from '../api/authService';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { Mail } from 'lucide-react';
import toast from 'react-hot-toast';
import { useState } from 'react';
import { Link } from 'react-router-dom';

const schema = z.object({ email: z.string().email() });

export default function ForgotPassword() {
  const [sent, setSent] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) });

  const onSubmit = async (data) => {
    try {
      await forgotPassword(data.email);
      setSent(true);
      toast.success('If the email exists, a reset link has been sent.');
    } catch (err) {
      // error handled
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-center mb-6">Forgot Password</h2>
        {sent ? (
          <div className="text-center space-y-4">
            <p className="text-green-600">Check your email for the reset token.</p>
            <Link to="/reset-password" className="text-brand-600 hover:underline">Enter reset token</Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input label="Email" icon={Mail} type="email" {...register('email')} error={errors.email?.message} />
            <Button type="submit" className="w-full" isLoading={isSubmitting}>Send Reset Token</Button>
          </form>
        )}
      </div>
    </div>
  );
}
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { resetPassword } from '../api/authService';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { Key, Lock } from 'lucide-react';
import toast from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

const schema = z.object({
  token: z.string().min(1, 'Token is required'),
  newPassword: z.string().min(6),
});

export default function ResetPassword() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) });

  const onSubmit = async (data) => {
    try {
      await resetPassword(data.token, data.newPassword);
      toast.success('Password reset successful! Please login.');
      navigate('/login');
    } catch (err) {
      // handled
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-center mb-8">Reset Password</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <Input label="Reset Token" icon={Key} {...register('token')} error={errors.token?.message} />
          <Input label="New Password" icon={Lock} type="password" {...register('newPassword')} error={errors.newPassword?.message} />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>Reset Password</Button>
        </form>
      </div>
    </div>
  );
}
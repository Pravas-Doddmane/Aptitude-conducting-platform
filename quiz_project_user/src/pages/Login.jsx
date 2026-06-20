import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { Mail, Lock } from 'lucide-react';
import toast from 'react-hot-toast';

const schema = z.object({
  email: z.string().email('Invalid email'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data) => {
    try {
      await login(data);
      toast.success('Welcome back!');
      navigate('/dashboard');
    } catch (err) {
      // error handled by interceptor
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-center mb-8">Sign In</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <Input label="Email" icon={Mail} type="email" {...register('email')} error={errors.email?.message} />
          <Input label="Password" icon={Lock} type="password" {...register('password')} error={errors.password?.message} />
          <Button type="submit" className="w-full" isLoading={isSubmitting}>Login</Button>
        </form>
        <div className="mt-6 text-center text-sm space-y-2">
          <Link to="/forgot-password" className="text-brand-600 hover:underline">Forgot password?</Link>
          <p>Don't have an account? <Link to="/register" className="text-brand-600 font-medium hover:underline">Sign up</Link></p>
        </div>
      </div>
    </div>
  );
}
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { register as registerApi, sendRegistrationOtp, verifyRegistrationOtp } from '../api/authService';
import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { KeyRound, Mail, Lock, User } from 'lucide-react';
import toast from 'react-hot-toast';
import { useState } from 'react';

const schema = z.object({
  firstName: z.string().min(1, 'Required'),
  lastName: z.string().min(1, 'Required'),
  email: z.string().email(),
  otp: z.string().optional(),
  password: z.string().min(8, 'Password must be at least 8 characters'),
});

export default function Register() {
  const navigate = useNavigate();
  const [otpSent, setOtpSent] = useState(false);
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isVerifyingOtp, setIsVerifyingOtp] = useState(false);
  const [emailVerificationToken, setEmailVerificationToken] = useState('');
  const [verifiedEmail, setVerifiedEmail] = useState('');
  const { register, handleSubmit, watch, getValues, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) });
  const email = watch('email');

  const handleSendOtp = async () => {
    const currentEmail = getValues('email');
    if (!currentEmail) {
      toast.error('Enter your email first');
      return;
    }
    setIsSendingOtp(true);
    try {
      await sendRegistrationOtp(currentEmail);
      setOtpSent(true);
      setEmailVerificationToken('');
      setVerifiedEmail('');
      toast.success('OTP sent to your email. Check it once.');
    } finally {
      setIsSendingOtp(false);
    }
  };

  const handleVerifyOtp = async () => {
    const currentEmail = getValues('email');
    const otp = getValues('otp');
    if (!currentEmail || !otp) {
      toast.error('Enter email and OTP');
      return;
    }
    setIsVerifyingOtp(true);
    try {
      const { data } = await verifyRegistrationOtp(currentEmail, otp);
      setEmailVerificationToken(data.verificationToken);
      setVerifiedEmail(currentEmail);
      toast.success('Email verified successfully');
    } finally {
      setIsVerifyingOtp(false);
    }
  };

  const onSubmit = async (data) => {
    if (!emailVerificationToken || data.email !== verifiedEmail) {
      toast.error('Please verify your email OTP first');
      return;
    }
    try {
      const { otp, ...payload } = data;
      await registerApi({ ...payload, emailVerificationToken });
      toast.success('Registration successful! Please login.');
      navigate('/login');
    } catch (err) {
      // handled
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
        <h2 className="text-3xl font-bold text-center mb-8">Create Account</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <Input label="First Name" icon={User} {...register('firstName')} error={errors.firstName?.message} />
          <Input label="Last Name" icon={User} {...register('lastName')} error={errors.lastName?.message} />
          <div className="space-y-2">
            <Input label="Email" icon={Mail} type="email" {...register('email')} error={errors.email?.message} />
            <Button
              type="button"
              variant="secondary"
              className="w-full justify-center"
              isLoading={isSendingOtp}
              disabled={!email || (!!emailVerificationToken && email === verifiedEmail)}
              onClick={handleSendOtp}
            >
              {emailVerificationToken && email === verifiedEmail ? 'Email Verified' : otpSent ? 'Resend OTP' : 'Send OTP'}
            </Button>
          </div>
          {otpSent && (
            <div className="space-y-2 rounded-xl border border-brand-100 bg-brand-50/70 p-3 dark:border-brand-900 dark:bg-brand-900/20">
              <p className="text-sm text-gray-600 dark:text-gray-300">OTP sent to your email. Check it once and verify before registration.</p>
              <Input label="Email OTP" icon={KeyRound} inputMode="numeric" maxLength="6" {...register('otp')} error={errors.otp?.message} />
              <Button
                type="button"
                variant={emailVerificationToken ? 'primary' : 'secondary'}
                className="w-full justify-center"
                isLoading={isVerifyingOtp}
                disabled={!!emailVerificationToken && email === verifiedEmail}
                onClick={handleVerifyOtp}
              >
                {emailVerificationToken && email === verifiedEmail ? 'Verified' : 'Verify OTP'}
              </Button>
            </div>
          )}
          <Input label="Password" icon={Lock} type="password" {...register('password')} error={errors.password?.message} />
          <Button type="submit" className="w-full justify-center" isLoading={isSubmitting} disabled={!emailVerificationToken || email !== verifiedEmail}>Register</Button>
        </form>
        <p className="mt-6 text-center text-sm">
          Already have an account? <Link to="/login" className="text-brand-600 hover:underline">Login</Link>
        </p>
      </div>
    </div>
  );
}

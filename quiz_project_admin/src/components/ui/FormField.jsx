import { Input } from './Input'
import { Select } from './Select'

export const FormField = ({ type = 'text', ...props }) => {
  if (type === 'select') return <Select {...props} />
  if (type === 'textarea') return <textarea {...props} className={`...same as Input`} />
  return <Input type={type} {...props} />
}
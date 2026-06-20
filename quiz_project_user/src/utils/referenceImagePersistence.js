import { captureReferenceImage } from '../api/identityVerificationService';
import { uploadProctoringImage, toAbsoluteUploadUrl } from '../api/uploadService';

export function dataUrlToFile(dataUrl, fileName = 'reference.jpg') {
  const [header, base64Data] = dataUrl.split(',');
  if (!base64Data) {
    throw new Error('Invalid image data');
  }
  const mimeMatch = header.match(/:(.*?);/);
  const mimeType = mimeMatch ? mimeMatch[1] : 'image/jpeg';
  const binary = atob(base64Data);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new File([bytes], fileName, { type: mimeType });
}

/**
 * Uploads the captured frame to Spring Boot storage, then persists the URL
 * on the existing identity_verifications record via the reference API.
 */
export async function persistReferenceImage(attemptId, frameDataUrl) {
  if (!attemptId || !frameDataUrl) {
    throw new Error('Reference image data is missing');
  }

  const file = dataUrlToFile(frameDataUrl);
  const uploadRes = await uploadProctoringImage(file);
  const storedUrl = toAbsoluteUploadUrl(uploadRes.data?.imageUrl);

  if (!storedUrl) {
    throw new Error('Image upload did not return a URL');
  }

  const verificationRes = await captureReferenceImage(attemptId, storedUrl);
  return verificationRes.data;
}

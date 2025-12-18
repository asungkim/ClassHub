/**
 * 전화번호 자동 포맷팅 유틸
 * 숫자만 입력하면 010-XXXX-XXXX 형식으로 자동 변환
 */
export function formatPhoneNumber(value: string): string {
  const digits = value.replace(/\D/g, ''); // 숫자만 추출

  if (digits.length <= 3) return digits;
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7, 11)}`;
}

/**
 * 전화번호 유효성 검증
 * 010-XXXX-XXXX 또는 010-XXX-XXXX 형식 (12~13자)
 */
export function validatePhoneNumber(phone: string): boolean {
  return /^010-\d{3,4}-\d{4}$/.test(phone) && phone.length >= 12 && phone.length <= 13;
}

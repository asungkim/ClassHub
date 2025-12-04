export type ClassHubTheme = typeof classhubTheme;

export const classhubTheme = {
  name: "ClassHub Theme",
  description: "모던하고 깔끔한 교육 플랫폼 스타일",
  colors: {
    primary: {
      main: "#5B5FED",
      light: "#7B7FF5",
      dark: "#4A4DC9",
      gradient: "linear-gradient(135deg, #5B5FED 0%, #9D4EDD 100%)"
    },
    secondary: {
      main: "#9D4EDD",
      light: "#B968F0",
      dark: "#7B3AB8"
    },
    background: {
      main: "#F8F9FC",
      card: "#FFFFFF",
      gradient: "linear-gradient(135deg, #E8EAFF 0%, #F5E6FF 50%, #FFE6F5 100%)"
    },
    text: {
      primary: "#1A1A1A",
      secondary: "#6B7280",
      placeholder: "#9CA3AF",
      link: "#5B5FED",
      muted: "#A1A1A1"
    },
    border: {
      light: "#E5E7EB",
      main: "#D1D5DB",
      focus: "#5B5FED"
    },
    icon: {
      primary: "#6B7280",
      secondary: "#9CA3AF"
    }
  },
  typography: {
    fontFamily: {
      primary:
        "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Noto Sans KR', Roboto, 'Helvetica Neue', Arial, sans-serif",
      korean: "'Noto Sans KR', 'Malgun Gothic', '맑은 고딕', sans-serif"
    },
    fontSize: {
      xs: "12px",
      sm: "14px",
      base: "16px",
      lg: "18px",
      xl: "20px",
      "2xl": "24px",
      "3xl": "30px",
      "4xl": "36px"
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },
  spacing: {
    xs: "4px",
    sm: "8px",
    md: "16px",
    lg: "24px",
    xl: "32px",
    "2xl": "48px",
    "3xl": "64px"
  },
  borderRadius: {
    sm: "8px",
    md: "12px",
    lg: "16px",
    xl: "20px",
    full: "9999px"
  },
  shadows: {
    sm: "0 1px 2px 0 rgba(0, 0, 0, 0.05)",
    md: "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)",
    lg: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
    xl: "0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)",
    card: "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)"
  }
} as const;

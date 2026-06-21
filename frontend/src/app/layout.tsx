import "./globals.css";

export const metadata = {
  title: "열람실 Now",
  description: "공공도서관 열람실 좌석 현황 지도 서비스",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    const backendApiBaseUrl =
      process.env.BACKEND_API_BASE_URL ?? "http://localhost:8080";

    return [
      {
        source: "/api/:path*",
        destination: `${backendApiBaseUrl}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;

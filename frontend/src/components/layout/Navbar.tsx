'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export default function Navbar() {
  const pathname = usePathname();
  const linkClass = (href: string) =>
    `text-sm hover:text-blue-600 transition-colors ${pathname === href ? 'text-blue-600 font-semibold' : 'text-gray-700'}`;

  return (
    <nav className="bg-white shadow-md sticky top-0 z-50">
      <div className="max-w-4xl mx-auto px-4">
        <div className="flex justify-between items-center py-4">
          <Link href="/" className="text-xl font-bold text-gray-900">My Dev Blog</Link>
          <div className="flex items-center space-x-4">
            <Link href="/posts" className={linkClass('/posts')}>📚 게시글</Link>
            <Link href="/qna" className={linkClass('/qna')}>📩 Q&A</Link>
            <Link href="/auth/login" className={linkClass('/auth/login')}>🔑 로그인</Link>
          </div>
        </div>
      </div>
    </nav>
  );
}

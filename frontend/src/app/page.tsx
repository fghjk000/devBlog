'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Post, PageResponse } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
const IMAGE_BASE = process.env.NEXT_PUBLIC_IMAGE_URL || 'https://blog.haruseop.com';

export default function HomePage() {
  const [posts, setPosts] = useState<Post[]>([]);

  useEffect(() => {
    fetch(`${API_URL}/api/posts?page=0&size=6`)
      .then(r => r.ok ? r.json() : { content: [] })
      .then((d: PageResponse<Post>) => setPosts(d.content ?? []))
      .catch(() => setPosts([]));
  }, []);

  return (
    <>
      {/* Hero */}
      <header className="bg-blue-700 text-white py-16 text-center px-4">
        <h1 className="text-4xl font-bold tracking-tight">💡 개발과 기술, 그리고 나의 이야기</h1>
        <p className="text-base text-blue-100 mt-3">개발자로 살아가는 이야기와 배운 것들을 공유합니다.</p>
      </header>

      {/* 최신 게시글 */}
      <section className="max-w-4xl mx-auto mt-10 px-6">
        <h2 className="text-2xl font-bold text-gray-800 mb-6">📝 최신 게시글</h2>
        {posts.length === 0 ? (
          <p className="text-gray-500">불러오는 중...</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {posts.map((post) => (
              <Link key={post.id} href={`/posts/${post.id}`}>
                <div className="bg-white rounded-lg shadow hover:shadow-lg transition overflow-hidden h-full">
                  {post.imageUrl ? (
                    <img
                      src={`${IMAGE_BASE}${post.imageUrl}`}
                      alt={post.title}
                      className="w-full h-48 object-cover"
                      onError={(e) => { (e.target as HTMLImageElement).src = '/default-image.png'; }}
                    />
                  ) : (
                    <div className="w-full h-48 bg-gray-100 flex items-center justify-center text-gray-400 text-4xl">📄</div>
                  )}
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-900 line-clamp-2">{post.title}</h3>
                    <p className="text-xs text-gray-500 mt-1">{new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
        <div className="mt-8 text-center">
          <Link href="/posts" className="inline-block bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition">
            전체 게시글 보기
          </Link>
        </div>
      </section>

      {/* 소개 */}
      <section className="max-w-4xl mx-auto px-6 text-center py-10 mt-10">
        <h2 className="text-2xl font-semibold text-gray-800 mb-3">👨‍💻 블로그 주인장 소개</h2>
        <p className="text-base text-gray-600">안녕하세요! 저는 개발자입니다. 이 블로그에서는 제가 배운 기술과 개발 이야기, 그리고 다양한 경험을 공유하려고 합니다. 함께 성장해 나가요!</p>
      </section>
    </>
  );
}

'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Post, PageResponse } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
const IMAGE_BASE = process.env.NEXT_PUBLIC_IMAGE_URL || 'https://blog.haruseop.com';

export default function PostsPage() {
  const [data, setData] = useState<PageResponse<Post> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page), size: '9' });
    if (query) params.set('search', query);
    fetch(`${API_URL}/api/posts?${params}`)
      .then(r => r.json())
      .then(setData)
      .catch(() => setData(null));
  }, [page, query]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setQuery(search);
  };

  return (
    <section className="max-w-5xl mx-auto mt-10 px-6 pb-16">
      <h2 className="text-3xl font-bold text-gray-800 mb-6">📝 게시글 목록</h2>

      <form onSubmit={handleSearch} className="mb-6 flex gap-2">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="제목 검색..."
          className="flex-1 p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">검색</button>
      </form>

      {!data ? (
        <p className="text-gray-500">불러오는 중...</p>
      ) : data.content.length === 0 ? (
        <p className="text-gray-500">게시글이 없습니다.</p>
      ) : (
        <div className="grid md:grid-cols-3 gap-6">
          {data.content.map((post) => (
            <Link key={post.id} href={`/posts/${post.id}`}>
              <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition h-full">
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
                  <h3 className="text-lg font-semibold text-gray-900 mb-1 line-clamp-2">{post.title}</h3>
                  <p className="text-sm text-gray-500">{new Date(post.createdAt).toLocaleDateString('ko-KR')}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-1.5 mt-8">
          {data.number > 0 && (
            <button onClick={() => setPage(p => p - 1)} className="px-3 py-2 border rounded bg-gray-200 hover:bg-gray-300">이전</button>
          )}
          {Array.from({ length: data.totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`px-3 py-2 border rounded ${i === data.number ? 'bg-blue-600 text-white' : 'bg-gray-200 hover:bg-gray-300'}`}
            >
              {i + 1}
            </button>
          ))}
          {data.number + 1 < data.totalPages && (
            <button onClick={() => setPage(p => p + 1)} className="px-3 py-2 border rounded bg-gray-200 hover:bg-gray-300">다음</button>
          )}
        </div>
      )}
    </section>
  );
}

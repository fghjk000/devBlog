'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Qna, PageResponse } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export default function QnaPage() {
  const [data, setData] = useState<PageResponse<Qna> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page), size: '10' });
    if (query) params.set('search', query);
    fetch(`${API_URL}/api/qna?${params}`)
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
    <>
      <header className="relative h-40 flex items-center justify-center text-white bg-blue-700">
        <div className="text-center">
          <h1 className="text-4xl font-bold">📩 Q&A 게시판</h1>
          <p className="text-blue-100 mt-2">개발 관련 질문과 답변을 공유하는 공간입니다.</p>
        </div>
      </header>

      <section className="max-w-4xl mx-auto mt-10 px-6 pb-16">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-3xl font-bold text-gray-800">📝 Q&A 목록</h2>
          <Link href="/qna/create" className="text-sm text-blue-600 hover:text-blue-800">새 질문 작성</Link>
        </div>

        <form onSubmit={handleSearch} className="mb-6 flex gap-2">
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="검색어 입력..."
            className="flex-1 p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button type="submit" className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700">검색</button>
        </form>

        {!data ? (
          <p className="text-gray-500">불러오는 중...</p>
        ) : data.content.length === 0 ? (
          <p className="text-gray-500">Q&A가 없습니다.</p>
        ) : (
          <div className="flex flex-col gap-4">
            {data.content.map((qna) => (
              <Link key={qna.id} href={`/qna/${qna.id}`}>
                <div className="bg-white rounded-lg shadow hover:shadow-lg transition p-6">
                  <h3 className="text-xl font-semibold text-gray-900 hover:text-blue-600">{qna.title}</h3>
                  <p className="text-gray-500 text-sm mt-1">질문자: {qna.username}</p>
                  <p className="text-gray-500 text-sm">{new Date(qna.createdAt).toLocaleDateString('ko-KR')}</p>
                  <p className="text-gray-700 text-sm mt-2 line-clamp-2">{qna.content}</p>
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
    </>
  );
}

'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export default function QnaCreatePage() {
  const router = useRouter();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    setSubmitting(true);
    setError('');
    const res = await fetch(`${API_URL}/api/qna`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, content }),
      credentials: 'include',
    });
    if (res.ok) {
      const data = await res.json();
      router.push(`/qna/${data.id}`);
    } else if (res.status === 401 || res.status === 403) {
      setError('로그인이 필요합니다.');
    } else {
      setError('Q&A 작성에 실패했습니다. 다시 시도해주세요.');
    }
    setSubmitting(false);
  };

  return (
    <>
      <header className="relative h-48 flex items-center justify-center text-white bg-blue-700">
        <div className="text-center">
          <h1 className="text-4xl font-bold">Q&A를 작성해보세요!</h1>
          <p className="text-base text-blue-100 mt-2">여러분의 질문을 남기세요.</p>
        </div>
      </header>

      <section className="max-w-4xl mx-auto px-6 mt-12 pb-16">
        <div className="bg-white p-8 rounded-lg shadow-xl">
          <h2 className="text-3xl font-bold text-gray-800 mb-6">새 Q&A 작성</h2>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-300 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-6">
              <label htmlFor="title" className="block text-gray-700 text-lg font-semibold mb-2">제목</label>
              <input
                type="text"
                id="title"
                value={title}
                onChange={e => setTitle(e.target.value)}
                className="w-full p-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="제목을 입력하세요."
                required
              />
            </div>

            <div className="mb-6">
              <label htmlFor="content" className="block text-gray-700 text-lg font-semibold mb-2">내용</label>
              <textarea
                id="content"
                value={content}
                onChange={e => setContent(e.target.value)}
                rows={8}
                className="w-full p-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="내용을 입력하세요."
                required
              />
            </div>

            <div className="flex justify-between items-center">
              <Link href="/qna" className="text-blue-600 hover:underline">← Q&A 목록으로</Link>
              <button
                type="submit"
                disabled={submitting}
                className="bg-blue-600 text-white px-8 py-4 rounded-lg hover:bg-blue-700 transition duration-200 disabled:opacity-50"
              >
                {submitting ? '작성 중...' : 'Q&A 작성'}
              </button>
            </div>
          </form>
        </div>
      </section>
    </>
  );
}

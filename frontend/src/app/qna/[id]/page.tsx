'use client';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { Qna } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';

export default function QnaDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [qna, setQna] = useState<Qna | null>(null);
  const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${API_URL}/api/qna/${id}`)
      .then(r => r.ok ? r.json() : null)
      .then(d => { setQna(d); setLoading(false); });
  }, [id]);

  const handleAnswer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!answer.trim()) return;
    await fetch(`${API_URL}/api/qna/${id}/answer`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: answer }),
      credentials: 'include',
    });
    setAnswer('');
    const updated = await fetch(`${API_URL}/api/qna/${id}`).then(r => r.json());
    setQna(updated);
  };

  if (loading) return <div className="max-w-4xl mx-auto mt-10 px-6 text-gray-500">불러오는 중...</div>;
  if (!qna) return <div className="max-w-4xl mx-auto mt-10 px-6 text-gray-500">Q&A를 찾을 수 없습니다.</div>;

  return (
    <section className="max-w-4xl mx-auto my-8 px-6 pb-16">
      {/* 질문 카드 */}
      <div className="bg-white p-8 rounded-lg shadow-lg mb-8 border-l-4 border-blue-500">
        <h1 className="text-3xl font-semibold text-gray-900 mb-4">{qna.title}</h1>
        <p className="text-sm text-gray-500">질문자: <span className="font-semibold">{qna.username}</span></p>
        <p className="text-sm text-gray-500 mb-4">작성일: {new Date(qna.createdAt).toLocaleString('ko-KR')}</p>
        <h2 className="text-xl font-semibold text-gray-900 mb-3">질문 내용</h2>
        <p className="text-gray-700 text-lg leading-relaxed whitespace-pre-wrap">{qna.content}</p>
      </div>

      {/* 답변 카드 */}
      <div className="bg-white p-6 rounded-lg shadow-lg mb-8 border-l-4 border-green-500">
        <h2 className="text-xl font-bold text-gray-900 mb-4">답변 내용</h2>
        {!qna.answers || qna.answers.length === 0 ? (
          <p className="text-gray-500 italic">답변이 없습니다.</p>
        ) : (
          qna.answers.map((a, i) => (
            <div key={i} className="mb-3 p-3 border-b border-gray-200">
              <span className="font-semibold text-green-600">{a.username}: </span>
              <span className="text-gray-700">{a.content}</span>
            </div>
          ))
        )}
      </div>

      {/* 답변 작성 */}
      <div className="bg-white p-6 rounded-lg shadow-lg">
        <h3 className="text-xl font-semibold text-gray-900 mb-4">답변하기</h3>
        <form onSubmit={handleAnswer}>
          <textarea
            value={answer}
            onChange={e => setAnswer(e.target.value)}
            placeholder="답변을 작성해주세요"
            className="w-full p-4 rounded-lg border border-gray-300 mb-4 resize-none"
            rows={4}
          />
          <button type="submit" className="bg-blue-600 text-white p-3 rounded-lg hover:bg-blue-700">
            답변 제출
          </button>
        </form>
      </div>

      <div className="mt-6">
        <Link href="/qna" className="text-blue-600 hover:underline">← Q&A 목록으로</Link>
      </div>
    </section>
  );
}

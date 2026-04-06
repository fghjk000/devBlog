'use client';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { Post, Comment } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
const IMAGE_BASE = process.env.NEXT_PUBLIC_IMAGE_URL || 'https://blog.haruseop.com';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetch(`${API_URL}/api/posts/${id}`).then(r => r.ok ? r.json() : null),
      fetch(`${API_URL}/api/comments/${id}`).then(r => r.ok ? r.json() : []),
    ]).then(([p, c]) => {
      setPost(p);
      setComments(c ?? []);
      setLoading(false);
    });
  }, [id]);

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) return;
    await fetch(`${API_URL}/api/comments/${id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: comment }),
      credentials: 'include',
    });
    setComment('');
    const updated = await fetch(`${API_URL}/api/comments/${id}`).then(r => r.json());
    setComments(updated ?? []);
  };

  if (loading) return <div className="max-w-4xl mx-auto mt-10 px-6 text-gray-500">불러오는 중...</div>;
  if (!post) return <div className="max-w-4xl mx-auto mt-10 px-6 text-gray-500">게시글을 찾을 수 없습니다.</div>;

  return (
    <div className="max-w-4xl mx-auto mt-10 px-6 pb-16">
      <article className="bg-white rounded-lg shadow-lg p-6 mb-8">
        <h2 className="text-3xl font-semibold text-gray-900 mb-4">{post.title}</h2>
        {post.imageUrl && (
          <div className="mb-6">
            <img
              src={`${IMAGE_BASE}${post.imageUrl}`}
              alt={post.title}
              className="w-full max-h-72 object-contain rounded-lg"
              onError={(e) => { (e.target as HTMLImageElement).src = '/default-image.png'; }}
            />
          </div>
        )}
        <div
          className="text-gray-700 mb-4 prose max-w-none"
          style={{ whiteSpace: 'pre-wrap', overflowWrap: 'break-word' }}
          dangerouslySetInnerHTML={{ __html: post.content }}
        />
        <p className="text-sm text-gray-500">작성자: {post.author}</p>
        <p className="text-sm text-gray-500">작성일: {new Date(post.createdAt).toLocaleString('ko-KR')}</p>
        <Link href="/posts" className="inline-block mt-4 text-blue-600 hover:underline">← 목록으로</Link>
      </article>

      {/* 댓글 */}
      <section>
        <h3 className="text-2xl font-semibold mb-4">💬 댓글 ({comments.length})</h3>
        <form onSubmit={handleComment} className="mb-6 bg-white p-4 rounded-lg shadow">
          <textarea
            value={comment}
            onChange={e => setComment(e.target.value)}
            rows={3}
            placeholder="댓글을 입력하세요..."
            className="w-full p-2 border rounded-md resize-none"
          />
          <button type="submit" className="mt-2 px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-700">
            댓글 등록
          </button>
        </form>
        <div className="bg-white rounded-lg shadow-lg p-4">
          {comments.length === 0 ? (
            <p className="text-gray-500 text-sm">댓글이 없습니다.</p>
          ) : (
            <ul className="divide-y divide-gray-200">
              {comments.map((c) => (
                <li key={c.id} className="py-3" style={{ overflowWrap: 'break-word' }}>
                  <strong className="text-blue-600">{c.username}</strong>
                  <span className="mx-1 text-gray-700">{c.content}</span>
                  <small className="text-gray-400 ml-2">{new Date(c.createdAt).toLocaleString('ko-KR')}</small>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}

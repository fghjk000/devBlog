const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
// Docker 내부 SSR용 - runtime env (NEXT_PUBLIC_* 는 build-time bake이므로 SSR에서는 내부 주소 사용)
const INTERNAL_API_URL = process.env.INTERNAL_API_URL || API_URL;

export async function fetchPosts(page = 0, search?: string) {
  const params = new URLSearchParams({ page: String(page), size: '9' });
  if (search) params.set('search', search);
  const res = await fetch(`${API_URL}/api/posts?${params}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch posts');
  return res.json();
}

export async function fetchPost(id: string | number) {
  const res = await fetch(`${API_URL}/api/posts/${id}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch post');
  return res.json();
}

export async function fetchComments(postId: string | number) {
  const res = await fetch(`${API_URL}/api/comments/${postId}`, { cache: 'no-store' });
  if (!res.ok) return [];
  return res.json();
}

export async function fetchQnaList(page = 0, search?: string) {
  const params = new URLSearchParams({ page: String(page), size: '10' });
  if (search) params.set('search', search);
  const res = await fetch(`${API_URL}/api/qna?${params}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch qna');
  return res.json();
}

export async function fetchQna(id: string | number) {
  const res = await fetch(`${API_URL}/api/qna/${id}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Failed to fetch qna');
  return res.json();
}

export async function fetchRecentPosts() {
  const res = await fetch(`${INTERNAL_API_URL}/api/posts?page=0&size=6`, { next: { revalidate: 60 } });
  if (!res.ok) return { content: [] };
  return res.json();
}

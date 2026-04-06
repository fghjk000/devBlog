# UI Redesign - Dark Minimal Theme Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Next.js 프론트엔드를 미니멀 다크 개발자 블로그로 리디자인 (Zenn.dev 스타일)

**Architecture:** 기존 컴포넌트들의 스타일만 변경. Tailwind arbitrary values (`bg-[#1a1a1a]`) 방식 사용. 새 라이브러리 추가 없이 Pretendard 폰트는 CDN으로 로드.

**Tech Stack:** Next.js 14, Tailwind CSS v4, CSS custom properties, Pretendard 폰트 (CDN)

**Color System:**
- bg: `#111111`
- surface: `#1a1a1a`
- border: `#2a2a2a`
- text: `#e5e5e5`
- muted: `#888888`
- accent: `#a78bfa` (연보라)

---

### Task 1: 전역 스타일 및 폰트 설정

**Files:**
- Modify: `frontend/src/app/globals.css`
- Modify: `frontend/src/app/layout.tsx`

**Step 1: globals.css 전체 교체**

```css
@import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css');

@import "tailwindcss";

body {
  background: #111111;
  color: #e5e5e5;
  font-family: 'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* ===== Prose - 블로그 본문 스타일 ===== */
.prose {
  color: #d4d4d4;
  line-height: 1.85;
  font-size: 1rem;
}

.prose h1,
.prose h2,
.prose h3,
.prose h4 {
  color: #ffffff;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  line-height: 1.35;
}

.prose h1 { font-size: 1.875rem; }
.prose h2 { font-size: 1.5rem; border-bottom: 1px solid #2a2a2a; padding-bottom: 0.5rem; }
.prose h3 { font-size: 1.25rem; }

.prose p {
  margin-bottom: 1.25rem;
}

.prose a {
  color: #a78bfa;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.prose a:hover { color: #c4b5fd; }

.prose strong { color: #ffffff; font-weight: 600; }

.prose code {
  font-family: 'JetBrains Mono', 'Fira Code', ui-monospace, monospace;
  background: #1e1e2e;
  color: #a78bfa;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.875em;
}

.prose pre {
  background: #1e1e2e;
  border: 1px solid #2a2a2a;
  border-radius: 10px;
  padding: 1.25rem 1.5rem;
  overflow-x: auto;
  margin: 1.5rem 0;
}

.prose pre code {
  background: transparent;
  color: #e5e5e5;
  padding: 0;
  font-size: 0.9em;
}

.prose blockquote {
  border-left: 3px solid #a78bfa;
  padding-left: 1.25rem;
  color: #888888;
  font-style: italic;
  margin: 1.5rem 0;
}

.prose ul,
.prose ol {
  padding-left: 1.5rem;
  margin-bottom: 1.25rem;
}
.prose li { margin-bottom: 0.5rem; }

.prose table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  font-size: 0.9rem;
}
.prose th,
.prose td {
  border: 1px solid #2a2a2a;
  padding: 0.6rem 0.875rem;
  text-align: left;
}
.prose th {
  background: #1a1a1a;
  color: #e5e5e5;
  font-weight: 600;
}

.prose img { border-radius: 8px; max-width: 100%; }

.prose hr {
  border: none;
  border-top: 1px solid #2a2a2a;
  margin: 2rem 0;
}
```

**Step 2: layout.tsx 업데이트 (Inter 폰트 제거)**

```tsx
import type { Metadata } from 'next';
import './globals.css';
import Providers from './providers';
import Header from '@/components/layout/Header';
import Footer from '@/components/layout/Footer';

export const metadata: Metadata = {
  title: 'DevBlog',
  description: '개발 블로그',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          <Header />
          <main className="min-h-screen">{children}</main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
```

**Step 3: 확인**

`http://localhost:3000` 접속 → 배경이 `#111111` 어두운 색으로 바뀌면 성공

**Step 4: 커밋**

```bash
git add frontend/src/app/globals.css frontend/src/app/layout.tsx
git commit -m "style: 전역 다크 테마 및 Pretendard 폰트 적용"
```

---

### Task 2: Header & Footer 리디자인

**Files:**
- Modify: `frontend/src/components/layout/Header.tsx`
- Modify: `frontend/src/components/layout/Footer.tsx`

**Step 1: Header.tsx 전체 교체**

```tsx
'use client';

import Link from 'next/link';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';

export default function Header() {
  const { user, logout } = useAuthStore();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout');
    } finally {
      logout();
      router.push('/');
    }
  };

  return (
    <header className="sticky top-0 z-50 bg-[#111111] border-b border-[#2a2a2a]">
      <div className="max-w-3xl mx-auto px-6 py-4 flex items-center justify-between">
        <Link
          href="/"
          className="font-mono text-lg font-bold text-[#a78bfa] tracking-tight hover:opacity-80 transition-opacity"
        >
          seop.dev
        </Link>
        <nav className="flex items-center gap-6 text-sm">
          <Link href="/posts" className="text-[#888888] hover:text-[#e5e5e5] transition-colors">
            Posts
          </Link>
          <Link href="/qna" className="text-[#888888] hover:text-[#e5e5e5] transition-colors">
            Q&A
          </Link>
          {user ? (
            <>
              {user.role === 'ROLE_ADMIN' && (
                <Link href="/posts/new" className="text-[#a78bfa] hover:opacity-80 transition-opacity">
                  Write
                </Link>
              )}
              <Link href="/mypage" className="text-[#888888] hover:text-[#e5e5e5] transition-colors">
                Me
              </Link>
              <button
                onClick={handleLogout}
                className="text-[#888888] hover:text-[#e5e5e5] transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <Link href="/auth/login" className="text-[#a78bfa] hover:opacity-80 transition-opacity">
              Login
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
```

**Step 2: Footer.tsx 전체 교체**

```tsx
export default function Footer() {
  return (
    <footer className="border-t border-[#2a2a2a] mt-16">
      <div className="max-w-3xl mx-auto px-6 py-8 flex items-center justify-between text-sm text-[#888888]">
        <span className="font-mono text-[#a78bfa] text-sm">seop.dev</span>
        <div className="flex items-center gap-4">
          <a
            href="https://github.com/kimhanseop"
            target="_blank"
            rel="noopener noreferrer"
            className="hover:text-[#e5e5e5] transition-colors"
          >
            GitHub
          </a>
          <span>© 2026 seop</span>
        </div>
      </div>
    </footer>
  );
}
```

**Step 3: 확인**

페이지 새로고침 → 헤더가 어두운 배경에 `seop.dev` 모노스페이스 로고, 네비 링크들이 표시되면 성공

**Step 4: 커밋**

```bash
git add frontend/src/components/layout/
git commit -m "style: Header/Footer 다크 테마 적용"
```

---

### Task 3: PostCard 컴포넌트 리디자인

**Files:**
- Modify: `frontend/src/components/post/PostCard.tsx`

**Step 1: PostCard.tsx 전체 교체**

```tsx
import Link from 'next/link';
import { Post } from '@/types';

export default function PostCard({ post }: { post: Post }) {
  return (
    <article className="bg-[#1a1a1a] border border-[#2a2a2a] rounded-xl overflow-hidden group hover:border-[#a78bfa] transition-all duration-200">
      {post.imageUrl && (
        <div className="aspect-video overflow-hidden">
          <img
            src={`${process.env.NEXT_PUBLIC_API_URL}${post.imageUrl}`}
            alt={post.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        </div>
      )}
      <div className="p-5">
        {post.categoryName && (
          <span className="inline-block text-[0.7rem] bg-[#2a2a2a] text-[#a78bfa] px-2 py-0.5 rounded font-medium tracking-wide">
            {post.categoryName}
          </span>
        )}
        <h2 className="mt-2 text-[0.95rem] font-semibold text-[#e5e5e5] leading-snug line-clamp-2">
          <Link
            href={`/posts/${post.id}`}
            className="hover:text-[#a78bfa] transition-colors"
          >
            {post.title}
          </Link>
        </h2>
        <div className="mt-3 flex items-center gap-3 text-xs text-[#888888]">
          <span>{new Date(post.createdAt).toLocaleDateString('ko-KR')}</span>
          {post.commentCount > 0 && <span>· 댓글 {post.commentCount}</span>}
        </div>
      </div>
    </article>
  );
}
```

**Step 2: 확인**

홈 페이지에서 카드들이 어두운 배경, 보라 border hover 효과로 보이면 성공

**Step 3: 커밋**

```bash
git add frontend/src/components/post/PostCard.tsx
git commit -m "style: PostCard 다크 테마 및 hover 효과 적용"
```

---

### Task 4: 홈 페이지 리디자인

**Files:**
- Modify: `frontend/src/app/page.tsx`

**Step 1: page.tsx 전체 교체**

```tsx
import Link from 'next/link';
import PostCard from '@/components/post/PostCard';
import { Post, PageResponse } from '@/types';

async function getRecentPosts(): Promise<Post[]> {
  try {
    const res = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/posts?page=0&size=6`,
      { next: { revalidate: 60 } }
    );
    const data: PageResponse<Post> = await res.json();
    return data.content;
  } catch {
    return [];
  }
}

export default async function Home() {
  const posts = await getRecentPosts();

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      {/* 히어로 섹션 */}
      <section className="mb-16">
        <p className="text-sm text-[#a78bfa] font-mono mb-3">Hello, World 👋</p>
        <h1 className="text-4xl font-bold text-white leading-tight tracking-tight">
          seop의 개발 블로그
        </h1>
        <p className="mt-4 text-[#888888] leading-relaxed">
          Backend 개발 공부 기록 및 프로젝트 경험을 남기는 공간입니다.
        </p>
      </section>

      {/* 최근 글 */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-[#e5e5e5]">최근 글</h2>
          <Link
            href="/posts"
            className="text-sm text-[#888888] hover:text-[#a78bfa] transition-colors"
          >
            전체 보기 →
          </Link>
        </div>
        {posts.length === 0 ? (
          <p className="text-[#888888] text-center py-12">아직 게시글이 없습니다.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
```

**Step 2: 확인**

홈 페이지에 히어로 섹션 + 포스트 그리드가 다크 테마로 표시되면 성공

**Step 3: 커밋**

```bash
git add frontend/src/app/page.tsx
git commit -m "style: 홈 페이지 히어로 섹션 및 다크 테마 적용"
```

---

### Task 5: 게시글 목록 페이지 리디자인

**Files:**
- Modify: `frontend/src/app/posts/page.tsx`

**Step 1: posts/page.tsx 전체 교체 (카드 → 리스트 형식)**

```tsx
import PostCard from '@/components/post/PostCard';
import { Post, PageResponse } from '@/types';
import Link from 'next/link';

async function getPosts(page: number): Promise<PageResponse<Post>> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/posts?page=${page}&size=10`,
    { next: { revalidate: 60 } }
  );
  return res.json();
}

export default async function PostsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const params = await searchParams;
  const page = Number(params.page ?? 0);
  const data = await getPosts(page);

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <h1 className="text-2xl font-bold text-white mb-10">Posts</h1>

      <ul className="space-y-0 divide-y divide-[#2a2a2a]">
        {data.content.map((post) => (
          <li key={post.id} className="py-5 group">
            <Link href={`/posts/${post.id}`} className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1.5">
                  {post.categoryName && (
                    <span className="text-[0.65rem] bg-[#2a2a2a] text-[#a78bfa] px-1.5 py-0.5 rounded font-medium">
                      {post.categoryName}
                    </span>
                  )}
                </div>
                <h2 className="text-[0.95rem] font-medium text-[#e5e5e5] group-hover:text-[#a78bfa] transition-colors leading-snug truncate">
                  {post.title}
                </h2>
                <p className="mt-1 text-xs text-[#888888]">
                  {new Date(post.createdAt).toLocaleDateString('ko-KR')}
                  {post.commentCount > 0 && ` · 댓글 ${post.commentCount}`}
                </p>
              </div>
              {post.imageUrl && (
                <div className="w-16 h-12 flex-shrink-0 rounded-lg overflow-hidden">
                  <img
                    src={`${process.env.NEXT_PUBLIC_API_URL}${post.imageUrl}`}
                    alt=""
                    className="w-full h-full object-cover"
                  />
                </div>
              )}
            </Link>
          </li>
        ))}
      </ul>

      {/* 페이지네이션 */}
      {data.totalPages > 1 && (
        <div className="flex justify-center gap-1.5 mt-10">
          {Array.from({ length: data.totalPages }, (_, i) => (
            <Link
              key={i}
              href={`/posts?page=${i}`}
              className={`w-8 h-8 flex items-center justify-center rounded text-sm transition-colors ${
                i === page
                  ? 'bg-[#a78bfa] text-[#111111] font-semibold'
                  : 'text-[#888888] hover:text-[#e5e5e5] hover:bg-[#1a1a1a]'
              }`}
            >
              {i + 1}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
```

**Step 2: 확인**

`/posts` 페이지에서 리스트 형식으로 게시글이 표시되면 성공

**Step 3: 커밋**

```bash
git add frontend/src/app/posts/page.tsx
git commit -m "style: 게시글 목록 다크 리스트 형식으로 변경"
```

---

### Task 6: 게시글 상세 페이지 리디자인

**Files:**
- Modify: `frontend/src/app/posts/[id]/page.tsx`

**Step 1: posts/[id]/page.tsx 전체 교체**

```tsx
import { Post, Comment } from '@/types';
import CommentSection from '@/components/comment/CommentSection';
import { notFound } from 'next/navigation';
import Link from 'next/link';

async function getPost(id: string): Promise<Post> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/posts/${id}`,
    { cache: 'no-store' }
  );
  if (!res.ok) notFound();
  return res.json();
}

async function getComments(id: string): Promise<Comment[]> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/comments/${id}`,
    { cache: 'no-store' }
  );
  if (!res.ok) return [];
  return res.json();
}

export default async function PostDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [post, comments] = await Promise.all([getPost(id), getComments(id)]);

  return (
    <div className="max-w-[680px] mx-auto px-6 py-16">
      {/* 뒤로가기 */}
      <Link
        href="/posts"
        className="inline-flex items-center gap-1 text-sm text-[#888888] hover:text-[#a78bfa] transition-colors mb-10"
      >
        ← Posts
      </Link>

      <article>
        {/* 메타 */}
        <div className="mb-8">
          {post.categoryName && (
            <span className="inline-block text-[0.7rem] bg-[#2a2a2a] text-[#a78bfa] px-2 py-0.5 rounded font-medium mb-3">
              {post.categoryName}
            </span>
          )}
          <h1 className="text-3xl font-bold text-white leading-tight tracking-tight">
            {post.title}
          </h1>
          <p className="mt-3 text-sm text-[#888888]">
            {new Date(post.createdAt).toLocaleDateString('ko-KR', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })}
            {post.updatedAt && (
              <span className="ml-2">(수정됨)</span>
            )}
          </p>
        </div>

        {/* 썸네일 */}
        {post.imageUrl && (
          <img
            src={`${process.env.NEXT_PUBLIC_API_URL}${post.imageUrl}`}
            alt={post.title}
            className="w-full rounded-xl mb-10 max-h-80 object-cover"
          />
        )}

        {/* 본문 */}
        <div
          className="prose max-w-none"
          dangerouslySetInnerHTML={{ __html: post.content }}
        />
      </article>

      {/* 댓글 */}
      <CommentSection postId={Number(id)} initialComments={comments} />
    </div>
  );
}
```

**Step 2: 확인**

게시글 상세 페이지에서 본문이 prose 스타일로 깔끔하게 표시되면 성공

**Step 3: 커밋**

```bash
git add frontend/src/app/posts/
git commit -m "style: 게시글 상세 페이지 다크 prose 스타일 적용"
```

---

### Task 7: Q&A 페이지 리디자인

**Files:**
- Modify: `frontend/src/app/qna/page.tsx`
- Modify: `frontend/src/app/qna/[id]/page.tsx`

**Step 1: qna/page.tsx 전체 교체**

```tsx
import Link from 'next/link';
import { Qna, PageResponse } from '@/types';

async function getQnaList(page: number): Promise<PageResponse<Qna>> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/qna?page=${page}&size=10`,
    { cache: 'no-store' }
  );
  return res.json();
}

export default async function QnaPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const params = await searchParams;
  const page = Number(params.page ?? 0);
  const data = await getQnaList(page);

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <div className="flex items-center justify-between mb-10">
        <h1 className="text-2xl font-bold text-white">Q&A</h1>
        <Link
          href="/qna/new"
          className="text-sm bg-[#a78bfa] text-[#111111] px-4 py-2 rounded-lg font-semibold hover:opacity-90 transition-opacity"
        >
          질문하기
        </Link>
      </div>

      <ul className="divide-y divide-[#2a2a2a]">
        {data.content.map((qna) => (
          <li key={qna.id} className="py-5 group">
            <Link href={`/qna/${qna.id}`} className="flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1.5">
                  {qna.hasAnswer ? (
                    <span className="text-[0.65rem] bg-[#1a2e1a] text-[#4ade80] px-1.5 py-0.5 rounded font-medium">
                      답변완료
                    </span>
                  ) : (
                    <span className="text-[0.65rem] bg-[#2a2a2a] text-[#888888] px-1.5 py-0.5 rounded font-medium">
                      미답변
                    </span>
                  )}
                </div>
                <h2 className="text-[0.95rem] font-medium text-[#e5e5e5] group-hover:text-[#a78bfa] transition-colors">
                  {qna.title}
                </h2>
                <p className="mt-1 text-xs text-[#888888]">
                  {qna.username} · {new Date(qna.createdAt).toLocaleDateString('ko-KR')}
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

**Step 2: qna/[id]/page.tsx 전체 교체**

```tsx
import { Qna } from '@/types';
import { notFound } from 'next/navigation';
import Link from 'next/link';

async function getQna(id: string): Promise<Qna> {
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/qna/${id}`,
    { cache: 'no-store' }
  );
  if (!res.ok) notFound();
  return res.json();
}

export default async function QnaDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const qna = await getQna(id);

  return (
    <div className="max-w-[680px] mx-auto px-6 py-16">
      <Link
        href="/qna"
        className="inline-flex items-center gap-1 text-sm text-[#888888] hover:text-[#a78bfa] transition-colors mb-10"
      >
        ← Q&A
      </Link>

      <article>
        <div className="mb-8">
          <div className="mb-3">
            {qna.hasAnswer ? (
              <span className="text-[0.7rem] bg-[#1a2e1a] text-[#4ade80] px-2 py-0.5 rounded font-medium">
                답변완료
              </span>
            ) : (
              <span className="text-[0.7rem] bg-[#2a2a2a] text-[#888888] px-2 py-0.5 rounded font-medium">
                미답변
              </span>
            )}
          </div>
          <h1 className="text-2xl font-bold text-white leading-tight">{qna.title}</h1>
          <p className="mt-3 text-sm text-[#888888]">
            {qna.username} · {new Date(qna.createdAt).toLocaleDateString('ko-KR')}
          </p>
        </div>

        <div className="bg-[#1a1a1a] border border-[#2a2a2a] rounded-xl p-6 text-[#d4d4d4] leading-relaxed whitespace-pre-wrap text-sm">
          {qna.content}
        </div>

        {qna.answer && (
          <div className="mt-6">
            <div className="flex items-center gap-2 mb-3">
              <div className="h-px flex-1 bg-[#2a2a2a]" />
              <span className="text-xs text-[#a78bfa] font-medium px-2">답변</span>
              <div className="h-px flex-1 bg-[#2a2a2a]" />
            </div>
            <div className="bg-[#1a1a1a] border border-[#a78bfa]/30 rounded-xl p-6 text-[#d4d4d4] leading-relaxed whitespace-pre-wrap text-sm">
              {qna.answer}
            </div>
          </div>
        )}
      </article>
    </div>
  );
}
```

**Step 3: 확인**

`/qna` 페이지에서 다크 리스트, 답변완료/미답변 뱃지가 표시되면 성공

**Step 4: 커밋**

```bash
git add frontend/src/app/qna/
git commit -m "style: Q&A 페이지 다크 테마 적용"
```

---

### Task 8: 로그인 페이지 리디자인

**Files:**
- Modify: `frontend/src/app/auth/login/page.tsx`

**Step 1: login/page.tsx 전체 교체**

```tsx
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { setUser } = useAuthStore();
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await api.post('/api/auth/login', { email, password });
      setUser(res.data);
      router.push('/');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#111111] px-4">
      <div className="w-full max-w-sm">
        {/* 로고 */}
        <div className="text-center mb-8">
          <span className="font-mono text-xl font-bold text-[#a78bfa]">seop.dev</span>
          <p className="mt-2 text-sm text-[#888888]">로그인하여 계속하세요</p>
        </div>

        {/* 카드 */}
        <div className="bg-[#1a1a1a] border border-[#2a2a2a] rounded-2xl p-8">
          {error && (
            <div className="mb-4 px-3 py-2 bg-red-500/10 border border-red-500/20 rounded-lg text-sm text-red-400 text-center">
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-xs text-[#888888] mb-1.5">이메일</label>
              <input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="w-full bg-[#111111] border border-[#2a2a2a] text-[#e5e5e5] rounded-lg px-3 py-2.5 text-sm placeholder-[#444] focus:outline-none focus:border-[#a78bfa] transition-colors"
              />
            </div>
            <div>
              <label className="block text-xs text-[#888888] mb-1.5">비밀번호</label>
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full bg-[#111111] border border-[#2a2a2a] text-[#e5e5e5] rounded-lg px-3 py-2.5 text-sm placeholder-[#444] focus:outline-none focus:border-[#a78bfa] transition-colors"
              />
            </div>
            <button
              type="submit"
              className="w-full bg-[#a78bfa] text-[#111111] py-2.5 rounded-lg font-semibold text-sm hover:opacity-90 transition-opacity mt-2"
            >
              로그인
            </button>
          </form>

          <div className="mt-4 relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-[#2a2a2a]" />
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="bg-[#1a1a1a] px-2 text-[#888888]">또는</span>
            </div>
          </div>

          <div className="mt-4">
            <a
              href={`${process.env.NEXT_PUBLIC_API_URL}/oauth2/authorization/google`}
              className="w-full flex items-center justify-center gap-2 border border-[#2a2a2a] text-[#e5e5e5] rounded-lg py-2.5 text-sm hover:border-[#a78bfa] hover:text-[#a78bfa] transition-colors"
            >
              <svg width="16" height="16" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
              Google로 로그인
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
```

**Step 2: 확인**

`/auth/login` 페이지에서 다크 로그인 카드가 표시되면 성공

**Step 3: 커밋**

```bash
git add frontend/src/app/auth/
git commit -m "style: 로그인 페이지 다크 테마 적용"
```

---

### Task 9: 댓글 섹션 리디자인

**Files:**
- Modify: `frontend/src/components/comment/CommentSection.tsx`

**Step 1: CommentSection.tsx 전체 교체**

```tsx
'use client';

import { useState } from 'react';
import { Comment } from '@/types';
import { useAuthStore } from '@/store/authStore';
import api from '@/lib/api';
import Link from 'next/link';

export default function CommentSection({
  postId,
  initialComments,
}: {
  postId: number;
  initialComments: Comment[];
}) {
  const [comments, setComments] = useState<Comment[]>(initialComments);
  const [content, setContent] = useState('');
  const { user } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    try {
      const res = await api.post('/api/comments', { content, postId });
      setComments([...comments, res.data]);
      setContent('');
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <section className="mt-16 border-t border-[#2a2a2a] pt-10">
      <h2 className="text-base font-semibold text-[#e5e5e5] mb-6">
        댓글 <span className="text-[#a78bfa]">{comments.length}</span>
      </h2>

      <ul className="space-y-4 mb-8">
        {comments.map((comment) => (
          <li key={comment.id} className="bg-[#1a1a1a] border border-[#2a2a2a] rounded-xl px-4 py-3">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-sm font-medium text-[#e5e5e5]">
                {comment.user?.username ?? '익명'}
              </span>
              <span className="text-xs text-[#888888]">
                {new Date(comment.createdAt).toLocaleDateString('ko-KR')}
              </span>
            </div>
            <p className="text-sm text-[#d4d4d4] leading-relaxed">{comment.content}</p>
          </li>
        ))}
      </ul>

      {user ? (
        <form onSubmit={handleSubmit} className="flex gap-2">
          <input
            type="text"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="댓글을 입력하세요..."
            className="flex-1 bg-[#1a1a1a] border border-[#2a2a2a] text-[#e5e5e5] rounded-xl px-4 py-2.5 text-sm placeholder-[#444] focus:outline-none focus:border-[#a78bfa] transition-colors"
          />
          <button
            type="submit"
            className="px-4 py-2.5 bg-[#a78bfa] text-[#111111] rounded-xl text-sm font-semibold hover:opacity-90 transition-opacity whitespace-nowrap"
          >
            등록
          </button>
        </form>
      ) : (
        <div className="bg-[#1a1a1a] border border-[#2a2a2a] rounded-xl px-4 py-4 text-center">
          <p className="text-sm text-[#888888]">
            댓글을 작성하려면{' '}
            <Link href="/auth/login" className="text-[#a78bfa] hover:underline">
              로그인
            </Link>
            하세요.
          </p>
        </div>
      )}
    </section>
  );
}
```

**Step 2: 확인**

게시글 상세 페이지 하단 댓글 섹션이 다크 스타일로 표시되면 성공

**Step 3: 최종 커밋**

```bash
git add frontend/src/components/comment/CommentSection.tsx
git commit -m "style: 댓글 섹션 다크 테마 적용"
```

---

## 완료 후 확인 사항

1. `http://localhost:3000` — 히어로 + 포스트 그리드
2. `http://localhost:3000/posts` — 다크 리스트 형식
3. `http://localhost:3000/posts/[id]` — prose 본문 스타일
4. `http://localhost:3000/qna` — Q&A 다크 리스트
5. `http://localhost:3000/auth/login` — 다크 로그인 카드

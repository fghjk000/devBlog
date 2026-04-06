export interface Post {
  id: number;
  title: string;
  content: string;
  author: string;
  imageUrl: string | null;
  categoryName: string | null;
  commentCount: number;
  createdAt: string;
  updatedAt: string | null;
}

export interface Comment {
  id: number;
  content: string;
  username: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export interface Qna {
  id: number;
  title: string;
  content: string;
  username: string;
  createdAt: string;
  answers: Answer[];
}

export interface Answer {
  id: number;
  content: string;
  username: string;
  createdAt: string;
}

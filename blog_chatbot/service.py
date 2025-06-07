import mysql.connector
from fastapi import HTTPException
from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from database import get_db_connection

okt = Okt()


def extract_nouns(text: str):
    # 문장에서 명사만 추출
    return " ".join(okt.nouns(text))


def get_best_answer_from_db(query: str):
    tables = ["post", "qna"]
    best_answer = None
    best_similarity = 0
    suggestions = []

    conn = get_db_connection()
    for table in tables:
        try:
            cursor = conn.cursor()
            if table == "post":
                # 게시글 테이블 에서는 'title', 'content' 를 기준으로 검색
                cursor.execute(f"SELECT title, content FROM {table}")
            else:
                # Q&A 테이블에서는 'content', 'title' 을 기준으로 검색
                cursor.execute(f"SELECT content, title FROM {table}")
            data = cursor.fetchall()

            # 각 테이블에 맞는 항목들 리스트로 추출
            if table == "product":
                titles = []
                contents = []
                for title in data:
                    titles.append(title[0])
                    contents.append(title[1])
            else:
                titles = [title[0] for title in data]  # 질문
                contents = [title[1] for title in data]  # 답변

            # 핵심 단어 추출
            query_nouns = extract_nouns(query)
            title_nouns = [extract_nouns(title) for title in titles]

            # TF-IDF 벡터화
            vectorizer = TfidfVectorizer()
            tfidf_matrix = vectorizer.fit_transform([query_nouns] + title_nouns)

            # 유사도 계산 (첫 번째 벡터는 사용자가 입력한 질문)
            similarities = cosine_similarity(
                tfidf_matrix[0:1], tfidf_matrix[1:]
            ).flatten()

            # 가장 유사한 항목의 인덱스
            best_match_idx = similarities.argmax()

            if similarities[best_match_idx] > best_similarity:
                best_similarity = similarities[best_match_idx]
                if table == "post":
                    best_answer = (
                        f"<strong>제목:</strong> {titles[best_match_idx]}<br>"
                        f"<strong>내용:</strong> {contents[best_match_idx]}"
                    )
                else:
                    best_answer = (
                        f"<strong>제목:</strong> {titles[best_match_idx]}<br>"
                        f"<strong>내용:</strong> {contents[best_match_idx]}"
                    )

            # 유사도가 낮을 경우 top3 추천 질문을 추출
            if similarities[best_match_idx] < 0.6:
                suggestions = []
                for idx in similarities.argsort()[:3]:
                    suggestions.append(
                        titles[idx]
                        if table != "post"
                        else f"{titles[idx]} - {contents[idx]}"
                    )

        except mysql.connector.Error as err:
            raise HTTPException(status_code=500, detail=f"Database query error: {err}")
        finally:
            cursor.close()
    conn.close()

    # 유사도가 너무 낮으면 고객센터 안내 메시지 반환
    if best_similarity < 0.1:  # 임계값을 0.1로 설정
        best_answer = "고객센터에 문의해 주세요. 자세한 사항은 고객센터를 통해 확인할 수 있습니다."

    return best_answer, suggestions

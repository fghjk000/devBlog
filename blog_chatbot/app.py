from os import environ
from dotenv import load_dotenv
from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from langchain_core.output_parsers import StrOutputParser
from langchain_community.chat_models import ChatOpenAI

from service import get_best_answer_from_db

load_dotenv(override=True)

API_KEY = environ.get("OPENAI_API_KEY")  # 환경변수명 OpenAI_API_KEY로 변경 권장

app = FastAPI()
templates = Jinja2Templates(directory="templates")


llm = ChatOpenAI(
    model="gpt-4o-mini",
    temperature=0.1,
    openai_api_key=API_KEY
)

chain = llm | StrOutputParser()

@app.get("/", response_class=HTMLResponse)
async def get_ui(request: Request):
    return templates.TemplateResponse(
        "index.html",
        {"request": request, "query": "", "answer": "", "table": "", "suggestions": []},
    )

@app.post("/", response_class=HTMLResponse)
async def chat(request: Request, query: str = Form(...)):
    answer, suggestions = get_best_answer_from_db(query)

    if not answer:
        answer = chain.invoke(query)

    return templates.TemplateResponse(
        "index.html",
        {
            "request": request,
            "query": query,
            "answer": answer,
            "su ggestions": suggestions,
        },
    )

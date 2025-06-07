from os import environ
import mysql.connector
from fastapi import HTTPException


def get_db_connection():
    try:
        # 올바르게 DB 연결 객체 반환
        connection = mysql.connector.connect(
            host=environ.get("MYSQL_HOST"),
            port=environ.get("MYSQL_PORT"),
            user=environ.get("MYSQL_USER"),
            password=environ.get("MYSQL_PASSWORD"),
            database=environ.get("MYSQL_DATABASE"),
        )
        return connection
    except mysql.connector.Error as err:
        # 에러 발생 시 HTTPException
        raise HTTPException(status_code=500, detail=f"Database connection error: {err}")

#!/usr/bin/env python3
"""Harvest API 서버 실행 스크립트"""

import uvicorn

from src.api.main import app

if __name__ == "__main__":
    # 개발 서버 실행
    uvicorn.run(
        app,
        host="0.0.0.0",  # 모든 인터페이스에서 접속 가능
        port=8000,
        reload=True,  # 코드 변경 시 자동 재시작
        log_level="info",
    )
